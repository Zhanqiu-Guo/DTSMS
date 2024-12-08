class TaskManager {
    constructor() {
        this.socket = new WebSocket('ws://localhost:8080/ws/tasks'); // connect web
        this.setupWebSocket();
        this.setupEventListeners();
    }

    setupWebSocket() {
        this.socket.onmessage = (event) => {
            const task = JSON.parse(event.data);
            this.updateTaskUI(task);
        };
    }

    setupEventListeners() {
        document.getElementById('taskForm').addEventListener('submit', (e) => {
            e.preventDefault();
            this.createTask();
        });
    }

    async createTask() {
        const taskData = {
            name: document.getElementById('taskName').value,
            priority: document.getElementById('taskPriority').value,
            threadsNeeded: parseInt(document.getElementById('taskThreadsNeeded').value, 10),
            command: document.getElementById('taskCommand').value
        };

        try {
            const response = await fetch('/api/tasks', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.getAuthToken()}`
                },
                body: JSON.stringify(taskData)
            });

            if (response.ok) {
                const task = await response.json();
                this.updateTaskUI(task);
            } else {
                // Handle error from backend
                const errorMessage = await response.text();
                alert(`Failed to create task: ${errorMessage}`);
            }
        } catch (error) {
            console.error('Error creating task:', error);
            alert('An unexpected error occurred while creating the task.');
        }
    }

    // Delete task by ID
    async deleteTask(taskId) {
        try {
            const response = await fetch(`/api/tasks/${taskId}`, {
                method: 'DELETE',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.getAuthToken()}`
                }
            });

            if (response.ok) {
                document.getElementById(`task-${taskId}`).remove();
                console.log(`Task ${taskId} deleted successfully.`);
            } else {
                console.error('Failed to delete task:', response.statusText);
            }
        } catch (error) {
            console.error('Error deleting task:', error);
        }
    }

    // Cancel task by ID
    async cancelTask(taskId) {
        try {
            const response = await fetch(`/api/tasks/${taskId}/status?status=CANCELLED`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${this.getAuthToken()}`
                }
            });

            if (response.ok) {
                const updatedTask = await response.json();
                this.updateTaskUI(updatedTask);
                console.log(`Task ${taskId} cancelled successfully.`);
            } else {
                console.error('Failed to cancel task:', response.statusText);
            }
        } catch (error) {
            console.error('Error cancelling task:', error);
        }
    }

    updateTaskUI(task) {
        const tasksList = document.getElementById('activeTasksList');
        const taskElement = document.createElement('div');
        taskElement.id = `task-${task.id}`;
        taskElement.className = `task-item p-4 border rounded ${this.getPriorityClass(task.priority)}`;

        const actionButtonText = (task.status === 'COMPLETED' || task.status === 'CANCELLED')
            ? 'Delete'
            : 'Cancel';
        const actionButtonOnClick = (task.status === 'COMPLETED' || task.status === 'CANCELLED')
            ? `taskManager.deleteTask(${task.id})`
            : `taskManager.cancelTask(${task.id})`;

        taskElement.innerHTML = `
            <div class="flex justify-between items-center">
                <h3 class="font-bold">${task.name}</h3>
                <span class="status-badge ${this.getStatusClass(task.status)}">
                    ${task.status}
                </span>
            </div>
            <div class="task-controls mt-2">
                <button onclick="${actionButtonOnClick}" class="btn-cancel">
                    ${actionButtonText}
                </button>
            </div>
        `;

        const existingTask = document.getElementById(`task-${task.id}`);
        if (existingTask) {
            existingTask.replaceWith(taskElement);
        } else {
            tasksList.prepend(taskElement);
        }
    }

    getPriorityClass(priority) {
        const classes = {
            LOW: 'bg-gray-100',
            MEDIUM: 'bg-blue-100',
            HIGH: 'bg-yellow-100',
            CRITICAL: 'bg-red-100'
        };
        return classes[priority] || classes.LOW;
    }

    getStatusClass(status) {
        const classes = {
            PENDING: 'bg-gray-500',
            RUNNING: 'bg-green-500',
            COMPLETED: 'bg-blue-500',
            FAILED: 'bg-red-500',
            CANCELLED: 'bg-yellow-500',
        };
        return classes[status] || classes.PENDING;
    }

    getAuthToken() {
        return localStorage.getItem('authToken');
    }
}

// Initialize TaskManager
const taskManager = new TaskManager();