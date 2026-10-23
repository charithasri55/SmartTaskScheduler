package com.taskscheduler.servlet;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.taskscheduler.dao.TaskDAO;
import com.taskscheduler.model.Task;

@WebServlet("/tasks")
public class TaskServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final TaskDAO dao = new TaskDAO();

    // GET /tasks?filter=all&sort=priority
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        setJson(response);
        try {
            List<Task> tasks = dao.getTasks(request.getParameter("filter"), request.getParameter("sort"));
            response.getWriter().write(toJson(tasks));
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(response, 500, "error", "Database error: " + e.getMessage());
        }
    }

    // POST /tasks  with action = add | update | delete | complete
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        setJson(response);

        String action = request.getParameter("action");
        if (action == null) {
            sendMessage(response, 400, "error", "Action is required");
            return;
        }

        try {
            switch (action) {
                case "add": {
                    Task task = readTask(request);
                    String error = validate(task);
                    if (error != null) {
                        sendMessage(response, 400, "error", error);
                        return;
                    }
                    dao.addTask(task);
                    sendMessage(response, 200, "message", "Task added successfully");
                    break;
                }
                case "update": {
                    Task task = readTask(request);
                    task.setId(Integer.parseInt(request.getParameter("id")));
                    String error = validate(task);
                    if (error != null) {
                        sendMessage(response, 400, "error", error);
                        return;
                    }
                    if (dao.updateTask(task)) {
                        sendMessage(response, 200, "message", "Task updated successfully");
                    } else {
                        sendMessage(response, 404, "error", "Task not found");
                    }
                    break;
                }
                case "delete": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    if (dao.deleteTask(id)) {
                        sendMessage(response, 200, "message", "Task deleted successfully");
                    } else {
                        sendMessage(response, 404, "error", "Task not found");
                    }
                    break;
                }
                case "complete": {
                    int id = Integer.parseInt(request.getParameter("id"));
                    if (dao.markCompleted(id)) {
                        sendMessage(response, 200, "message", "Task marked as completed");
                    } else {
                        sendMessage(response, 404, "error", "Task not found");
                    }
                    break;
                }
                default:
                    sendMessage(response, 400, "error", "Unknown action: " + action);
            }
        } catch (IllegalArgumentException e) { // covers NumberFormatException too
            sendMessage(response, 400, "error", "Invalid input");
        } catch (SQLException e) {
            e.printStackTrace();
            sendMessage(response, 500, "error", "Database error: " + e.getMessage());
        }
    }

    // ---------- helper methods ----------

    private Task readTask(HttpServletRequest request) {
        Task t = new Task();
        t.setTitle(trim(request.getParameter("title")));
        t.setDescription(trim(request.getParameter("description")));
        String priority = trim(request.getParameter("priority"));
        t.setPriority(priority.isEmpty() ? "MEDIUM" : priority.toUpperCase());
        t.setDeadline(trim(request.getParameter("deadline")));
        String status = trim(request.getParameter("status"));
        t.setStatus(status.isEmpty() ? "PENDING" : status.toUpperCase());
        return t;
    }

    private String validate(Task t) {
        if (t.getTitle().isEmpty()) {
            return "Title is required";
        }
        if (t.getTitle().length() > 100) {
            return "Title must be 100 characters or less";
        }
        if (t.getDescription().length() > 500) {
            return "Description must be 500 characters or less";
        }
        if (!t.getPriority().equals("HIGH") && !t.getPriority().equals("MEDIUM")
                && !t.getPriority().equals("LOW")) {
            return "Priority must be HIGH, MEDIUM or LOW";
        }
        if (!t.getStatus().equals("PENDING") && !t.getStatus().equals("COMPLETED")) {
            return "Status must be PENDING or COMPLETED";
        }
        try {
            Date.valueOf(t.getDeadline());
        } catch (IllegalArgumentException e) {
            return "Deadline must be a valid date (yyyy-MM-dd)";
        }
        return null;
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    private void setJson(HttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
    }

    private void sendMessage(HttpServletResponse response, int status, String key, String text)
            throws IOException {
        response.setStatus(status);
        response.getWriter().write("{\"" + key + "\":\"" + escape(text) + "\"}");
    }

    private String toJson(List<Task> tasks) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < tasks.size(); i++) {
            Task t = tasks.get(i);
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{")
              .append("\"id\":").append(t.getId()).append(",")
              .append("\"title\":\"").append(escape(t.getTitle())).append("\",")
              .append("\"description\":\"").append(escape(t.getDescription())).append("\",")
              .append("\"priority\":\"").append(escape(t.getPriority())).append("\",")
              .append("\"deadline\":\"").append(escape(t.getDeadline())).append("\",")
              .append("\"status\":\"").append(escape(t.getStatus())).append("\"")
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escape(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }
}