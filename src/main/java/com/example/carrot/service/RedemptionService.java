package com.example.carrot.service;

import com.example.carrot.model.Redemption;
import com.example.carrot.model.Reward;
import com.example.carrot.repository.RedemptionRepository;
import com.example.carrot.repository.RewardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 兑换业务：发起兑换（余额校验 + REDEEM 扣分 + 生成 PENDING 记录）、
 * 发放完成、取消（退回积分，写 ADJUST 冲正流水）。三者在各自事务内原子完成。
 */
@Service
public class RedemptionService {

    private final RedemptionRepository redemptionRepository;
    private final RewardRepository rewardRepository;
    private final PointService pointService;

    public RedemptionService(RedemptionRepository redemptionRepository,
                             RewardRepository rewardRepository,
                             PointService pointService) {
        this.redemptionRepository = redemptionRepository;
        this.rewardRepository = rewardRepository;
        this.pointService = pointService;
    }

    @Transactional(readOnly = true)
    public List<Redemption> findAll() {
        return redemptionRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Redemption getById(Long id) {
        return redemptionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("兑换记录不存在"));
    }

    /**
     * 发起兑换：校验奖励上架、库存、余额足够，然后在一个事务内
     * 「插入 PENDING 兑换记录 + 写入 REDEEM 扣分流水」。
     */
    @Transactional
    public Redemption redeem(Long rewardId) {
        Reward reward = rewardRepository.findEnabledById(rewardId)
                .orElseThrow(() -> new IllegalArgumentException("该奖励不存在或已下架"));
        if (reward.getStock() != null && reward.getStock() <= 0) {
            throw new IllegalArgumentException("该奖励已无库存");
        }
        int balance = pointService.getCurrentBalance();
        if (balance < reward.getPointsCost()) {
            throw new IllegalArgumentException(
                    "积分不足，还差 " + (reward.getPointsCost() - balance) + " 分");
        }

        Redemption redemption = new Redemption();
        redemption.setRewardId(reward.getId());
        redemption.setRewardName(reward.getName());
        redemption.setPointsCost(reward.getPointsCost());
        redemption.setStatus("PENDING");
        redemption.setId(redemptionRepository.insert(redemption));

        pointService.add(-reward.getPointsCost(), "REDEEM", redemption.getId(),
                "兑换：" + reward.getName());
        return redemption;
    }

    /**
     * 标记发放完成。仅 PENDING 可操作。
     */
    @Transactional
    public void markDone(Long id) {
        Redemption redemption = getById(id);
        if (!redemption.isPending()) {
            throw new IllegalArgumentException("只有待发放的兑换可以标记发放");
        }
        redemptionRepository.updateStatus(id, "DONE");
    }

    /**
     * 取消兑换：退回积分（ADJUST 冲正流水），状态置 CANCELLED。仅 PENDING 可操作。
     */
    @Transactional
    public void cancel(Long id) {
        Redemption redemption = getById(id);
        if (!redemption.isPending()) {
            throw new IllegalArgumentException("只有待发放的兑换可以取消");
        }
        redemptionRepository.updateStatus(id, "CANCELLED");
        pointService.add(redemption.getPointsCost(), "ADJUST", id,
                "取消兑换退回：" + redemption.getRewardName());
    }
}
