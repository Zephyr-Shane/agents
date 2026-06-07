package com.opencode.agents.manager;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.opencode.agents.common.BusinessException;
import com.opencode.agents.common.ErrorCode;
import com.opencode.agents.config.WeChatConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 微信小程序 API 服务
 * 封装与微信服务器的交互：
 * - jscode2session：loginCode → openid + session_key
 * - access_token：获取并缓存接口调用凭证
 * - getuserphonenumber：phoneCode → 手机号
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatApiService {

    private static final String JSCODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session";
    private static final String TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential";
    private static final String GET_PHONE_URL =
            "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=";

    private final WeChatConfig weChatConfig;

    /** 缓存的 access_token */
    private String cachedAccessToken;
    /** access_token 过期时间（提前 5 分钟刷新） */
    private LocalDateTime tokenExpireTime;
    private final ReentrantLock tokenLock = new ReentrantLock();

    /**
     * jscode2session：用 loginCode 换取 openid 和 session_key
     *
     * @return Map 包含 openid, session_key, unionid（可能为 null）
     */
    public Map<String, String> jscode2session(String loginCode) {
        // 开发模式 mock
        if (isMockMode()) {
            log.warn("开发模式 mock jscode2session");
            Map<String, String> mock = new HashMap<>();
            mock.put("openid", "dev_mock_openid_" + System.currentTimeMillis());
            mock.put("session_key", "mock_session_key");
            mock.put("unionid", null);
            return mock;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("appid", weChatConfig.getAppId());
        params.put("secret", weChatConfig.getAppSecret());
        params.put("js_code", loginCode);
        params.put("grant_type", "authorization_code");

        try {
            String resp = HttpUtil.get(JSCODE2SESSION_URL, params, 5000);
            JSONObject json = JSONUtil.parseObj(resp);

            Integer errcode = json.getInt("errcode");
            if (errcode != null && errcode != 0) {
                String errmsg = json.getStr("errmsg", "unknown");
                log.error("jscode2session 失败: code={}, msg={}", errcode, errmsg);
                // code 无效时降级
                if (errcode == 40029 || errcode == 40163) {
                    throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录凭证已失效，请重新登录");
                }
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "微信登录验证失败: " + errmsg);
            }

            Map<String, String> result = new HashMap<>();
            result.put("openid", json.getStr("openid"));
            result.put("session_key", json.getStr("session_key"));
            result.put("unionid", json.getStr("unionid"));
            log.info("jscode2session 成功: openid={}", json.getStr("openid"));
            return result;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("jscode2session 请求异常", e);
            // 网络异常时降级为 mock（开发环境）
            if (isMockMode()) {
                log.warn("网络异常，降级为 mock login");
                Map<String, String> mock = new HashMap<>();
                mock.put("openid", "dev_fallback_openid");
                mock.put("session_key", "dev_fallback_session");
                mock.put("unionid", null);
                return mock;
            }
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "微信登录服务异常");
        }
    }

    /**
     * 获取 access_token（带缓存）
     * access_token 有效期 2 小时，提前 5 分钟刷新
     */
    public String getAccessToken() {
        // 检查缓存是否有效
        if (cachedAccessToken != null && tokenExpireTime != null
                && LocalDateTime.now().isBefore(tokenExpireTime)) {
            return cachedAccessToken;
        }

        tokenLock.lock();
        try {
            // 双重检查
            if (cachedAccessToken != null && tokenExpireTime != null
                    && LocalDateTime.now().isBefore(tokenExpireTime)) {
                return cachedAccessToken;
            }

            // 开发模式 mock
            if (isMockMode()) {
                log.warn("开发模式 mock access_token");
                cachedAccessToken = "mock_access_token";
                tokenExpireTime = LocalDateTime.now().plusHours(2);
                return cachedAccessToken;
            }

            Map<String, Object> params = new HashMap<>();
            params.put("appid", weChatConfig.getAppId());
            params.put("secret", weChatConfig.getAppSecret());

            String resp = HttpUtil.get(TOKEN_URL, params, 5000);
            JSONObject json = JSONUtil.parseObj(resp);

            Integer errcode = json.getInt("errcode");
            if (errcode != null && errcode != 0) {
                log.error("获取 access_token 失败: code={}, msg={}",
                        errcode, json.getStr("errmsg"));
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "获取微信接口凭证失败");
            }

            cachedAccessToken = json.getStr("access_token");
            int expiresIn = json.getInt("expires_in", 7200);
            // 提前 5 分钟过期，避免边界情况
            tokenExpireTime = LocalDateTime.now().plusSeconds(expiresIn - 300);
            log.info("access_token 刷新成功，有效期至: {}", tokenExpireTime);
            return cachedAccessToken;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取 access_token 异常", e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "获取微信接口凭证异常");
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * 用 phoneCode 换取手机号（新版接口）
     * 对应 wx.getPhoneNumber 返回的 code
     */
    public String getPhoneNumber(String phoneCode) {
        // 开发模式 mock
        if (isMockMode()) {
            log.warn("开发模式 mock getPhoneNumber");
            return "13800000000";
        }

        String accessToken = getAccessToken();
        String url = GET_PHONE_URL + accessToken;

        Map<String, Object> body = new HashMap<>();
        body.put("code", phoneCode);

        try {
            String resp = HttpUtil.post(url, JSONUtil.toJsonStr(body), 5000);
            JSONObject json = JSONUtil.parseObj(resp);

            Integer errcode = json.getInt("errcode");
            if (errcode != null && errcode != 0) {
                log.error("获取手机号失败: code={}, msg={}", errcode, json.getStr("errmsg"));
                if (errcode == 40029) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号凭证已失效，请重新授权");
                }
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "获取手机号失败");
            }

            JSONObject phoneInfo = json.getJSONObject("phone_info");
            String phoneNumber = phoneInfo.getStr("phoneNumber");
            log.info("获取手机号成功: phone={}", maskPhone(phoneNumber));
            return phoneNumber;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("获取手机号异常", e);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "获取手机号异常");
        }
    }

    /**
     * 判断是否为开发模式（appid 占位值）
     */
    private boolean isMockMode() {
        String appId = weChatConfig.getAppId();
        return appId == null || appId.contains("YOUR") || appId.contains("wxYOUR");
    }

    /**
     * 手机号脱敏（用于日志）
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

}
