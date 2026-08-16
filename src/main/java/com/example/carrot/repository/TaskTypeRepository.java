package com.example.carrot.repository;

import com.example.carrot.model.TaskType;
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
 * 任务类型数据访问。
 */
@Repository
public class TaskTypeRepository {

    private static final String SELECT =
            "SELECT id, name, kind, description, icon, base_points, good_points, excellent_points, "
          + "is_builtin AS builtin, enabled, created_at FROM task_types";
    private static final RowMapper<TaskType> MAPPER =
            BeanPropertyRowMapper.newInstance(TaskType.class);

    private final JdbcTemplate jdbcTemplate;

    public TaskTypeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<TaskType> findAll() {
        return jdbcTemplate.query(SELECT + " ORDER BY kind, is_builtin DESC, id", MAPPER);
    }

    public List<TaskType> findEnabledByKind(String kind) {
        return jdbcTemplate.query(SELECT + " WHERE kind = ? AND enabled = 1 ORDER BY id", MAPPER, kind);
    }

    public Optional<TaskType> findById(Long id) {
        List<TaskType> list = jdbcTemplate.query(SELECT + " WHERE id = ?", MAPPER, id);
        return list.stream().findFirst();
    }

    public Optional<TaskType> findEnabledById(Long id) {
        List<TaskType> list = jdbcTemplate.query(SELECT + " WHERE id = ? AND enabled = 1", MAPPER, id);
        return list.stream().findFirst();
    }

    public boolean existsByNameAndKind(String name, String kind) {
        return existsByNameAndKindExcluding(name, kind, null);
    }

    /**
     * 是否存在同名同类型的记录，可排除指定 id（编辑时用于排除自身）。
     */
    public boolean existsByNameAndKindExcluding(String name, String kind, Long excludeId) {
        String sql = "SELECT COUNT(*) FROM task_types WHERE name = ? AND kind = ?";
        if (excludeId != null) {
            sql += " AND id != " + excludeId;
        }
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, name, kind);
        return count != null && count > 0;
    }

    public void update(TaskType type) {
        jdbcTemplate.update(
                "UPDATE task_types SET name = ?, kind = ?, description = ?, icon = ?, "
              + "base_points = ?, good_points = ?, excellent_points = ? WHERE id = ?",
                type.getName(), type.getKind(), type.getDescription(), type.getIcon(),
                type.getBasePoints(), type.getGoodPoints(), type.getExcellentPoints(), type.getId());
    }

    public Long insert(TaskType type) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO task_types (name, kind, description, icon, base_points, good_points, excellent_points, is_builtin, enabled) "
                  + "VALUES (?, ?, ?, ?, ?, ?, ?, 0, 1)",
                    new String[]{"id"});
            ps.setString(1, type.getName());
            ps.setString(2, type.getKind());
            ps.setString(3, type.getDescription());
            ps.setString(4, type.getIcon());
            ps.setInt(5, type.getBasePoints());
            ps.setInt(6, type.getGoodPoints());
            ps.setInt(7, type.getExcellentPoints());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public void toggleEnabled(Long id) {
        jdbcTemplate.update("UPDATE task_types SET enabled = 1 - enabled WHERE id = ?", id);
    }
}
