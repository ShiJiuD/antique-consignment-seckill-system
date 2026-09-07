package com.antique.service;

import com.antique.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.antique.vo.LoginVO;
import com.antique.vo.UserProfileVO;

/**
 * 用户服务接口 — 封装登录模块所有业务逻辑
 *
 * <p>继承 MyBatis-Plus 的 {@code IService<User>}，自动获得 CRUD 方法。
 * 本接口额外定义登录、个人信息管理、Token 管理等方法。
 *
 * <h3>方法一览</h3>
 * <ul>
 *   <li>sendSmsCode — 发送短信验证码 → 接口 1</li>
 *   <li>smsLogin — 短信登录（自动注册）→ 接口 2</li>
 *   <li>passwordLogin — 密码登录 → 接口 3</li>
 *   <li>getProfile — 获取个人信息 → 接口 4</li>
 *   <li>updateProfile — 修改个人信息 → 接口 5</li>
 *   <li>generateToken — 生成 Redis Token</li>
 *   <li>removeToken — 删除 Token（退出登录）→ 接口 6</li>
 * </ul>
 */
public interface UserService extends IService<User> {

    /**
     * 发送短信验证码
     *
     * <p>验证码恒为 666666，存入 Redis Key: sms:code:{phone}，TTL 5 分钟。
     * 不调用真实短信服务商，开发/测试环境使用。
     *
     * @param phone 11 位手机号，格式 ^1[3-9]\d{9}$
     * @throws com.antique.exception.AuthException 手机号格式错误或 Redis 异常
     */
    void sendSmsCode(String phone);

    /**
     * 短信验证码登录（用户不存在则自动注册）
     *
     * <h3>业务流程</h3>
     * <ol>
     *   <li>从 Redis 读取验证码 sms:code:{phone} 并校验</li>
     *   <li>校验通过后删除验证码（防重复使用）</li>
     *   <li>查询用户：SELECT * FROM user WHERE phone=? AND deleted_time IS NULL</li>
     *   <li>用户不存在 → 自动注册（nickname="藏友"+随机6位数字）</li>
     *   <li>检查 status != 1 → 拒绝登录</li>
     *   <li>生成 32 位 UUID Token 存入 Redis</li>
     *   <li>返回 LoginVO（token + 用户信息）</li>
     * </ol>
     *
     * @param phone 手机号
     * @param code  6 位验证码
     * @return 登录成功响应（含 Token 和用户信息）
     * @throws com.antique.exception.AuthException 验证码错误/过期、账号禁用、系统繁忙
     */
    LoginVO smsLogin(String phone, String code);

    /**
     * 密码登录
     *
     * <h3>业务流程</h3>
     * <ol>
     *   <li>查询用户（phone + deleted_time IS NULL）</li>
     *   <li>用户不存在 → 返回统一错误提示（不区分"用户不存在"和"密码错误"，防撞库）</li>
     *   <li>密码为 null → 返回"手机号未注册"（该手机号仅用短信登录过）</li>
     *   <li>status != 1 → 拒绝登录</li>
     *   <li>BCrypt.checkpw() 校验密码</li>
     *   <li>生成 Token → 返回</li>
     * </ol>
     *
     * @param phone    手机号
     * @param password 明文密码（TLS 加密传输）
     * @return 登录成功响应
     * @throws com.antique.exception.AuthException 账号或密码错误、未注册、账号禁用
     */
    LoginVO passwordLogin(String phone, String password);

    /**
     * 获取用户个人信息
     *
     * <p>根据 userId 查询数据库，手机号脱敏后返回。
     *
     * @param userId 当前用户 ID（从 UserContext 获取）
     * @return 用户信息 VO
     * @throws com.antique.exception.AuthException 用户不存在/已删除/已禁用
     */
    UserProfileVO getProfile(Long userId);

    /**
     * 修改用户个人信息
     *
     * <p>仅更新非 null 字段（nickname、avatar）。手机号不支持修改。
     *
     * @param userId   当前用户 ID
     * @param nickname 新昵称（null 表示不修改）
     * @param avatar   新头像 URL（null 表示不修改）
     */
    void updateProfile(Long userId, String nickname, String avatar);

    /**
     * 生成 Token 并存入 Redis
     *
     * <p>Token 格式：32 位随机 UUID（去掉横线），无业务含义。
     * <p>Redis Key: antique:token:{token}
     * <p>Redis Value: {"userId":1,"phone":"138****8000"}
     * <p>TTL: 7 天
     *
     * @param user 用户实体
     * @return 32 位随机 Token 字符串
     */
    String generateToken(User user);

    /**
     * 删除 Token（退出登录）
     *
     * <p>直接从 Redis 删除 Token Key，使其立即失效。
     * 比 JWT 黑名单方案更简单。
     *
     * @param token 待删除的 Token
     */
    void removeToken(String token);
}
