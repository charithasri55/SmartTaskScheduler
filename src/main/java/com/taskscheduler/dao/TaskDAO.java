package com.taskscheduler.dao;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.taskscheduler.model.Task;
import com.taskscheduler.util.DBConnection;

public class TaskDAO {

    private static final String PRIORITY_ORDER =
            "CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END";

    // Add a new task
    public void addTask(Task task) throws SQLException {
        String sql = "INSERT INTO tasks (title, description, priority, deadline, status) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getPriority());
            ps.setDate(4, Date.valueOf(task.getDeadline()));
            ps.setString(5, task.getStatus());
            ps.executeUpdate();
        }
    }

    // Update an existing task
    public boolean updateTask(Task task) throws SQLException {
        String sql = "UPDATE tasks SET title = ?, description = ?, priority = ?, "
                   + "deadline = ?, status = ? WHERE task_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setString(3, task.getPriority());
            ps.setDate(4, Date.valueOf(task.getDeadline()));
            ps.setString(5, task.getStatus());
            ps.setInt(6, task.getId());
            return ps.executeUpdate() > 0;
        }
    }

    // Delete a task
    public boolean deleteTask(int id) throws SQLException {
        String sql = "DELETE FROM tasks WHERE task_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Mark a task as completed
    public boolean markCompleted(int id) throws SQLException {
        String sql = "UPDATE tasks SET status = 'COMPLETED' WHERE task_id = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    // Get tasks with filter and sort
    // filter: all | today | overdue | high | pending | completed
    // sort:   priority | deadline | (anything else = newest first)
    public List<Task> getTasks(String filter, String sort) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT task_id, title, description, priority, deadline, status FROM tasks");

        if (filter == null) {
            filter = "all";
        }
        switch (filter) {
            case "today":
                sql.append(" WHERE TRUNC(deadline) = TRUNC(SYSDATE)");
                break;
            case "overdue":
                sql.append(" WHERE TRUNC(deadline) < TRUNC(SYSDATE) AND status = 'PENDING'");
                break;
            case "high":
                sql.append(" WHERE priority = 'HIGH'");
                break;
            case "pending":
                sql.append(" WHERE status = 'PENDING'");
                break;
            case "completed":
                sql.append(" WHERE status = 'COMPLETED'");
                break;
            default:
                break; // all tasks
        }

        if ("priority".equals(sort)) {
            sql.append(" ORDER BY ").append(PRIORITY_ORDER).append(", deadline ASC");
        } else if ("deadline".equals(sort)) {
            sql.append(" ORDER BY deadline ASC, ").append(PRIORITY_ORDER);
        } else {
            sql.append(" ORDER BY task_id DESC");
        }

        List<Task> list = new ArrayList<>();
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString());
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Task t = new Task();
                t.setId(rs.getInt("task_id"));
                t.setTitle(rs.getString("title"));
                String desc = rs.getString("description");
                t.setDescription(desc == null ? "" : desc);
                t.setPriority(rs.getString("priority"));
                t.setDeadline(rs.getDate("deadline").toString()); // yyyy-MM-dd
                t.setStatus(rs.getString("status"));
                list.add(t);
            }
        }
        return list;
    }
}