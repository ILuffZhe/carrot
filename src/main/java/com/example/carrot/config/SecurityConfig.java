package com.example.carrot.config;

import com.example.carrot.log.OpsLogger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import javax.sql.DataSource;

/**
 * Spring Security：表单登录 + BCrypt + 记住我。
 *
 * <p>除登录页与静态资源外，所有请求均需认证；认证通过后默认跳转首页看板。</p>
 * <p>登录页勾选「记住我」后，向数据库 persistent_logins 写入持久化令牌，30 天内打开页面自动登录
 * （令牌存储在库中，应用重启后依然有效；退出登录时令牌一并失效）。</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 记住我令牌签名密钥（application.yml 可配置，家庭内网场景保持默认即可） */
    @Value("${carrot.remember-me-key:carrot-home-remember-me-key}")
    private String rememberMeKey;

    /** 记住我有效期：30 天 */
    private static final int REMEMBER_ME_SECONDS = 30 * 24 * 60 * 60;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   PersistentTokenRepository tokenRepository) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/img/**", "/vendor/**", "/uploads/**").permitAll()
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error")
                .permitAll())
            .rememberMe(remember -> remember
                .tokenRepository(tokenRepository)
                .tokenValiditySeconds(REMEMBER_ME_SECONDS)
                .rememberMeParameter("remember-me")
                .key(rememberMeKey))
            .logout(logout -> logout
                .logoutSuccessUrl("/login?logout")
                // 自定义 LogoutHandler 在会话失效前记录操作人（默认处理器在其之后执行）
                .addLogoutHandler((request, response, authentication) -> {
                    if (authentication != null) {
                        OpsLogger.log("退出登录", "user=" + authentication.getName());
                    }
                })
                .permitAll());
        return http.build();
    }

    /**
     * 数据库持久化的记住我令牌仓库：令牌写入 SQLite，应用重启后依然有效。
     * persistent_logins 表由 schema.sql 幂等创建，无需 setCreateTableOnStartup。
     */
    @Bean
    public PersistentTokenRepository persistentTokenRepository(DataSource dataSource) {
        JdbcTokenRepositoryImpl repository = new JdbcTokenRepositoryImpl();
        repository.setDataSource(dataSource);
        return repository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
