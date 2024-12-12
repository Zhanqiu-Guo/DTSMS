# Task Scheduler

Built with Spring Boot and JDBC to support a multi-threaded task scheduling system that provides reliable task execution. User can use the monitoring and management feature through the front-end (HTML). This system is designed for ARM CPU based microcontrollers such as the Raspberry Pi.
![Main Console](/pictures/main-consol.png)

## Support Features

- **Task execution**: Supports Python, Bash and other script/Unix command execution.
- **Scheduling**: Schedule tasks based on priority, time, and number of threads through PriorityBlockingQueue.
- **Record & Fault Tolerance**:
- PostgreSQL accesses user task records
- The front end reads the database to update the task status in real time
- Use Java socket to notify users of task failures
![Socket](/pictures/Fail.png)
- The database saves task execution history
- **Monitoring and metrics**:
- Real-time task execution monitoring: including CPU Usage and Memory Usage
![SMC](/pictures/SMC.png)
- **Security & Permissions**:
- Configure authorizeHttpRequests to restrict users from accessing irrelevant pages
- The front end checks user input
- **Integration**:
- RESTful API

## How to Start

### Prerequisites

- JDK 17 or later
- Maven
- Gradle
- PostgreSQL

### Setup

1. Clone this repository;

2. Create DB:
```bash
psql
CREATE DATABASE taskscheduler;
ALTER USER postgres WITH PASSWORD 'password';
```
you can test your setting using:
```bash
psql -U postgres -h localhost -d taskscheduler
```

3. Start System
```bash
cd task-scheduler
mvn clean install
mvn spring-boot:run
```
You will be able to schdule a task by access your localhost with 8080 port: [localhost:](http://localhost:8080/)

### Creating Your First Task
Type task name, priority, thread needed and command on webpage.
![Update](/pictures/update.png)

### Sample Tests
We prepared a python script and a bash script to test it. You can find them in job-test/.
```bash
cd job-test/
realpath print.py
```
Please copy the path and paste it into the command textbox, along with the following: python3 /path/to/print.py. You are welcome to do the same for run_threads.sh. Please note that 5 threads must be selected for this bash to run.


## Architecture

The system consists of three main components:

1. **Core Service (scheduler-core)**
   - Task management
   - Scheduling logic
   - API endpoints

2. **Executor Service (scheduler-executor)**
   - Task execution
   - Result displaying

3. **Monitor Service (scheduler-monitor)**
   - Execution monitoring
   - Metrics collection
   - Health checking(CPU & Mem)

## Monitoring

Access monitoring interfaces:
- Main Console: http://localhost:8080/
