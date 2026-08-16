package com.example.carrot.service;

import com.example.carrot.log.OpsLogger;
import com.example.carrot.model.PointTransaction;
import com.example.carrot.repository.PointRepository;
import com.example.carrot.util.PointMath;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 积分账本核心：所有积分变动都经由这里写入流水（只追加）。
 * 调用方需处于事务中，保证「业务变更 + 流水」原子。
 *
 * <p>惰性按天复利：余额按年化利率（carrot.interest-rate）每天复利增长，
 * 在每次加/减流水或查询余额时结算，无需调度器。利息记入一条 INTEREST 流水。</p>
 */
@Service
public class PointService {

    private final PointRepository pointRepository;
    private final double interestRate;

    public PointService(PointRepository pointRepository,
                        @Value("${carrot.interest-rate:0.02}") double interestRate) {
        this.pointRepository = pointRepository;
        this.interestRate = interestRate;
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
    public PointTransaction add(double changeAmount, String type, Long refId, String description) {
        accrueInterest();
        double balance = pointRepository.getCurrentBalance();
        double change = PointMath.round2(changeAmount);
        PointTransaction txn = new PointTransaction();
        txn.setChangeAmount(change);
        txn.setBalanceAfter(PointMath.round2(balance + change));
        txn.setType(type);
        txn.setRefId(refId);
        txn.setDescription(description);
        pointRepository.insert(txn);
        OpsLogger.log("积分流水", String.format(
                "type=%s change=%+.2f balance=%.2f refId=%s | %s",
                type, change, txn.getBalanceAfter(), refId, description));
        return txn;
    }

    /**
     * 当前余额（查询前先结算利息）。
     */
    @Transactional
    public double getCurrentBalance() {
        accrueInterest();
        return pointRepository.getCurrentBalance();
    }

    @Transactional(readOnly = true)
    public List<PointTransaction> findByRef(Long refId) {
        return pointRepository.findByRef(refId);
    }

    /**
     * 惰性结算利息：自上次结算日至今天按天复利一次入账。
     * 空余额或非正余额只推进结算日期，不产生流水。幂等（同一天重复调用为 no-op）。
     */
    private void accrueInterest() {
        String lastStr = pointRepository.getLastAccrualDate();
        LocalDate today = LocalDate.now();
        LocalDate last;
        try {
            last = lastStr == null ? today : LocalDate.parse(lastStr);
        } catch (Exception e) {
            last = today;
        }
        if (last.isAfter(today)) {
            last = today;
        }
        long days = ChronoUnit.DAYS.between(last, today);
        if (days <= 0) {
            return;
        }

        double balance = pointRepository.getCurrentBalance();
        if (balance > 0) {
            double factor = Math.pow(1 + interestRate / 365.0, days);
            double newBalance = PointMath.round2(balance * factor);
            double interest = PointMath.round2(newBalance - balance);
            if (interest != 0) {
                PointTransaction txn = new PointTransaction();
                txn.setChangeAmount(interest);
                txn.setBalanceAfter(newBalance);
                txn.setType("INTEREST");
                txn.setRefId(null);
                txn.setDescription(String.format("利息（年化%.0f%%）", interestRate * 100));
                pointRepository.insert(txn);
                OpsLogger.log("利息结算", String.format(
                        "days=%d rate=%.2f%% interest=%+.2f balance=%.2f",
                        days, interestRate * 100, interest, newBalance));
            }
        }
        pointRepository.updateLastAccrualDate(today.toString());
    }
}
