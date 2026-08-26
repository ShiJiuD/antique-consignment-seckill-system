package com.antique.service.impl;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.antique.constant.MessageConstant;
import com.antique.entity.User;
import com.antique.exception.AuthException;
import com.antique.mapper.UserMapper;
import com.antique.service.UserService;
import com.antique.vo.LoginVO;
import com.antique.vo.UserProfileVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.antique.constant.RedisConstant.*;

/**
 * 用户服务实现 — 登录模块核心业务逻辑
 *
 * <p>继承 MyBatis-Plus 的 {@code ServiceImpl<UserMapper, User>}，
 * 自动获得 save/getById/updateById/lambdaQuery 等 CRUD 方法。
 *
 * <h3>认证机制</h3>
 * <p>使用 Redis 存储随机 Token（非 JWT），每次请求从 Redis 读取并续期。
 * 登出时直接删除 Redis Key 即可使 Token 失效。
 *
 * <h3>密码安全</h3>
 * <ul>
 *   <li>存储：BCrypt 加密（salt rounds = 10，jBCrypt 默认值）</li>
 *   <li>短信登录用户可以不设置密码（password 字段为 null）</li>
 *   <li>密码登录时统一返回"账号或密码错误"（不区分用户不存在/密码错，防撞库）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final StringRedisTemplate redisTemplate;

    // ========================================================================
    //  接口 1：发送短信验证码
    // ========================================================================

    /**
     * 发送短信验证码到 Redis
     *
     * <p>验证码恒为 666666（开发/测试用，不接入真实短信服务）。
     * <p>Redis Key: sms:code:{phone}，TTL: 5 分钟。
     */
    @Override
    public void sendSmsCode(String phone) {
        // 兜底校验手机号格式（DTO 层已校验，此处二次保证）
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new AuthException(MessageConstant.PHONE_INVALID);
        }

        // 固定验证码（开发/测试环境）
        String code = "666666";

        // 存入 Redis，5 分钟过期
        try {
            redisTemplate.opsForValue().set(
                    KEY_SMS_CODE + phone,
                    code,
                    CODE_TTL,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.error("Redis 存储验证码失败: phone={}", phone, e);
            throw new AuthException(MessageConstant.SYSTEM_BUSY);
        }

        log.info("发送验证码到 {}: {}", phone, code);
    }

    // ========================================================================
    //  接口 2：短信验证码登录（含自动注册）
    // ========================================================================

    /**
     * 短信验证码登录 — 用户不存在时自动注册
     *
     * <p>完整流程：
     * <ol>
     *   <li>校验验证码</li>
     *   <li>删除 Redis 中的验证码（一次性使用）</li>
     *   <li>查询用户（phone + 未逻辑删除）</li>
     *   <li>不存在 → 自动创建（nickname="藏友"+6位随机数）</li>
     *   <li>检查账号状态</li>
     *   <li>生成 Token → 返回</li>
     * </ol>
     */
    @Override
    public LoginVO smsLogin(String phone, String code) {
        // ----- 步骤 1：校验验证码 -----
        String storedCode;
        try {
            storedCode = redisTemplate.opsForValue().get(KEY_SMS_CODE + phone);
        } catch (Exception e) {
            log.error("Redis 读取验证码失败: phone={}", phone, e);
            throw new AuthException(MessageConstant.SYSTEM_BUSY);
        }

        if (storedCode == null || !storedCode.equals(code)) {
            throw new AuthException(MessageConstant.CODE_ERROR_OR_EXPIRED);
        }

        // ----- 步骤 2：删除验证码（一次性使用，防重放） -----
        try {
            redisTemplate.delete(KEY_SMS_CODE + phone);
        } catch (Exception e) {
            // 删除失败不阻塞登录，仅记录警告
            log.warn("Redis 删除验证码失败: phone={}, error={}", phone, e.getMessage());
        }

        // ----- 步骤 3：查询用户 -----
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getPhone, phone)
               .isNull(User::getDeletedTime);  // 逻辑删除过滤
        User user = getOne(wrapper);

        // ----- 步骤 4：用户不存在 → 自动注册 -----
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            // 默认昵称：藏友 + 6 位随机数字
            user.setNickname("藏友" + RandomUtil.randomNumbers(6));
            user.setStatus(1);       // 正常
            user.setPoints(0);       // 初始积分
            user.setSignInDays(0);   // 初始签到天数
            save(user);              // MyBatis-Plus 自动填充 createTime/updateTime
            log.info("新用户自动注册: phone={}, nickname={}", phone, user.getNickname());
        }

        // ----- 步骤 5：检查账号状态 -----
        if (user.getStatus() != 1) {
            throw new AuthException(MessageConstant.ACCOUNT_DISABLED);
        }

        // ----- 步骤 6：生成 Token -----
        String token = generateToken(user);

        // ----- 步骤 7：组装响应 -----
        return buildLoginVO(token, user);
    }

    // ========================================================================
    //  接口 3：密码登录
    // ========================================================================

    /**
     * 密码登录 — BCrypt 校验密码
     *
     * <p>安全设计：
     * <ul>
     *   <li>用户不存在和密码错误返回同一提示，防止撞库</li>
     *   <li>未设置密码的用户（仅短信登录过）返回"手机号未注册"</li>
     * </ul>
     */
    @Override
    public LoginVO passwordLogin(String phone, String password) {
        // ----- 步骤 1：查询用户 -----
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getPhone, phone)
               .isNull(User::getDeletedTime);
        User user = getOne(wrapper);

        // ----- 步骤 2：用户不存在 -----
        // 不区分"用户不存在"和"密码错误"，统一提示防止撞库
        if (user == null) {
            throw new AuthException(MessageConstant.ACCOUNT_OR_PASSWORD_ERROR);
        }

        // ----- 步骤 3：未设置密码（仅短信登录过的用户） -----
        if (user.getPassword() == null) {
            throw new AuthException(MessageConstant.PHONE_NOT_REGISTERED);
        }

        // ----- 步骤 4：检查账号状态 -----
        if (user.getStatus() != 1) {
            throw new AuthException(MessageConstant.ACCOUNT_DISABLED);
        }

        // ----- 步骤 5：BCrypt 密码校验 -----
        // BCrypt.checkpw(明文, 密文) → true/false
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new AuthException(MessageConstant.ACCOUNT_OR_PASSWORD_ERROR);
        }

        // ----- 步骤 6：生成 Token -----
        String token = generateToken(user);

        // ----- 步骤 7：组装响应 -----
        return buildLoginVO(token, user);
    }

    // ========================================================================
    //  接口 4：获取个人信息
    // ========================================================================

    /**
     * 获取用户个人信息
     *
     * <p>查询数据库 → 手机号脱敏 → 返回。
     */
    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = getById(userId);

        // 用户不存在或已逻辑删除
        if (user == null || user.getDeletedTime() != null) {
            throw new AuthException(MessageConstant.NOT_LOGIN);
        }

        // 账号被禁用
        if (user.getStatus() != 1) {
            throw new AuthException(MessageConstant.ACCOUNT_DISABLED);
        }

        // 组装响应（手机号脱敏处理）
        return UserProfileVO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(maskPhone(user.getPhone()))
                .status(user.getStatus())
                .points(user.getPoints() != null ? user.getPoints() : 0)
                .signInDays(user.getSignInDays() != null ? user.getSignInDays() : 0)
                .createdTime(user.getCreateTime())
                .build();
    }

    // ========================================================================
    //  接口 5：修改个人信息
    // ========================================================================

    /**
     * 修改个人信息 — 仅更新传入的非 null 字段
     *
     * <p>手机号不支持通过此接口修改。
     */
    @Override
    public void updateProfile(Long userId, String nickname, String avatar) {
        User user = getById(userId);

        if (user == null || user.getDeletedTime() != null) {
            throw new AuthException(MessageConstant.NOT_LOGIN);
        }

        // 仅更新非 null 字段
        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }

        updateById(user);  // MyBatis-Plus 自动填充 updateTime
    }

    // ========================================================================
    //  Token 管理
    // ========================================================================

    /**
     * 生成并存储 Redis Token
     *
     * <p>Token 采用 32 位随机 UUID（去掉横线），无业务含义，不可解码。
     * <p>Redis Value 存储用户最小标识：userId + 脱敏手机号。
     * <p>TTL 为 7 天，每次拦截器续期。
     */
    @Override
    public String generateToken(User user) {
        // 生成 32 位随机字符串（UUID 去横线）
        String token = UUID.randomUUID().toString().replace("-", "");

        // 构建 Redis Value：用户标识信息
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("phone", maskPhone(user.getPhone()));

        // 存入 Redis，TTL 7 天
        try {
            // Hutool JSONUtil 序列化为 JSON 字符串（无检查异常，无需捕获序列化错误）
            String json = JSONUtil.toJsonStr(userInfo);
            redisTemplate.opsForValue().set(
                    KEY_TOKEN + token,
                    json,
                    TOKEN_TTL,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.error("Redis 存储 Token 失败: userId={}", user.getId(), e);
            throw new AuthException(MessageConstant.SYSTEM_BUSY);
        }

        return token;
    }

    /**
     * 删除 Token（退出登录）
     *
     * <p>直接从 Redis 删除 Key，Token 立即失效。
     * <p>对比 JWT 方案：JWT 无法主动失效，需维护黑名单；
     * Redis Token 方案只需删除 Key 即可，更简洁。
     */
    @Override
    public void removeToken(String token) {
        try {
            redisTemplate.delete(KEY_TOKEN + token);
        } catch (Exception e) {
            // 删除失败仅记录日志，不抛出异常（即使删除失败，Token 也会自然过期）
            log.warn("Redis 删除 Token 失败: token={}, error={}", token, e.getMessage());
        }
    }

    // ========================================================================
    //  私有工具方法
    // ========================================================================

    /**
     * 组装登录响应 VO
     */
    private LoginVO buildLoginVO(String token, User user) {
        return LoginVO.builder()
                .token(token)
                .userInfo(LoginVO.UserInfoVO.builder()
                        .id(user.getId())
                        .nickname(user.getNickname())
                        .avatar(user.getAvatar())
                        .phone(maskPhone(user.getPhone()))
                        .points(user.getPoints() != null ? user.getPoints() : 0)
                        .signInDays(user.getSignInDays() != null ? user.getSignInDays() : 0)
                        .build())
                .build();
    }

    /**
     * 手机号脱敏：13800138000 → 138****8000
     *
     * <p>中间 4 位替换为星号，保护用户隐私。
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}