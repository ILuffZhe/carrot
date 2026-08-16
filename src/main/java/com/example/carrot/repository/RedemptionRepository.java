package com.example.carrot.repository;

import com.example.carrot.model.Redemption;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

/**
 * 兑换记录数据访问。
 */
@Repository
public class RedemptionRepository {

    private static final String SELECT =
            "SELECT id, reward_id, reward_name, points_cost, status, redeemed_at, completed_at, note, created_by, created_at "
          + "FROM redemptions";
    private static final RowMapper<Redemption> MAPPER =
            BeanPropertyRowMapper.newInstance(Redemption.class);

    private final JdbcTemplate jdbcTemplate;

    public RedemptionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Redemption> findAll() {
        return jdbcTemplate.query(SELECT + " ORDER BY id DESC", MAPPER);
    }

    public Optional<Redemption> findById(Long id) {
        List<Redemption> list = jdbcTemplate.query(SELECT + " WHERE id = ?", MAPPER, id);
        return list.stream().findFirst();
    }

    public Long insert(Redemption redemption) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO redemptions (reward_id, reward_name, points_cost, status, created_by) "
                  + "VALUES (?, ?, ?, 'PENDING', ?)",
                    new String[]{"id"});
            ps.setLong(1, redemption.getRewardId());
            ps.setString(2, redemption.getRewardName());
            ps.setDouble(3, redemption.getPointsCost());
            ps.setString(4, redemption.getCreatedBy());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update(
                "UPDATE redemptions SET status = ?, completed_at = datetime('now','localtime') WHERE id = ?",
                status, id);
    }

    /**
     * 最近 N 条兑换记录（看板用，倒序）。
     */
    public List<Redemption> findRecent(int limit) {
        return jdbcTemplate.query(SELECT + " ORDER BY id DESC LIMIT ?", MAPPER, limit);
    }
}
