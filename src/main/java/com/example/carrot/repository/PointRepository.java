package com.example.carrot.repository;

import com.example.carrot.dto.TrendPoint;
import com.example.carrot.model.PointTransaction;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 积分流水数据访问（账本只追加，不做 UPDATE/DELETE）。
 */
@Repository
public class PointRepository {

    private static final String SELECT =
            "SELECT id, change_amount, balance_after, type, ref_id, description, created_at "
          + "FROM point_transactions";
    private static final RowMapper<PointTransaction> MAPPER =
            BeanPropertyRowMapper.newInstance(PointTransaction.class);

    private final JdbcTemplate jdbcTemplate;

    public PointRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 当前余额 = 最近一条流水的 balance_after；无流水时为 0。
     */
    public int getCurrentBalance() {
        Integer balance = jdbcTemplate.queryForObject(
                "SELECT COALESCE((SELECT balance_after FROM point_transactions ORDER BY id DESC LIMIT 1), 0)",
                Integer.class);
        return balance == null ? 0 : balance;
    }

    public void insert(PointTransaction txn) {
        jdbcTemplate.update(
                "INSERT INTO point_transactions (change_amount, balance_after, type, ref_id, description) "
              + "VALUES (?, ?, ?, ?, ?)",
                txn.getChangeAmount(), txn.getBalanceAfter(), txn.getType(),
                txn.getRefId(), txn.getDescription());
    }

    public List<PointTransaction> findByRef(Long refId) {
        return jdbcTemplate.query(SELECT + " WHERE ref_id = ? ORDER BY id", MAPPER, refId);
    }

    /**
     * 最近 N 条流水（看板用，倒序）。
     */
    public List<PointTransaction> findRecent(int limit) {
        return jdbcTemplate.query(SELECT + " ORDER BY id DESC LIMIT ?", MAPPER, limit);
    }

    /**
     * 某日（YYYY-MM-DD）净积分变动合计。
     */
    public int sumChangeOn(String date) {
        Integer sum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(change_amount), 0) FROM point_transactions WHERE date(created_at) = ?",
                Integer.class, date);
        return sum == null ? 0 : sum;
    }

    /**
     * 自 startDate（YYYY-MM-DD，含）起按天聚合：入账 / 扣减 / 净变动。
     */
    public List<TrendPoint> dailyTrend(String startDate) {
        return jdbcTemplate.query(
                "SELECT date(created_at) AS date, "
              + "  COALESCE(SUM(CASE WHEN change_amount > 0 THEN change_amount ELSE 0 END), 0) AS income, "
              + "  COALESCE(SUM(CASE WHEN change_amount < 0 THEN -change_amount ELSE 0 END), 0) AS deduction, "
              + "  COALESCE(SUM(change_amount), 0) AS net "
              + "FROM point_transactions WHERE date(created_at) >= ? "
              + "GROUP BY date(created_at) ORDER BY date(created_at)",
                BeanPropertyRowMapper.newInstance(TrendPoint.class), startDate);
    }
}
