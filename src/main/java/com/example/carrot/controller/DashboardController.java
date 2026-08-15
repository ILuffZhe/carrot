package com.example.carrot.controller;

import com.example.carrot.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 数据看板（首页）：当前积分、今日概览、7/30 天趋势、最近流水与兑换。
 */
@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("stats", dashboardService.stats());
        return "dashboard";
    }
}
