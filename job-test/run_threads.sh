#!/bin/bash

# Function to be executed by each thread
run_thread() {
    local thread_id=$1
    echo "Thread $thread_id is running"
    sleep 10
    echo "Thread $thread_id has finished"
}

max_threads=$(ulimit -u)
available_threads=$((max_threads - 2))  # Reserve extra thread

# limit thread
num_threads=4
if [[ $num_threads -gt $available_threads ]]; then
    num_threads=$available_threads
    echo "Adjusting number of threads to: $num_threads"
fi

# Start threads
for ((i=1; i<=num_threads; i++)); do
    run_thread $i &
    sleep 0.1 
done

# Wait for all threads to finish
wait
echo "All threads have completed"