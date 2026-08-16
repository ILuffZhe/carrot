package com.example.carrot.repository;

import com.example.carrot.model.Reward;
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
 * 奖励数据访问。
 */
@Repository
public class RewardRepository {

    private static final String SELECT =
            "SELECT id, name, description, points_cost, type, image_path, stock, enabled, created_at "
          + "FROM rewards";
    private static final RowMapper<Reward> MAPPER =
            BeanPropertyRowMapper.newInstance(Reward.class);

    private final JdbcTemplate jdbcTemplate;

    public RewardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Reward> findAll() {
        return jdbcTemplate.query(SELECT + " ORDER BY enabled DESC, id", MAPPER);
    }

    public Optional<Reward> findById(Long id) {
        List<Reward> list = jdbcTemplate.query(SELECT + " WHERE id = ?", MAPPER, id);
        return list.stream().findFirst();
    }

    public Optional<Reward> findEnabledById(Long id) {
        List<Reward> list = jdbcTemplate.query(SELECT + " WHERE id = ? AND enabled = 1", MAPPER, id);
        return list.stream().findFirst();
    }

    public Long insert(Reward reward) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO rewards (name, description, points_cost, type, image_path, stock, enabled) "
                  + "VALUES (?, ?, ?, ?, ?, ?, 1)",
                    new String[]{"id"});
            ps.setString(1, reward.getName());
            ps.setString(2, reward.getDescription());
            ps.setDouble(3, reward.getPointsCost());
            ps.setString(4, reward.getType());
            ps.setString(5, reward.getImagePath());
            if (reward.getStock() == null) {
                ps.setNull(6, java.sql.Types.INTEGER);
            } else {
                ps.setInt(6, reward.getStock());
            }
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void update(Reward reward) {
        jdbcTemplate.update(
                "UPDATE rewards SET name = ?, description = ?, points_cost = ?, type = ?, image_path = ?, stock = ? "
              + "WHERE id = ?",
                reward.getName(), reward.getDescription(), reward.getPointsCost(),
                reward.getType(), reward.getImagePath(),
                reward.getStock() == null ? null : reward.getStock(), reward.getId());
    }

    public void toggleEnabled(Long id) {
        jdbcTemplate.update("UPDATE rewards SET enabled = 1 - enabled WHERE id = ?", id);
    }
}
