class TaskManager {
    constructor() {
        this.socket = new WebSocket('ws://localhost:8080/ws/tasks');
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
            priority: document.getElementById('taskPriority').value
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
            }
        } catch (error) {
            console.error('Error creating task:', error);
        }
    }

    updateTaskUI(task) {
        const tasksList = document.getElementById('activeTasksList');
        const taskElement = document.createElement('div');
        taskElement.id = `task-${task.id}`;
        taskElement.className = `task-item p-4 border rounded ${this.getPriorityClass(task.priority)}`;
        
        taskElement.innerHTML = `
            <div class="flex justify-between items-center">
                <h3 class="font-bold">${task.name}</h3>
                <span class="status-badge ${this.getStatusClass(task.status)}">
                    ${task.status}
                </span>
            </div>
            <div class="task-controls mt-2">
                <button onclick="taskManager.toggleTaskStatus(${task.id})" class="btn-control">
                    ${task.status === 'RUNNING' ? 'Pause' : 'Resume'}
                </button>
                <button onclick="taskManager.cancelTask(${task.id})" class="btn-cancel">
                    Cancel
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
            PAUSED: 'bg-purple-500'
        };
        return classes[status] || classes.PENDING;
    }

    getAuthToken() {
        return localStorage.getItem('authToken');
    }
}

// Initialize TaskManager
const taskManager = new TaskManager();