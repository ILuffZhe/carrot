package com.example.carrot.repository;

import com.example.carrot.model.Task;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 任务记录数据访问。
 */
@Repository
public class TaskRepository {

    private static final String SELECT =
            "SELECT id, task_type_id, title, description, base_points, good_points, excellent_points, "
          + "status, tier, earned_points, task_date, completed_at, photo_paths, remark, created_at FROM tasks";
    private static final RowMapper<Task> MAPPER = BeanPropertyRowMapper.newInstance(Task.class);

    private final JdbcTemplate jdbcTemplate;

    public TaskRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long insert(Task task) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO tasks (task_type_id, title, description, base_points, good_points, excellent_points, "
                  + "status, tier, earned_points, task_date, completed_at, photo_paths, remark) "
                  + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, datetime('now','localtime'), ?, ?)",
                    new String[]{"id"});
            ps.setObject(1, task.getTaskTypeId());
            ps.setString(2, task.getTitle());
            ps.setString(3, task.getDescription());
            ps.setInt(4, task.getBasePoints());
            ps.setInt(5, task.getGoodPoints());
            ps.setInt(6, task.getExcellentPoints());
            ps.setString(7, task.getStatus());
            ps.setObject(8, task.getTier());
            ps.setObject(9, task.getEarnedPoints());
            ps.setString(10, task.getTaskDate());
            ps.setString(11, task.getPhotoPaths());
            ps.setString(12, task.getRemark());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    public Optional<Task> findById(Long id) {
        List<Task> list = jdbcTemplate.query(SELECT + " WHERE id = ?", MAPPER, id);
        return list.stream().findFirst();
    }

    /**
     * 按日期范围与状态筛选。start/end 为 null 时不限制；status 为 null 时不限制。
     */
    public List<Task> findByFilters(String start, String end, String status) {
        List<String> where = new ArrayList<>();
        List<Object> params = new ArrayList<>();
        if (start != null) {
            where.add("task_date >= ?");
            params.add(start);
        }
        if (end != null) {
            where.add("task_date <= ?");
            params.add(end);
        }
        if (status != null) {
            where.add("status = ?");
            params.add(status);
        }
        String sql = SELECT;
        if (!where.isEmpty()) {
            sql += " WHERE " + String.join(" AND ", where);
        }
        sql += " ORDER BY task_date DESC, id DESC";
        return jdbcTemplate.query(sql, MAPPER, params.toArray());
    }

    public void updateStatus(Long id, String status) {
        jdbcTemplate.update("UPDATE tasks SET status = ? WHERE id = ?", status, id);
    }

    /**
     * 某日（YYYY-MM-DD）已登记记录数。positive=true 计正向任务（tier 非空），false 计惩罚项。
     */
    public int countByDateAndTier(String date, boolean positive) {
        String tierCond = positive ? "tier IS NOT NULL" : "tier IS NULL";
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tasks WHERE task_date = ? AND status = 'COMPLETED' AND " + tierCond,
                Integer.class, date);
        return count == null ? 0 : count;
    }
}
