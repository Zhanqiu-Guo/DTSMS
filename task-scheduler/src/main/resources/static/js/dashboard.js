class DashboardMetrics {
    constructor() {
        this.chart = null;
        this.initializeChart();
        this.startMetricsPolling();
    }

    initializeChart() {
        const ctx = document.getElementById('metricsChart').getContext('2d');
        this.chart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: [],
                datasets: [{
                    label: 'Active Tasks',
                    data: [],
                    borderColor: 'rgb(75, 192, 192)',
                    tension: 0.1
                }, {
                    label: 'CPU Usage',
                    data: [],
                    borderColor: 'rgb(255, 99, 132)',
                    tension: 0.1
                }]
            },
            options: {
                responsive: true,
                scales: {
                    y: {
                        beginAtZero: true
                    }
                }
            }
        });
    }

    async startMetricsPolling() {
        setInterval(async () => {
            try {
                const response = await fetch('/api/metrics', {
                    headers: {
                        'Authorization': `Bearer ${taskManager.getAuthToken()}`
                    }
                });
                const metrics = await response.json();
                this.updateChart(metrics);
            } catch (error) {
                console.error('Error fetching metrics:', error);
            }
        }, 5000);
    }

    updateChart(metrics) {
        const timestamp = new Date().toLocaleTimeString();

        this.chart.data.labels.push(timestamp);
        this.chart.data.datasets[0].data.push(metrics.activeTasks);
        this.chart.data.datasets[1].data.push(metrics.cpuUsage);

        if (this.chart.data.labels.length > 20) {
            this.chart.data.labels.shift();
            this.chart.data.datasets.forEach(dataset => dataset.data.shift());
        }

        this.chart.update();
    }
    updateActiveTasksTable() {
        this.activeTasksTable.innerHTML = ''; // Clear existing rows

        this.activeTasks.forEach((task, index) => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td class="border px-4 py-2">${task.name}</td>
                <td class="border px-4 py-2">${task.priority}</td>
                <td class="border px-4 py-2">${task.threadsNeeded}</td>
                <td class="border px-4 py-2">${task.command}</td>
                <td class="border px-4 py-2">
                    <button onclick="taskManager.removeTask(${index})" class="text-red-500 hover:underline">Remove</button>
                </td>
            `;
            this.activeTasksTable.appendChild(row);
        });
    }
}

// Initialize Dashboard
const dashboard = new DashboardMetrics();