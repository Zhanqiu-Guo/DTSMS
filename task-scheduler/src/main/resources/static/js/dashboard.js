let stompClient = null;

function connectWebSocket() {
    // Connect to socket
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function () {
        console.log("WebSocket connected");

        // Update task status
        stompClient.subscribe('/topic/tasks', function (message) {
            const taskUpdate = JSON.parse(message.body);
            updateTaskStatus(taskUpdate);
        });

        // Update Error
        stompClient.subscribe('/topic/tasks/error', function (message) {
            const errorUpdate = JSON.parse(message.body);
            displayTaskError(errorUpdate);
        });
    }, function (error) {
        console.error("WebSocket connection error:", error);
    });
}

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
            tasksList.innerHTML = ''; // Clear task list

            tasks.forEach(task => {
                taskManager.updateTaskUI(task); // Update UI by call TaskManager
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


// Update status in UI
function updateTaskStatus(taskUpdate) {
    const tasksList = document.getElementById('wsUpdates');
    const taskElement = document.getElementById(`task-${taskUpdate.taskId}`);

    if (taskElement) {
        taskElement.querySelector('.task-status').innerText = `Status: ${taskUpdate.status}`;
    } else {
        const newTaskElement = document.createElement('div');
        newTaskElement.id = `task-${taskUpdate.taskId}`;
        newTaskElement.className = 'p-4 border rounded bg-gray-100';
        newTaskElement.innerHTML = `
            <div>
                <span class="font-bold">Name: - ${taskUpdate.name}</span>
                <span class="task-status">Status: ${taskUpdate.status}</span>
            </div>
        `;
        tasksList.prepend(newTaskElement);
    }
}

// Show error
function displayTaskError(errorUpdate) {
    console.error(`Task ${errorUpdate.taskId} Error: ${errorUpdate.error}`);
    alert(`Task ${errorUpdate.taskId} encountered an error: ${errorUpdate.error}`);
}

window.onload = function () {
    connectWebSocket();

    loadTasks();
    loadTaskMetrics();

    setInterval(loadTasks, 5000);
    setInterval(loadTaskMetrics, 5000);
};