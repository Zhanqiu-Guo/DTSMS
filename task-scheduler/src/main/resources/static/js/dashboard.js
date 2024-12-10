async function loadTasks() {
    try {
        const response = await fetch('/api/tasks', {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${taskManager.getAuthToken()}`
            }
        });

        if (response.ok) {
            const tasks = await response.json();
            const tasksList = document.getElementById('activeTasksList');
            tasksList.innerHTML = ''; // 清空任务列表

            tasks.forEach(task => {
                taskManager.updateTaskUI(task); // 调用 TaskManager 来更新 UI
            });
        } else {
            console.error("Failed to load tasks");
        }
    } catch (error) {
        console.error("Error fetching tasks:", error);
    }
}

async function loadTaskMetrics() {
    const tableBody = document.getElementById("taskMetricsTable");
    if (!tableBody) {
        console.error("taskMetricsTable element not found!");
        return;
    }

    tableBody.innerHTML = `<tr><td colspan="4" class="text-center">Loading...</td></tr>`;

    try {
        const response = await fetch("/api/process-metrics", {
            headers: {
                "Authorization": `Bearer ${taskManager.getAuthToken()}`
            }
        });

        if (!response.ok) throw new Error("Failed to fetch task metrics");
        const metrics = await response.json();

        tableBody.innerHTML = "";

        if (metrics.length === 0) {
            tableBody.innerHTML = `<tr><td colspan="4" class="text-center">No task metrics available</td></tr>`;
            return;
        }

        metrics.forEach(metric => {
            const row = document.createElement("tr");
            row.innerHTML = `
                <td class="py-2 px-4 border-b text-center">${metric.taskId}</td>
                <td class="py-2 px-4 border-b text-center">${metric.processName}</td>
                <td class="py-2 px-4 border-b text-center">${metric.cpuUsage.toFixed(2)}%</td>
                <td class="py-2 px-4 border-b text-center">${metric.memoryUsage.toFixed(2)} MB</td>
            `;
            tableBody.appendChild(row);
        });
    } catch (error) {
        console.error("Error loading task metrics:", error);
        tableBody.innerHTML = `<tr><td colspan="4" class="text-center text-red-500">Failed to load task metrics</td></tr>`;
    }
}

window.onload = function () {
    loadTasks();
    loadTaskMetrics();

    setInterval(loadTasks, 5000);
    setInterval(loadTaskMetrics, 5000);
};