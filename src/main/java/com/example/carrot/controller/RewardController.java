package com.example.carrot.controller;

import com.example.carrot.dto.RewardForm;
import com.example.carrot.model.Reward;
import com.example.carrot.service.PointService;
import com.example.carrot.service.RedemptionService;
import com.example.carrot.service.RewardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 奖励管理：列表 / 新建 / 编辑 / 上架下架 / 发起兑换。
 */
@Controller
@RequestMapping("/rewards")
public class RewardController {

    private final RewardService rewardService;
    private final RedemptionService redemptionService;
    private final PointService pointService;

    public RewardController(RewardService rewardService,
                            RedemptionService redemptionService,
                            PointService pointService) {
        this.rewardService = rewardService;
        this.redemptionService = redemptionService;
        this.pointService = pointService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rewards", rewardService.findAll());
        model.addAttribute("currentBalance", pointService.getCurrentBalance());
        return "rewards";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("reward", new Reward());
        return "rewards-form";
    }

    @PostMapping
    public String create(@ModelAttribute RewardForm form,
                         @RequestParam(value = "image", required = false) MultipartFile image,
                         RedirectAttributes ra) {
        try {
            Reward reward = rewardService.create(form.getName(), form.getType(),
                    form.getDescription(), form.getPointsCost(), form.getStock(), image);
            ra.addFlashAttribute("success", "已新建奖励：" + reward.getName());
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rewards";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("reward", rewardService.getById(id));
        } catch (IllegalArgumentException e) {
            return "redirect:/rewards";
        }
        return "rewards-form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @ModelAttribute RewardForm form,
                       @RequestParam(value = "image", required = false) MultipartFile image,
                       RedirectAttributes ra) {
        try {
            rewardService.update(id, form.getName(), form.getType(),
                    form.getDescription(), form.getPointsCost(), form.getStock(), image);
            ra.addFlashAttribute("success", "奖励已更新");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rewards";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        try {
            rewardService.toggle(id);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rewards";
    }

    @PostMapping("/{id}/redeem")
    public String redeem(@PathVariable Long id, RedirectAttributes ra) {
        try {
            redemptionService.redeem(id);
            ra.addFlashAttribute("success", "兑换成功，积分已扣减");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/redemptions";
    }
}
