package com.example.carrot.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 全局异常处理：上传超限时重定向回登记页并给出友好提示，
 * 避免走容器默认的 413 空白错误页。
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleUploadTooLarge(RedirectAttributes ra) {
        ra.addFlashAttribute("error", "照片文件过大，单张不能超过 5MB");
        return "redirect:/tasks/record";
    }
}
