#!/bin/bash

# Function to be executed by each thread
run_thread() {
    local thread_id=$1
    echo "Thread $thread_id is running"
    sleep 10  # Simulate some work
    echo "Thread $thread_id has finished"
}

# Number of threads
num_threads=20

# Start threads
for ((i=1; i<=num_threads; i++)); do
    run_thread $i &  # Run the function in the background
done

# Wait for all threads to finish
wait

echo "All threads have completed"