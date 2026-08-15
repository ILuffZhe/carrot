package com.example.carrot.service;

import com.example.carrot.dto.TaskForm;
import com.example.carrot.model.Task;
import com.example.carrot.model.TaskType;
import com.example.carrot.repository.TaskRepository;
import com.example.carrot.repository.TaskTypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 任务登记核心服务。
 *
 * <p>登记即完成（快照标题与三档积分），积分即时入账；撤销走冲正，不做物理删除。
 * 「登记 + 流水」「撤销 + 冲正流水」都在一个事务内。</p>
 */
@Service
public class TaskService {

    private static final Set<String> IMAGE_EXTS =
            Set.of("jpg", "jpeg", "png", "gif", "webp", "heic", "bmp");
    private static final int MAX_PHOTOS = 3;

    private final TaskRepository taskRepository;
    private final TaskTypeRepository taskTypeRepository;
    private final PointService pointService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String uploadDir;

    public TaskService(TaskRepository taskRepository,
                       TaskTypeRepository taskTypeRepository,
                       PointService pointService,
                       @Value("${carrot.upload-dir:./uploads}") String uploadDir) {
        this.taskRepository = taskRepository;
        this.taskTypeRepository = taskTypeRepository;
        this.pointService = pointService;
        this.uploadDir = uploadDir;
    }

    /**
     * 登记一条记录（正向完成入账或惩罚扣分）。
     */
    @Transactional
    public Task record(TaskForm form, List<MultipartFile> photos) {
        if (form == null || form.getKind() == null) {
            throw new IllegalArgumentException("请选择记录类型");
        }
        String kind = form.getKind();
        String title;
        int base = 0, good = 0, excellent = 0;
        int earned;
        Integer tier = null;

        if ("POSITIVE".equals(kind)) {
            tier = form.getTier();
            if (tier == null || tier < 1 || tier > 3) {
                throw new IllegalArgumentException("请选择完成档次（完成/良好/优秀）");
            }
            if (form.getPositiveTypeId() != null) {
                TaskType type = loadEnabled(form.getPositiveTypeId(), "POSITIVE");
                base = type.getBasePoints();
                good = type.getGoodPoints();
                excellent = type.getExcellentPoints();
                title = type.getName();
            } else {
                title = trimToNull(form.getPositiveCustomTitle());
                if (title == null) {
                    throw new IllegalArgumentException("请填写自定义任务标题");
                }
                base = nvl(form.getPositiveBasePoints());
                good = nvl(form.getPositiveGoodPoints());
                excellent = nvl(form.getPositiveExcellentPoints());
                if (base < 0 || good < 0 || excellent < 0) {
                    throw new IllegalArgumentException("积分不能为负数");
                }
            }
            earned = switch (tier) {
                case 1 -> base;
                case 2 -> good;
                default -> excellent;
            };
        } else if ("NEGATIVE".equals(kind)) {
            int points;
            if (form.getNegativeTypeId() != null) {
                TaskType type = loadEnabled(form.getNegativeTypeId(), "NEGATIVE");
                points = type.getBasePoints();
                title = type.getName();
            } else {
                title = trimToNull(form.getNegativeCustomTitle());
                if (title == null) {
                    throw new IllegalArgumentException("请填写自定义违规标题");
                }
                points = form.getNegativePoints() == null ? 0 : form.getNegativePoints();
                if (points <= 0) {
                    throw new IllegalArgumentException("扣分值必须大于 0");
                }
            }
            earned = -points;
        } else {
            throw new IllegalArgumentException("非法的记录类型");
        }

        String photoJson = savePhotos(photos);
        String taskDate = trimToNull(form.getTaskDate());
        if (taskDate == null) {
            taskDate = LocalDate.now().toString();
        }

        Task task = new Task();
        task.setTaskTypeId(form.getPositiveTypeId() != null
                ? form.getPositiveTypeId() : form.getNegativeTypeId());
        task.setTitle(title);
        task.setBasePoints(base);
        task.setGoodPoints(good);
        task.setExcellentPoints(excellent);
        task.setStatus("COMPLETED");
        task.setTier(tier);
        task.setEarnedPoints(earned);
        task.setTaskDate(taskDate);
        task.setPhotoPaths(photoJson);
        task.setRemark(trimToNull(form.getRemark()));
        task.setId(taskRepository.insert(task));

        String txnType = "POSITIVE".equals(kind) ? "TASK" : "PENALTY";
        String desc = ("POSITIVE".equals(kind) ? "完成任务：" : "记录违规：") + title;
        pointService.add(earned, txnType, task.getId(), desc);
        return task;
    }

    /**
     * 撤销记录：状态置 CANCELLED，并追加等额反向冲正流水。
     */
    @Transactional
    public void reverse(Long id) {
        Task task = getById(id);
        if ("CANCELLED".equals(task.getStatus())) {
            throw new IllegalArgumentException("该记录已撤销，无需重复操作");
        }
        taskRepository.updateStatus(id, "CANCELLED");
        pointService.add(-task.getEarnedPoints(), "ADJUST", id, "撤销记录：" + task.getTitle());
    }

    @Transactional(readOnly = true)
    public Task getById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("记录不存在"));
    }

    /**
     * 记录列表：按日期范围（today/week/all）与状态（active/cancelled/all）筛选。
     */
    @Transactional(readOnly = true)
    public List<Task> list(String range, String status) {
        LocalDate today = LocalDate.now();
        String start = null;
        String end = null;
        if ("today".equals(range)) {
            start = end = today.toString();
        } else if ("week".equals(range)) {
            LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1L);
            start = weekStart.toString();
            end = weekStart.plusDays(6).toString();
        }
        String statusParam = switch (status) {
            case "active" -> "COMPLETED";
            case "cancelled" -> "CANCELLED";
            default -> null;
        };
        return taskRepository.findByFilters(start, end, statusParam);
    }

    private TaskType loadEnabled(Long id, String kind) {
        TaskType type = taskTypeRepository.findEnabledById(id)
                .orElseThrow(() -> new IllegalArgumentException("该任务类型不存在或已停用"));
        if (!kind.equals(type.getKind())) {
            throw new IllegalArgumentException("任务类型与记录类型不匹配");
        }
        return type;
    }

    /**
     * 保存照片到 {uploadDir}/tasks/{yyyyMM}/{uuid}.{ext}，返回 JSON 相对路径数组；无照片返回 null。
     */
    private String savePhotos(List<MultipartFile> photos) {
        if (photos == null) {
            return null;
        }
        List<String> paths = new ArrayList<>();
        for (MultipartFile file : photos) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            if (paths.size() >= MAX_PHOTOS) {
                throw new IllegalArgumentException("最多上传 " + MAX_PHOTOS + " 张照片");
            }
            String ext = resolveExt(file);
            String yyyymm = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            String filename = UUID.randomUUID().toString().replace("-", "");
            try {
                Path dir = Paths.get(uploadDir).toAbsolutePath().normalize()
                        .resolve("tasks").resolve(yyyymm);
                Files.createDirectories(dir);
                file.transferTo(dir.resolve(filename + "." + ext));
            } catch (IOException e) {
                throw new IllegalArgumentException("照片保存失败，请重试", e);
            }
            paths.add("/uploads/tasks/" + yyyymm + "/" + filename + "." + ext);
        }
        if (paths.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(paths);
        } catch (IOException e) {
            throw new IllegalArgumentException("照片信息解析失败", e);
        }
    }

    private String resolveExt(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null) {
            int dot = original.lastIndexOf('.');
            if (dot >= 0) {
                String ext = original.substring(dot + 1).toLowerCase();
                if (IMAGE_EXTS.contains(ext)) {
                    return ext;
                }
            }
        }
        String contentType = file.getContentType();
        if (contentType != null) {
            if (contentType.contains("jpeg")) return "jpg";
            if (contentType.contains("png")) return "png";
            if (contentType.contains("gif")) return "gif";
            if (contentType.contains("webp")) return "webp";
            if (contentType.contains("heic")) return "heic";
        }
        throw new IllegalArgumentException("不支持的图片格式");
    }

    private String trimToNull(String s) {
        return s == null ? null : (s.isBlank() ? null : s.trim());
    }

    private int nvl(Integer i) {
        return i == null ? 0 : i;
    }
}
