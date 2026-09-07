# 登录模块 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现古玩寄卖平台登录模块全部 6 个接口（发送验证码、短信登录、密码登录、获取/修改个人信息、退出登录），基于 Redis 随机 Token 认证。

**Architecture:** Maven 多模块（common/pojo/server），Spring Boot 3.2.0 + MyBatis-Plus + Redis Token。遵循 ciTY Art 参考项目的代码分层模式，核心差异是用 Redis 随机 UUID Token 替代 JWT。

**Tech Stack:** Java 17, Spring Boot 3.2.0, MyBatis-Plus 3.5.5, Redis (Lettuce), MySQL, BCrypt (jbcrypt 0.4), Lombok, Hutool 5.8.26, Knife4j 4.4.0

## Global Constraints

- 所有接口响应格式：`{ "code": 1, "msg": "...", "data": {...} }`（1=成功, 0=失败）
- Token 格式：UUID 去横线，存入 Redis Key `antique:token:{token}`，Value 为 `{"userId":1,"phone":"138****8000"}`，TTL 7 天
- 验证码固定 `666666`，Key 为 `sms:code:{phone}`，TTL 5 分钟
- 密码使用 BCrypt 加密
- 用户表逻辑删除字段 `deleted_time`，所有查询必须过滤 `deleted_time IS NULL`
- 短信登录用户不存在时自动注册，默认昵称 `藏友` + 6 位随机数字
- 包名统一 `com.antique`，编码 UTF-8
- 异常统一使用 `throw new AuthException(msg)`，由 `GlobalExceptionHandler` 捕获转 `Result.error()`

---

### Task 1: antique-common 公共模块基础类

**目标：** 创建所有模块依赖的基础类 — 统一响应、常量、异常、用户上下文。这是整个项目的基石，零外部依赖。

**文件清单：**
- Create: `antique-common/src/main/java/com/antique/result/Result.java`
- Create: `antique-common/src/main/java/com/antique/constant/MessageConstant.java`
- Create: `antique-common/src/main/java/com/antique/constant/RedisConstant.java`
- Create: `antique-common/src/main/java/com/antique/exception/BaseException.java`
- Create: `antique-common/src/main/java/com/antique/exception/AuthException.java`
- Create: `antique-common/src/main/java/com/antique/context/UserContext.java`

**全部代码如下：**

#### 1.1 `Result.java`

```java
package com.antique.result;

import lombok.Data;

import java.io.Serializable;

@Data
public class Result<T> implements Serializable {

    private Integer code; // 1=成功, 0=失败
    private String msg;
    private T data;

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = 1;
        result.msg = "操作成功";
        return result;
    }

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.code = 1;
        result.msg = "操作成功";
        result.data = data;
        return result;
    }

    public static <T> Result<T> success(T data, String msg) {
        Result<T> result = new Result<>();
        result.code = 1;
        result.msg = msg;
        result.data = data;
        return result;
    }

    public static <T> Result<T> error(String msg) {
        Result<T> result = new Result<>();
        result.code = 0;
        result.msg = msg;
        return result;
    }
}
```

#### 1.2 `MessageConstant.java`

```java
package com.antique.constant;

public class MessageConstant {

    public static final String OP_SUCCESS = "操作成功";
    public static final String SYSTEM_ERROR = "系统异常";

    // Auth messages
    public static final String PHONE_INVALID = "手机号格式不正确";
    public static final String CODE_SEND_SUCCESS = "验证码发送成功";
    public static final String CODE_ERROR_OR_EXPIRED = "验证码错误或已过期";
    public static final String LOGIN_SUCCESS = "登录成功";
    public static final String ACCOUNT_OR_PASSWORD_ERROR = "账号或密码错误";
    public static final String PHONE_NOT_REGISTERED = "手机号未注册";
    public static final String ACCOUNT_DISABLED = "账号已被禁用";
    public static final String NOT_LOGIN = "未登录，请先登录";
    public static final String LOGOUT_SUCCESS = "退出成功";
    public static final String PROFILE_UPDATE_SUCCESS = "修改成功";
}
```

#### 1.3 `RedisConstant.java`

```java
package com.antique.constant;

public class RedisConstant {

    /** Token 存储 Key 前缀 */
    public static final String KEY_TOKEN = "antique:token:";

    /** 短信验证码 Key 前缀 */
    public static final String KEY_SMS_CODE = "sms:code:";

    /** Token 过期时间：7 天（秒） */
    public static final long TOKEN_TTL = 604800L;

    /** 验证码过期时间：5 分钟（秒） */
    public static final long CODE_TTL = 300L;
}
```

#### 1.4 `BaseException.java`

```java
package com.antique.exception;

/**
 * 业务异常基类，所有业务异常继承此类
 */
public abstract class BaseException extends RuntimeException {

    public BaseException() {
    }

    public BaseException(String msg) {
        super(msg);
    }
}
```

#### 1.5 `AuthException.java`

```java
package com.antique.exception;

/**
 * 认证相关异常：登录、Token 校验等
 */
public class AuthException extends BaseException {

    public AuthException() {
    }

    public AuthException(String msg) {
        super(msg);
    }
}
```

#### 1.6 `UserContext.java`

```java
package com.antique.context;

/**
 * 用户上下文 — 基于 ThreadLocal，存储当前请求的用户信息
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_PHONE = new ThreadLocal<>();

    public static void set(Long userId, String phone) {
        USER_ID.set(userId);
        USER_PHONE.set(phone);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static String getUserPhone() {
        return USER_PHONE.get();
    }

    public static void clear() {
        USER_ID.remove();
        USER_PHONE.remove();
    }
}
```

**接口定义：**
- Produces: `Result<T>` (success/error 工厂方法), `MessageConstant.*` (所有消息常量), `RedisConstant.*` (Key 前缀和 TTL), `BaseException` / `AuthException` (异常类), `UserContext` (ThreadLocal 读写清理)

---

### Task 2: antique-pojo 数据模型层

**目标：** 创建用户实体、所有请求 DTO 和响应 VO。依赖 Task 1 的公共模块（实际上不直接引用，但同属一个项目）。

**文件清单：**
- Create: `antique-pojo/src/main/java/com/antique/entity/User.java`
- Create: `antique-pojo/src/main/java/com/antique/dto/SendCodeDTO.java`
- Create: `antique-pojo/src/main/java/com/antique/dto/SmsLoginDTO.java`
- Create: `antique-pojo/src/main/java/com/antique/dto/PasswordLoginDTO.java`
- Create: `antique-pojo/src/main/java/com/antique/dto/UpdateProfileDTO.java`
- Create: `antique-pojo/src/main/java/com/antique/vo/LoginVO.java`
- Create: `antique-pojo/src/main/java/com/antique/vo/UserProfileVO.java`

**全部代码：**

#### 2.1 `User.java`

```java
package com.antique.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user")
public class User implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String phone;

    private String password;

    private String nickname;

    private String avatar;

    /** 1=正常, 0=禁用 */
    private Integer status;

    private Integer points;

    private Integer signInDays;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private LocalDateTime deletedTime;
}
```

#### 2.2 `SendCodeDTO.java`

```java
package com.antique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendCodeDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
}
```

#### 2.3 `SmsLoginDTO.java`

```java
package com.antique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SmsLoginDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "验证码不能为空")
    private String code;
}
```

#### 2.4 `PasswordLoginDTO.java`

```java
package com.antique.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PasswordLoginDTO {

    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "密码不能为空")
    private String password;
}
```

#### 2.5 `UpdateProfileDTO.java`

```java
package com.antique.dto;

import lombok.Data;

@Data
public class UpdateProfileDTO {

    private String nickname;

    private String avatar;
}
```

#### 2.6 `LoginVO.java`

```java
package com.antique.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginVO {

    private String token;

    private UserInfoVO userInfo;

    @Data
    @Builder
    public static class UserInfoVO {
        private Long id;
        private String nickname;
        private String avatar;
        private String phone;
        private Integer points;
        private Integer signInDays;
    }
}
```

#### 2.7 `UserProfileVO.java`

```java
package com.antique.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileVO {

    private Long id;
    private String nickname;
    private String avatar;
    private String phone;
    private Integer status;
    private Integer points;
    private Integer signInDays;
    private String createdTime;
}
```

**接口定义：**
- Consumes: 无直接依赖（Jakarta Validation 注解独立，MyBatis-Plus 注解独立，Lombok 独立）
- Produces: `User` (MyBatis-Plus 实体), `SendCodeDTO` / `SmsLoginDTO` / `PasswordLoginDTO` / `UpdateProfileDTO` (请求 DTO), `LoginVO` / `LoginVO.UserInfoVO` / `UserProfileVO` (响应 VO)

---

### Task 3: antique-server 基础设施层

**目标：** 创建 Spring Boot 启动类、Web 配置、Token 拦截器、全局异常处理器、MyBatis-Plus 自动填充、UserMapper。这是业务代码运行的骨架。

**文件清单：**
- Create: `antique-server/src/main/java/com/antique/AntiqueApplication.java`
- Create: `antique-server/src/main/java/com/antique/config/WebMvcConfiguration.java`
- Create: `antique-server/src/main/java/com/antique/interceptor/TokenInterceptor.java`
- Create: `antique-server/src/main/java/com/antique/handler/GlobalExceptionHandler.java`
- Create: `antique-server/src/main/java/com/antique/handler/MyMetaObjectHandler.java`
- Create: `antique-server/src/main/java/com/antique/mapper/UserMapper.java`

**全部代码：**

#### 3.1 `AntiqueApplication.java`

```java
package com.antique;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.antique.mapper")
public class AntiqueApplication {

    public static void main(String[] args) {
        SpringApplication.run(AntiqueApplication.class, args);
    }
}
```

#### 3.2 `WebMvcConfiguration.java`

```java
package com.antique.config;

import com.antique.interceptor.TokenInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfiguration implements WebMvcConfigurer {

    private final TokenInterceptor tokenInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tokenInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**"  // 登录/验证码接口无需认证
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("*")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
```

#### 3.3 `TokenInterceptor.java`

```java
package com.antique.interceptor;

import com.antique.constant.MessageConstant;
import com.antique.context.UserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.antique.constant.RedisConstant.KEY_TOKEN;
import static com.antique.constant.RedisConstant.TOKEN_TTL;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate redisTemplate;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        // 从 Header 获取 token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"" + MessageConstant.NOT_LOGIN + "\",\"data\":null}");
            return false;
        }

        String token = authHeader.substring(7);
        String redisKey = KEY_TOKEN + token;

        // 从 Redis 获取用户信息
        String userJson;
        try {
            userJson = redisTemplate.opsForValue().get(redisKey);
        } catch (Exception e) {
            log.error("Redis 读取 Token 失败", e);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"系统繁忙\",\"data\":null}");
            return false;
        }

        if (userJson == null) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":0,\"msg\":\"" + MessageConstant.NOT_LOGIN + "\",\"data\":null}");
            return false;
        }

        // 解析 JSON 获取 userId
        @SuppressWarnings("unchecked")
        Map<String, Object> userMap = MAPPER.readValue(userJson, Map.class);
        Long userId = Long.valueOf(userMap.get("userId").toString());
        String phone = (String) userMap.get("phone");

        // 设置用户上下文
        UserContext.set(userId, phone);

        // 续期 Token TTL（7天）
        try {
            redisTemplate.expire(redisKey, TOKEN_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis Token 续期失败: {}", e.getMessage());
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
```

#### 3.4 `GlobalExceptionHandler.java`

```java
package com.antique.handler;

import com.antique.constant.MessageConstant;
import com.antique.exception.BaseException;
import com.antique.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常 */
    @ExceptionHandler(BaseException.class)
    public Result<?> handleBaseException(BaseException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return Result.error(ex.getMessage());
    }

    /** 参数校验异常（@Valid） */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(e -> e.getDefaultMessage())
                .findFirst()
                .orElse("参数错误");
        return Result.error(msg);
    }

    /** 请求体解析异常 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return Result.error("请求参数格式错误");
    }

    /** 兜底异常 */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception ex) {
        log.error("系统异常", ex);
        return Result.error(MessageConstant.SYSTEM_ERROR);
    }
}
```

#### 3.5 `MyMetaObjectHandler.java`

```java
package com.antique.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

#### 3.6 `UserMapper.java`

```java
package com.antique.mapper;

import com.antique.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
```

**接口定义：**
- Consumes: Task 1 的 `Result`, `MessageConstant`, `RedisConstant`, `UserContext`, `AuthException`; Task 2 的 `User`
- Produces: `AntiqueApplication` (启动类 + `@MapperScan`), `WebMvcConfiguration` (CORS + 拦截器注册), `TokenInterceptor` (从 Header 取 Token → Redis 校验 → 设 UserContext → 续期), `GlobalExceptionHandler` (异常 → Result.error), `MyMetaObjectHandler` (时间自动填充), `UserMapper` (MyBatis-Plus BaseMapper)

---

### Task 4: 业务服务层

**目标：** 创建 UserService（用户注册/登录/查询/更新）和 TokenService（Token 生成/删除）。这是核心业务逻辑。

**文件清单：**
- Create: `antique-server/src/main/java/com/antique/service/UserService.java`
- Create: `antique-server/src/main/java/com/antique/service/impl/UserServiceImpl.java`

**全部代码：**

#### 4.1 `UserService.java`

```java
package com.antique.service;

import com.antique.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.antique.vo.LoginVO;
import com.antique.vo.UserProfileVO;

public interface UserService extends IService<User> {

    /**
     * 发送短信验证码到 Redis
     * @return 生成的验证码
     */
    String sendSmsCode(String phone);

    /**
     * 短信验证码登录（用户不存在则自动注册）
     */
    LoginVO smsLogin(String phone, String code);

    /**
     * 密码登录
     */
    LoginVO passwordLogin(String phone, String password);

    /**
     * 获取用户个人信息
     */
    UserProfileVO getProfile(Long userId);

    /**
     * 修改用户个人信息
     */
    void updateProfile(Long userId, String nickname, String avatar);

    /**
     * 生成并存储 Token
     */
    String generateToken(User user);

    /**
     * 删除 Token（退出登录）
     */
    void removeToken(String token);
}
```

#### 4.2 `UserServiceImpl.java`

```java
package com.antique.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.antique.constant.RedisConstant.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final StringRedisTemplate redisTemplate;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String sendSmsCode(String phone) {
        // 校验手机号格式（兜底校验，DTO 层也有）
        if (!phone.matches("^1[3-9]\\d{9}$")) {
            throw new AuthException(MessageConstant.PHONE_INVALID);
        }

        String code = "666666";
        try {
            redisTemplate.opsForValue().set(
                    KEY_SMS_CODE + phone,
                    code,
                    CODE_TTL,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.error("Redis 存储验证码失败", e);
            throw new AuthException("系统繁忙");
        }

        log.info("发送验证码到 {}: {}", phone, code);
        return code;
    }

    @Override
    public LoginVO smsLogin(String phone, String code) {
        // 1. 校验验证码
        String storedCode;
        try {
            storedCode = redisTemplate.opsForValue().get(KEY_SMS_CODE + phone);
        } catch (Exception e) {
            log.error("Redis 读取验证码失败", e);
            throw new AuthException("系统繁忙");
        }

        if (storedCode == null || !storedCode.equals(code)) {
            throw new AuthException(MessageConstant.CODE_ERROR_OR_EXPIRED);
        }

        // 2. 删除验证码（防重复使用）
        try {
            redisTemplate.delete(KEY_SMS_CODE + phone);
        } catch (Exception e) {
            log.warn("Redis 删除验证码失败: {}", e.getMessage());
        }

        // 3. 查询用户
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getPhone, phone).isNull(User::getDeletedTime);
        User user = getOne(wrapper);

        // 4. 用户不存在 → 自动注册
        if (user == null) {
            user = new User();
            user.setPhone(phone);
            user.setNickname("藏友" + RandomUtil.randomNumbers(6));
            user.setStatus(1);
            user.setPoints(0);
            user.setSignInDays(0);
            save(user);
            log.info("新用户注册: phone={}, nickname={}", phone, user.getNickname());
        }

        // 5. 检查状态
        if (user.getStatus() != 1) {
            throw new AuthException(MessageConstant.ACCOUNT_DISABLED);
        }

        // 6. 生成 Token
        String token = generateToken(user);

        // 7. 返回
        return buildLoginVO(token, user);
    }

    @Override
    public LoginVO passwordLogin(String phone, String password) {
        // 1. 查询用户
        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getPhone, phone).isNull(User::getDeletedTime);
        User user = getOne(wrapper);

        // 2. 用户不存在
        if (user == null) {
            throw new AuthException(MessageConstant.ACCOUNT_OR_PASSWORD_ERROR);
        }

        // 3. 未设置密码
        if (user.getPassword() == null) {
            throw new AuthException(MessageConstant.PHONE_NOT_REGISTERED);
        }

        // 4. 检查状态
        if (user.getStatus() != 1) {
            throw new AuthException(MessageConstant.ACCOUNT_DISABLED);
        }

        // 5. BCrypt 校验密码
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new AuthException(MessageConstant.ACCOUNT_OR_PASSWORD_ERROR);
        }

        // 6. 生成 Token
        String token = generateToken(user);

        // 7. 返回
        return buildLoginVO(token, user);
    }

    @Override
    public UserProfileVO getProfile(Long userId) {
        User user = getById(userId);
        if (user == null || user.getDeletedTime() != null) {
            throw new AuthException(MessageConstant.NOT_LOGIN);
        }
        if (user.getStatus() != 1) {
            throw new AuthException(MessageConstant.ACCOUNT_DISABLED);
        }

        return UserProfileVO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .phone(maskPhone(user.getPhone()))
                .status(user.getStatus())
                .points(user.getPoints() != null ? user.getPoints() : 0)
                .signInDays(user.getSignInDays() != null ? user.getSignInDays() : 0)
                .createdTime(formatTime(user.getCreateTime()))
                .build();
    }

    @Override
    public void updateProfile(Long userId, String nickname, String avatar) {
        User user = getById(userId);
        if (user == null || user.getDeletedTime() != null) {
            throw new AuthException(MessageConstant.NOT_LOGIN);
        }

        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        updateById(user);
    }

    @Override
    public String generateToken(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", user.getId());
        userInfo.put("phone", maskPhone(user.getPhone()));

        try {
            String json = MAPPER.writeValueAsString(userInfo);
            redisTemplate.opsForValue().set(
                    KEY_TOKEN + token,
                    json,
                    TOKEN_TTL,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.error("Redis 存储 Token 失败", e);
            throw new AuthException("系统繁忙");
        }

        return token;
    }

    @Override
    public void removeToken(String token) {
        try {
            redisTemplate.delete(KEY_TOKEN + token);
        } catch (Exception e) {
            log.warn("Redis 删除 Token 失败: {}", e.getMessage());
        }
    }

    // ====== 私有方法 ======

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

    private String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    private String formatTime(LocalDateTime time) {
        if (time == null) {
            return null;
        }
        return time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
```

**接口定义：**
- Consumes: Task 1 全部基础类; Task 2 的 `User`, `LoginVO`, `UserProfileVO`; Task 3 的 `UserMapper`
- Produces: `UserService` / `UserServiceImpl` (全部 6 个接口的业务逻辑方法)

---

### Task 5: 控制器层

**目标：** 创建 REST 控制器，连接 HTTP 请求到业务服务。所有接口路径严格匹配 API 文档。

**文件清单：**
- Create: `antique-server/src/main/java/com/antique/controller/AuthController.java`
- Create: `antique-server/src/main/java/com/antique/controller/UserController.java`

**全部代码：**

#### 5.1 `AuthController.java`

```java
package com.antique.controller;

import com.antique.constant.MessageConstant;
import com.antique.dto.PasswordLoginDTO;
import com.antique.dto.SendCodeDTO;
import com.antique.dto.SmsLoginDTO;
import com.antique.result.Result;
import com.antique.service.UserService;
import com.antique.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /** 1. 发送短信验证码 */
    @PostMapping("/sms/send")
    public Result<?> sendCode(@Valid @RequestBody SendCodeDTO dto) {
        userService.sendSmsCode(dto.getPhone());
        return Result.success(null, MessageConstant.CODE_SEND_SUCCESS);
    }

    /** 2. 短信验证码登录 */
    @PostMapping("/login/sms")
    public Result<LoginVO> smsLogin(@Valid @RequestBody SmsLoginDTO dto) {
        LoginVO vo = userService.smsLogin(dto.getPhone(), dto.getCode());
        return Result.success(vo, MessageConstant.LOGIN_SUCCESS);
    }

    /** 3. 密码登录 */
    @PostMapping("/login/password")
    public Result<LoginVO> passwordLogin(@Valid @RequestBody PasswordLoginDTO dto) {
        LoginVO vo = userService.passwordLogin(dto.getPhone(), dto.getPassword());
        return Result.success(vo, MessageConstant.LOGIN_SUCCESS);
    }
}
```

#### 5.2 `UserController.java`

```java
package com.antique.controller;

import com.antique.constant.MessageConstant;
import com.antique.context.UserContext;
import com.antique.dto.UpdateProfileDTO;
import com.antique.result.Result;
import com.antique.service.UserService;
import com.antique.vo.UserProfileVO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 4. 获取个人信息 */
    @GetMapping("/profile")
    public Result<UserProfileVO> getProfile() {
        Long userId = UserContext.getUserId();
        UserProfileVO vo = userService.getProfile(userId);
        return Result.success(vo);
    }

    /** 5. 修改个人信息 */
    @PutMapping("/profile")
    public Result<?> updateProfile(@RequestBody UpdateProfileDTO dto) {
        Long userId = UserContext.getUserId();
        userService.updateProfile(userId, dto.getNickname(), dto.getAvatar());
        return Result.success(null, MessageConstant.PROFILE_UPDATE_SUCCESS);
    }

    /** 6. 退出登录 */
    @PostMapping("/logout")
    public Result<?> logout(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            userService.removeToken(token);
        }
        return Result.success(null, MessageConstant.LOGOUT_SUCCESS);
    }
}
```

**接口定义：**
- Consumes: Task 1 的 `Result`, `MessageConstant`, `UserContext`; Task 2 的全部 DTO/VO; Task 4 的 `UserService`
- Produces: `AuthController` (3 个公开接口), `UserController` (3 个需认证接口)

---

### Task 6: 配置文件

**目标：** 创建 application.yml 配置文件（基于已有的 application-template.yml）。

**文件清单：**
- Create: `antique-server/src/main/resources/application.yml`

**配置内容：**

```yaml
server:
  port: 8080

spring:
  application:
    name: antique-server

  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/antique?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: 123456

  data:
    redis:
      host: localhost
      port: 6379
      database: 0
      password:
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 4

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deletedTime
      logic-delete-value: NOW()
      logic-not-delete-value: "null"

knife4j:
  enable: true

springdoc:
  swagger-ui:
    path: /swagger-ui.html
```

---

## 实施顺序

```
Task 1 (common) → Task 2 (pojo) → Task 3 (infrastructure)
                                       ↓
                                  Task 4 (service) → Task 5 (controller) → Task 6 (config)
```

Task 1-3 可以串行，Task 4 必须在 Task 3 之后，Task 5 必须在 Task 4 之后，Task 6 最后。

## 验证方法

完成所有 Task 后，执行：

```bash
cd "C:\Users\20844\Desktop\antique project\server-side"
mvn compile
```

编译通过即表示所有代码正确。

启动后（需要 MySQL + Redis 运行），可手动测试：

```bash
# 1. 发送验证码
curl -X POST http://localhost:8080/api/auth/sms/send \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000"}'

# 2. 短信登录（自动注册）
curl -X POST http://localhost:8080/api/auth/login/sms \
  -H "Content-Type: application/json" \
  -d '{"phone":"13800138000","code":"666666"}'

# 3. 获取个人信息（用返回的 token）
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer <token>"

# 4. 退出登录
curl -X POST http://localhost:8080/api/user/logout \
  -H "Authorization: Bearer <token>"
```
