#!/bin/bash

# Function to be executed by each thread
run_thread() {
    local thread_id=$1
    echo "Thread $thread_id is running"

    # Allocate memory by creating a large array (1GB simulated in this example)
    local -a mem_array
    for ((i=0; i<1024; i++)); do  # Each element ~4KB
        mem_array[$i]="A large string occupying memory for Thread $thread_id"
    done

    sleep 10  # Simulate work while holding memory
    echo "Thread $thread_id has finished"
}

# Number of threads
num_threads=4

# Start threads
for ((i=1; i<=num_threads; i++)); do
    run_thread $i &  # Run the function in the background
done

# Wait for all threads to finish
wait

echo "All threads have completed"