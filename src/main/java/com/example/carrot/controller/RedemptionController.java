package com.example.carrot.controller;

import com.example.carrot.service.PointService;
import com.example.carrot.service.RedemptionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 兑换记录：列表 / 标记发放完成 / 取消（退回积分）。
 */
@Controller
@RequestMapping("/redemptions")
public class RedemptionController {

    private final RedemptionService redemptionService;
    private final PointService pointService;

    public RedemptionController(RedemptionService redemptionService, PointService pointService) {
        this.redemptionService = redemptionService;
        this.pointService = pointService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("redemptions", redemptionService.findAll());
        model.addAttribute("currentBalance", pointService.getCurrentBalance());
        return "redemptions";
    }

    @PostMapping("/{id}/done")
    public String done(@PathVariable Long id, RedirectAttributes ra) {
        try {
            redemptionService.markDone(id);
            ra.addFlashAttribute("success", "已标记发放完成");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/redemptions";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes ra) {
        try {
            redemptionService.cancel(id);
            ra.addFlashAttribute("success", "已取消兑换，积分已退回");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/redemptions";
    }
}
