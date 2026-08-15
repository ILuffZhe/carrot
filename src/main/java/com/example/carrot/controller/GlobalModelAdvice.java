package com.example.carrot.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 全局模型属性：向所有视图注入当前请求 URI，供导航高亮使用。
 *
 * <p>Thymeleaf 3.1 起默认不再暴露 #{request} 表达式对象，故改为通过模型传递。</p>
 */
@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute
    public void addCurrentUri(HttpServletRequest request, Model model) {
        model.addAttribute("currentUri", request.getRequestURI());
    }
}
