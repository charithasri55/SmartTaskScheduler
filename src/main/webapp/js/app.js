const API_URL = "tasks";

let tasks = [];
let currentFilter = "all";

// DOM elements
const form = document.getElementById("taskForm");
const taskIdInput = document.getElementById("taskId");
const titleInput = document.getElementById("title");
const descriptionInput = document.getElementById("description");
const priorityInput = document.getElementById("priority");
const deadlineInput = document.getElementById("deadline");
const statusInput = document.getElementById("status");
const formTitle = document.getElementById("formTitle");
const saveBtn = document.getElementById("saveBtn");
const cancelBtn = document.getElementById("cancelBtn");
const messageBox = document.getElementById("message");
const taskList = document.getElementById("taskList");
const sortBy = document.getElementById("sortBy");
const filterButtons = document.querySelectorAll(".filter-btn");

// ---------- helper functions ----------

function todayString() {
    const d = new Date();
    const month = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return d.getFullYear() + "-" + month + "-" + day;
}

function escapeHtml(text) {
    const div = document.createElement("div");
    div.textContent = text == null ? "" : text;
    return div.innerHTML;
}

function capitalize(word) {
    return word.charAt(0) + word.slice(1).toLowerCase();
}

function showMessage(text, isError) {
    messageBox.textContent = text;
    messageBox.className = "message " + (isError ? "error" : "success");
    setTimeout(function () {
        messageBox.textContent = "";
    }, 3000);
}

// Sends a POST request to the servlet
async function sendAction(params) {
    const response = await fetch(API_URL, {
        method: "POST",
        body: new URLSearchParams(params)
    });
    const data = await response.json();
    if (!response.ok) {
        throw new Error(data.error || "Request failed");
    }
    return data;
}

// ---------- load and display tasks ----------

async function loadTasks() {
    try {
        const url = API_URL + "?filter=" + currentFilter + "&sort=" + sortBy.value;
        const response = await fetch(url);
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.error || "Could not load tasks");
        }
        tasks = data;
        renderTasks();
    } catch (err) {
        taskList.innerHTML = '<p class="empty">Error: ' + escapeHtml(err.message) + "</p>";
    }
}

function renderTasks() {
    if (tasks.length === 0) {
        taskList.innerHTML = '<p class="empty">No tasks found.</p>';
        return;
    }

    const today = todayString();
    let html = "";

    tasks.forEach(function (t) {
        const isCompleted = t.status === "COMPLETED";
        const isOverdue = !isCompleted && t.deadline < today;
        const isToday = t.deadline === today;
        const priorityClass = t.priority.toLowerCase();

        html += '<div class="task priority-' + priorityClass + (isCompleted ? " completed" : "") + '">';
        html += '  <div class="task-header">';
        html += '    <span class="task-title">' + escapeHtml(t.title) + "</span>";
        html += '    <span class="badge ' + priorityClass + '">' + capitalize(t.priority) + "</span>";
        html += "  </div>";

        if (t.description) {
            html += '  <p class="task-desc">' + escapeHtml(t.description) + "</p>";
        }

        html += '  <div class="task-meta">';
        html += "    <span>Deadline: <b>" + escapeHtml(t.deadline) + "</b></span>";
        html += '    <span class="badge ' + (isCompleted ? "completed" : "pending") + '">' +
                capitalize(t.status) + "</span>";
        if (isOverdue) {
            html += '    <span class="badge overdue">Overdue</span>';
        }
        if (isToday && !isCompleted) {
            html += '    <span class="badge today">Due Today</span>';
        }
        html += "  </div>";

        html += '  <div class="task-actions">';
        if (!isCompleted) {
            html += '    <button class="btn-complete" data-action="complete" data-id="' + t.id + '">Complete</button>';
        }
        html += '    <button class="btn-edit" data-action="edit" data-id="' + t.id + '">Edit</button>';
        html += '    <button class="btn-delete" data-action="delete" data-id="' + t.id + '">Delete</button>';
        html += "  </div>";
        html += "</div>";
    });

    taskList.innerHTML = html;
}

// ---------- form handling ----------

function resetForm() {
    form.reset();
    taskIdInput.value = "";
    priorityInput.value = "MEDIUM";
    statusInput.value = "PENDING";
    deadlineInput.value = todayString();
    formTitle.textContent = "Add New Task";
    saveBtn.textContent = "Add Task";
    cancelBtn.classList.add("hidden");
}

function startEdit(id) {
    const task = tasks.find(function (t) {
        return t.id === id;
    });
    if (!task) {
        return;
    }
    taskIdInput.value = task.id;
    titleInput.value = task.title;
    descriptionInput.value = task.description;
    priorityInput.value = task.priority;
    deadlineInput.value = task.deadline;
    statusInput.value = task.status;

    formTitle.textContent = "Edit Task";
    saveBtn.textContent = "Update Task";
    cancelBtn.classList.remove("hidden");
    window.scrollTo({ top: 0, behavior: "smooth" });
}

form.addEventListener("submit", async function (event) {
    event.preventDefault();

    const isEdit = taskIdInput.value !== "";
    const params = {
        action: isEdit ? "update" : "add",
        title: titleInput.value,
        description: descriptionInput.value,
        priority: priorityInput.value,
        deadline: deadlineInput.value,
        status: statusInput.value
    };
    if (isEdit) {
        params.id = taskIdInput.value;
    }

    try {
        const data = await sendAction(params);
        showMessage(data.message, false);
        resetForm();
        loadTasks();
    } catch (err) {
        showMessage(err.message, true);
    }
});

cancelBtn.addEventListener("click", resetForm);

// ---------- task list buttons (edit, delete, complete) ----------

taskList.addEventListener("click", async function (event) {
    const button = event.target.closest("button[data-action]");
    if (!button) {
        return;
    }
    const action = button.dataset.action;
    const id = parseInt(button.dataset.id, 10);

    try {
        if (action === "edit") {
            startEdit(id);
        } else if (action === "delete") {
            if (confirm("Are you sure you want to delete this task?")) {
                const data = await sendAction({ action: "delete", id: id });
                showMessage(data.message, false);
                if (taskIdInput.value === String(id)) {
                    resetForm();
                }
                loadTasks();
            }
        } else if (action === "complete") {
            const data = await sendAction({ action: "complete", id: id });
            showMessage(data.message, false);
            loadTasks();
        }
    } catch (err) {
        showMessage(err.message, true);
    }
});

// ---------- filters and sorting ----------

filterButtons.forEach(function (btn) {
    btn.addEventListener("click", function () {
        filterButtons.forEach(function (b) {
            b.classList.remove("active");
        });
        btn.classList.add("active");
        currentFilter = btn.dataset.filter;
        loadTasks();
    });
});

sortBy.addEventListener("change", loadTasks);

// ---------- start ----------
resetForm();
loadTasks();