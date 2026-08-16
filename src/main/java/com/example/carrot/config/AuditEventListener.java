package com.example.carrot.config;

import com.example.carrot.log.OpsLogger;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * 登录审计：监听登录成功 / 密码错误失败，写入业务操作日志。
 * 登出由 SecurityConfig 的 LogoutHandler 记录（需在会话失效前取到用户名）。
 */
@Component
public class AuditEventListener {

    @EventListener
    public void onLoginSuccess(AuthenticationSuccessEvent event) {
        OpsLogger.log("登录成功", "user=" + event.getAuthentication().getName());
    }

    @EventListener
    public void onLoginFailure(AuthenticationFailureBadCredentialsEvent event) {
        OpsLogger.log("登录失败", "user=" + event.getAuthentication().getName() + " 密码错误");
    }
}
