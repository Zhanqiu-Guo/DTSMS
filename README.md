# Distributed Task Scheduler

A robust, distributed task scheduling system built with Spring Boot that provides reliable task execution, monitoring, and management capabilities.

## 🌟 Features

- **Distributed Task Execution**: Scale your task processing across multiple nodes
- **Flexible Scheduling**: Support for cron expressions and fixed-rate scheduling
- **Fault Tolerance**: 
  - Automatic retry mechanism with exponential backoff
  - Failed task handling
  - Task execution history
- **Monitoring & Metrics**: 
  - Real-time task execution monitoring
  - Prometheus metrics integration
  - Grafana dashboards
- **Easy Integration**: 
  - RESTful API
  - Event-driven architecture using Kafka
  - Extensible task handler framework

## 🚀 Quick Start

### Prerequisites

- JDK 17 or later
- Docker and Docker Compose
- Gradle

### Setup

1. Clone the repository:
```bash
git clone https://github.com/yourusername/distributed-scheduler.git
cd distributed-scheduler
```

2. Start the infrastructure services:
```bash
docker-compose up -d
```

3. Build the project:
```bash
./gradlew clean build
```

4. Start the services:
```bash
# In separate terminals
./gradlew scheduler-core:bootRun
./gradlew scheduler-executor:bootRun
./gradlew scheduler-monitor:bootRun
```

### Creating Your First Task

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "name": "sample-task",
    "cronExpression": "*/5 * * * * *",
    "handlerClass": "com.example.SampleHandler",
    "parameters": "{\"key\":\"value\"}",
    "maxRetries": 3
  }'
```

## 🏗️ Architecture

The system consists of three main components:

1. **Core Service (scheduler-core)**
   - Task management
   - Scheduling logic
   - API endpoints

2. **Executor Service (scheduler-executor)**
   - Task execution
   - Retry handling
   - Result processing

3. **Monitor Service (scheduler-monitor)**
   - Execution monitoring
   - Metrics collection
   - Health checking

## 📊 Monitoring

Access monitoring interfaces:

- Grafana: http://localhost:3000 (admin/admin)
- Prometheus: http://localhost:9090
- H2 Console: http://localhost:8080/h2-console

## 🔧 Configuration

### Core Service (application.yml)
```yaml
server:
  port: 8080

spring:
  application:
    name: scheduler-core
  datasource:
    url: jdbc:h2:mem:schedulerdb
    username: sa
    password: password
```

### Example Task Configuration
```json
{
  "name": "data-processing-task",
  "cronExpression": "0 */15 * * * *",  // Every 15 minutes
  "handlerClass": "com.example.DataProcessor",
  "parameters": {
    "sourceUrl": "http://example.com/data",
    "batchSize": 100
  },
  "maxRetries": 3
}
```

## 📚 API Documentation

### Task Management

#### Create Task
```http
POST /api/tasks
```

#### Get Task Status
```http
GET /api/tasks/{taskId}
```

#### Update Task Status
```http
PUT /api/tasks/{taskId}/status
```

#### List Tasks
```http
GET /api/tasks?page=0&size=20
```

### Monitoring

#### Get Task Executions
```http
GET /api/monitor/tasks/{taskId}/executions
```

#### Get System Metrics
```http
GET /api/monitor/metrics
```

## 🔌 Integration

### Implementing a Custom Task Handler

```java
public class CustomTaskHandler implements TaskHandler {
    @Override
    public TaskResult execute(TaskContext context) {
        // Implementation
        return TaskResult.success();
    }
}
```

### Event Handling

```java
@KafkaListener(topics = "task-events")
public void handleTaskEvent(TaskEvent event) {
    // Handle task events
}
```

## 📈 Scaling

The system can be scaled horizontally by:

1. Adding more executor nodes
2. Configuring Kafka partitions
3. Setting up database clustering

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Spring Boot and Spring Cloud teams
- Apache Kafka team
- Grafana and Prometheus teams

## 📞 Contact

Your Name - [@yourusername](https://twitter.com/yourusername)

Project Link: [https://github.com/yourusername/distributed-scheduler](https://github.com/yourusername/distributed-scheduler)