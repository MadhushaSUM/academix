#!/bin/bash

# dev.sh - Development helper script

# Enable BuildKit for better caching
export DOCKER_BUILDKIT=1
export COMPOSE_DOCKER_CLI_BUILD=1

# Function to build and run services
build_and_run() {
    echo "Building and starting services with optimized caching..."
    docker compose up --build -d
}

# Function to stop services
stop_services() {
    echo "Stoping services..."
    docker compose down
}

# Function to rebuild specific service
rebuild_service() {
    local service=$1
    if [ -z "$service" ]; then
        echo "Usage: rebuild_service <service_name>"
        return 1
    fi
    
    echo "Rebuilding $service..."
    docker compose build --no-cache "$service"
    docker compose up -d "$service"
}

# Function to clean up unused Docker resources
cleanup() {
    echo "Cleaning up unused Docker resources..."
    docker system prune -f
    docker volume prune -f
}

# Function to show Maven cache usage
show_cache_usage() {
    echo "Maven cache volume size:"
    docker system df -v | grep maven_cache
}

# Main script logic
case "$1" in
    "start")
        build_and_run
        ;;
    "stop")
        stop_services
        ;;
    "rebuild")
        rebuild_service "$2"
        ;;
    "cleanup")
        cleanup
        ;;
    "cache-info")
        show_cache_usage
        ;;
    *)
        echo "Usage: $0 {start|rebuild <service>|cleanup|cache-info}"
        echo "  start        - Build and start all services"
        echo "  rebuild      - Rebuild specific service"
        echo "  cleanup      - Clean up unused Docker resources"
        echo "  cache-info   - Show Maven cache usage"
        ;;
esac