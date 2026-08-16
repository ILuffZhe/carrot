package com.example.carrot.controller;

import com.example.carrot.dto.TaskForm;
import com.example.carrot.model.Task;
import com.example.carrot.service.PointService;
import com.example.carrot.service.TaskService;
import com.example.carrot.service.TaskTypeService;
import com.example.carrot.util.PointFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 任务记录：列表 / 登记 / 详情 / 撤销（冲正）。
 */
@Controller
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;
    private final TaskTypeService taskTypeService;
    private final PointService pointService;
    private final PointFormat pointFormat;

    public TaskController(TaskService taskService,
                          TaskTypeService taskTypeService,
                          PointService pointService,
                          PointFormat pointFormat) {
        this.taskService = taskService;
        this.taskTypeService = taskTypeService;
        this.pointService = pointService;
        this.pointFormat = pointFormat;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "today") String range,
                       @RequestParam(defaultValue = "active") String status,
                       Model model) {
        List<Task> tasks = taskService.list(range, status);
        List<Task> positives = new ArrayList<>();
        List<Task> negatives = new ArrayList<>();
        for (Task task : tasks) {
            if (task.isPositive()) {
                positives.add(task);
            } else {
                negatives.add(task);
            }
        }
        model.addAttribute("range", range);
        model.addAttribute("status", status);
        model.addAttribute("positiveTasks", positives);
        model.addAttribute("negativeTasks", negatives);
        model.addAttribute("positiveCount", positives.size());
        model.addAttribute("negativeCount", negatives.size());
        model.addAttribute("currentBalance", pointService.getCurrentBalance());
        return "tasks/list";
    }

    @GetMapping("/record")
    public String recordForm(Model model) {
        prepareRecordForm(model);
        return "tasks/record";
    }

    @PostMapping("/record")
    public String record(@ModelAttribute TaskForm form,
                         @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
                         RedirectAttributes ra) {
        try {
            Task task = taskService.record(form, photos);
            ra.addFlashAttribute("success",
                    "登记成功，" + pointFormat.fmtSigned(task.getEarnedPoints()) + " 积分已入账");
            return "redirect:/tasks/" + task.getId();
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/tasks/record";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Task task = taskService.getById(id);
        model.addAttribute("task", task);
        model.addAttribute("txns", pointService.findByRef(id));
        return "tasks/detail";
    }

    @PostMapping("/{id}/reverse")
    public String reverse(@PathVariable Long id, RedirectAttributes ra) {
        try {
            taskService.reverse(id);
            ra.addFlashAttribute("success", "已撤销该记录，积分已冲正");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/tasks/" + id;
    }

    private void prepareRecordForm(Model model) {
        model.addAttribute("positiveTypes", taskTypeService.findEnabledByKind("POSITIVE"));
        model.addAttribute("negativeTypes", taskTypeService.findEnabledByKind("NEGATIVE"));
        model.addAttribute("today", LocalDate.now().toString());
    }
}
