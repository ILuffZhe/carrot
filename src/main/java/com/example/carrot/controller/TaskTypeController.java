package com.example.carrot.controller;

import com.example.carrot.model.TaskType;
import com.example.carrot.service.TaskTypeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 任务类型管理：列表 / 新建（正向三档 or 惩罚单次扣分）/ 启停用。
 */
@Controller
@RequestMapping("/task-types")
public class TaskTypeController {

    private final TaskTypeService taskTypeService;

    public TaskTypeController(TaskTypeService taskTypeService) {
        this.taskTypeService = taskTypeService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("types", taskTypeService.findAll());
        return "task-types";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("type", new TaskType());
        return "task-type-form";
    }

    @PostMapping
    public String create(@RequestParam String kind,
                         @RequestParam String name,
                         @RequestParam(required = false) String icon,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) Integer basePoints,
                         @RequestParam(required = false) Integer goodPoints,
                         @RequestParam(required = false) Integer excellentPoints,
                         RedirectAttributes ra) {
        try {
            TaskType type = taskTypeService.create(kind, name, icon, description,
                    basePoints, goodPoints, excellentPoints);
            ra.addFlashAttribute("success", "已新建任务类型：" + type.getName());
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/task-types";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        try {
            model.addAttribute("type", taskTypeService.getById(id));
        } catch (IllegalArgumentException e) {
            return "redirect:/task-types";
        }
        return "task-type-form";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id,
                       @RequestParam String kind,
                       @RequestParam String name,
                       @RequestParam(required = false) String icon,
                       @RequestParam(required = false) String description,
                       @RequestParam(required = false) Integer basePoints,
                       @RequestParam(required = false) Integer goodPoints,
                       @RequestParam(required = false) Integer excellentPoints,
                       RedirectAttributes ra) {
        try {
            taskTypeService.update(id, kind, name, icon, description,
                    basePoints, goodPoints, excellentPoints);
            ra.addFlashAttribute("success", "任务类型已更新：" + name.trim());
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/task-types";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes ra) {
        try {
            taskTypeService.toggle(id);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/task-types";
    }
}
