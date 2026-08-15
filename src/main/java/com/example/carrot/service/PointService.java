package com.example.carrot.service;

import com.example.carrot.model.PointTransaction;
import com.example.carrot.repository.PointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 积分账本核心：所有积分变动都经由这里写入流水（只追加）。
 * 调用方需处于事务中，保证「业务变更 + 流水」原子。
 */
@Service
public class PointService {

    private final PointRepository pointRepository;

    public PointService(PointRepository pointRepository) {
        this.pointRepository = pointRepository;
    }

    /**
     * 追加一条流水，自动计算变动后余额。
     *
     * @param changeAmount 变动额（正=入账 负=扣减）
     * @param type         TASK / PENALTY / REDEEM / ADJUST
     * @param refId        关联记录 id
     * @param description  说明
     * @return 已写入的流水
     */
    @Transactional
    public PointTransaction add(int changeAmount, String type, Long refId, String description) {
        int balance = pointRepository.getCurrentBalance();
        PointTransaction txn = new PointTransaction();
        txn.setChangeAmount(changeAmount);
        txn.setBalanceAfter(balance + changeAmount);
        txn.setType(type);
        txn.setRefId(refId);
        txn.setDescription(description);
        pointRepository.insert(txn);
        return txn;
    }

    @Transactional(readOnly = true)
    public int getCurrentBalance() {
        return pointRepository.getCurrentBalance();
    }

    @Transactional(readOnly = true)
    public List<PointTransaction> findByRef(Long refId) {
        return pointRepository.findByRef(refId);
    }
}
