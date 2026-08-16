package com.example.carrot.service;

import com.example.carrot.log.OpsLogger;
import com.example.carrot.model.TaskType;
import com.example.carrot.repository.TaskTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 任务类型管理（内置 + 自定义）。内置类型不可删除，可停用；自定义同样可停用。
 */
@Service
public class TaskTypeService {

    private final TaskTypeRepository taskTypeRepository;

    public TaskTypeService(TaskTypeRepository taskTypeRepository) {
        this.taskTypeRepository = taskTypeRepository;
    }

    @Transactional(readOnly = true)
    public List<TaskType> findAll() {
        return taskTypeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TaskType> findEnabledByKind(String kind) {
        return taskTypeRepository.findEnabledByKind(kind);
    }

    @Transactional(readOnly = true)
    public TaskType getById(Long id) {
        return taskTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("任务类型不存在"));
    }

    /**
     * 新建任务类型。POSITIVE 需三档积分；NEGATIVE 只需单次扣分值（存 base_points）。
     */
    @Transactional
    public TaskType create(String kind, String name, String icon, String description,
                           Double basePoints, Double goodPoints, Double excellentPoints) {
        TaskType type = validateAndBuild(null, kind, name, icon, description,
                basePoints, goodPoints, excellentPoints);
        type.setBuiltin(false);
        type.setEnabled(true);
        type.setId(taskTypeRepository.insert(type));
        OpsLogger.log("新建任务类型", String.format(
                "id=%d 类型=%s 名称=%s 三档积分=%.2f/%.2f/%.2f",
                type.getId(), kind, type.getName(),
                type.getBasePoints(), type.getGoodPoints(), type.getExcellentPoints()));
        return type;
    }

    /**
     * 编辑任务类型。仅更新可编辑字段，保留 builtin / enabled 状态。
     */
    @Transactional
    public void update(Long id, String kind, String name, String icon, String description,
                       Double basePoints, Double goodPoints, Double excellentPoints) {
        TaskType existing = getById(id);
        TaskType type = validateAndBuild(id, kind, name, icon, description,
                basePoints, goodPoints, excellentPoints);
        type.setBuiltin(existing.isBuiltin());
        type.setEnabled(existing.isEnabled());
        taskTypeRepository.update(type);
        OpsLogger.log("编辑任务类型", String.format(
                "id=%d 类型=%s 名称=%s 三档积分=%.2f/%.2f/%.2f",
                id, kind, type.getName(),
                type.getBasePoints(), type.getGoodPoints(), type.getExcellentPoints()));
    }

    /**
     * 校验并构建 TaskType。id 为 null 时用于新建，否则用于编辑（名称唯一性排除自身）。
     */
    private TaskType validateAndBuild(Long id, String kind, String name, String icon, String description,
                                      Double basePoints, Double goodPoints, Double excellentPoints) {
        if (!"POSITIVE".equals(kind) && !"NEGATIVE".equals(kind)) {
            throw new IllegalArgumentException("请选择类型：正向任务或惩罚项");
        }
        name = name == null ? "" : name.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("请填写类型名称");
        }
        if (taskTypeRepository.existsByNameAndKindExcluding(name, kind, id)) {
            throw new IllegalArgumentException("该名称已存在：" + name);
        }
        if (basePoints == null || basePoints < 0) {
            throw new IllegalArgumentException("积分不能为空且不能为负数");
        }
        if ("POSITIVE".equals(kind)) {
            if (goodPoints == null || excellentPoints == null) {
                throw new IllegalArgumentException("正向任务需填写三档积分");
            }
            if (goodPoints < 0 || excellentPoints < 0) {
                throw new IllegalArgumentException("积分不能为负数");
            }
            if (goodPoints < basePoints || excellentPoints < goodPoints) {
                throw new IllegalArgumentException("积分应满足：完成 ≤ 良好 ≤ 优秀");
            }
        } else {
            if (basePoints <= 0) {
                throw new IllegalArgumentException("惩罚项扣分值需大于 0");
            }
        }

        TaskType type = new TaskType();
        type.setId(id);
        type.setKind(kind);
        type.setName(name);
        type.setIcon(icon == null ? null : icon.trim());
        type.setDescription(description == null ? null : description.trim());
        type.setBasePoints(basePoints);
        type.setGoodPoints(goodPoints == null ? 0 : goodPoints);
        type.setExcellentPoints(excellentPoints == null ? 0 : excellentPoints);
        return type;
    }

    @Transactional
    public void toggle(Long id) {
        if (taskTypeRepository.findById(id).isEmpty()) {
            throw new IllegalArgumentException("任务类型不存在");
        }
        taskTypeRepository.toggleEnabled(id);
        boolean enabled = taskTypeRepository.findById(id).orElseThrow().isEnabled();
        OpsLogger.log(enabled ? "启用任务类型" : "停用任务类型", "id=" + id);
    }
}
