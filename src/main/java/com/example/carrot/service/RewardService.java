package com.example.carrot.service;

import com.example.carrot.log.OpsLogger;
import com.example.carrot.model.Reward;
import com.example.carrot.repository.RewardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 奖励管理（CRUD + 上/下架）。兑换与发放/取消在 {@link RedemptionService}。
 */
@Service
public class RewardService {

    private final RewardRepository rewardRepository;
    private final ImageStore imageStore;

    public RewardService(RewardRepository rewardRepository, ImageStore imageStore) {
        this.rewardRepository = rewardRepository;
        this.imageStore = imageStore;
    }

    @Transactional(readOnly = true)
    public List<Reward> findAll() {
        return rewardRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Reward getById(Long id) {
        return rewardRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("奖励不存在"));
    }

    /**
     * 新建奖励。积分必须大于 0；库存留空表示不限量；图片可选。
     */
    @Transactional
    public Reward create(String name, String type, String description,
                         Integer pointsCost, Integer stock, MultipartFile image) {
        Reward reward = validateAndBuild(null, name, type, description, pointsCost, stock);
        reward.setImagePath(imageStore.save(image, "rewards"));
        reward.setEnabled(true);
        reward.setId(rewardRepository.insert(reward));
        OpsLogger.log("新建奖励", String.format(
                "id=%d 名称=%s 类型=%s 积分=%d 库存=%s",
                reward.getId(), reward.getName(), reward.getType(),
                reward.getPointsCost(), reward.getStock() == null ? "不限" : reward.getStock()));
        return reward;
    }

    /**
     * 编辑奖励。未上传新图则保留原图。
     */
    @Transactional
    public void update(Long id, String name, String type, String description,
                       Integer pointsCost, Integer stock, MultipartFile image) {
        Reward reward = validateAndBuild(id, name, type, description, pointsCost, stock);
        String newImage = imageStore.save(image, "rewards");
        reward.setImagePath(newImage != null ? newImage : reward.getImagePath());
        rewardRepository.update(reward);
        OpsLogger.log("编辑奖励", String.format(
                "id=%d 名称=%s 积分=%d 库存=%s",
                id, reward.getName(), reward.getPointsCost(),
                reward.getStock() == null ? "不限" : reward.getStock()));
    }

    @Transactional
    public void toggle(Long id) {
        if (rewardRepository.findById(id).isEmpty()) {
            throw new IllegalArgumentException("奖励不存在");
        }
        rewardRepository.toggleEnabled(id);
        boolean enabled = rewardRepository.findById(id).orElseThrow().isEnabled();
        OpsLogger.log(enabled ? "启用奖励" : "停用奖励", "id=" + id);
    }

    private Reward validateAndBuild(Long id, String name, String type, String description,
                                    Integer pointsCost, Integer stock) {
        name = name == null ? "" : name.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("请填写奖励名称");
        }
        if (!"PHYSICAL".equals(type) && !"ACTIVITY".equals(type)) {
            throw new IllegalArgumentException("请选择奖励类型：实物或活动");
        }
        if (pointsCost == null || pointsCost <= 0) {
            throw new IllegalArgumentException("所需积分需大于 0");
        }
        if (stock != null && stock < 0) {
            throw new IllegalArgumentException("库存不能为负数");
        }

        Reward reward = new Reward();
        if (id != null) {
            reward.setId(id);
            Reward existing = getById(id);
            reward.setImagePath(existing.getImagePath());
            reward.setEnabled(existing.isEnabled());
        }
        reward.setName(name);
        reward.setType(type);
        reward.setDescription(description == null ? null : description.trim());
        reward.setPointsCost(pointsCost);
        reward.setStock(stock);
        return reward;
    }
}
