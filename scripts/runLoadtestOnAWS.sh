#!/usr/bin/env bash
set -e

function help_text {
    cat <<EOF
    Usage: $0 [ -c|--containers n ] [ -u|--users n ] [ -d|--duration m ] [ -ramp|--ramp-up s ] [ -cl|--ecs-cluster ] [ -p|--profile PROFILE ] [-h]

        -c, --containers n                  (required) Number of concurrent Docker containers.
        -u, --users n                       (required) Number of users per Docker container.
        -d, --duration m                    (required) Max duration of loadtest in minutes.
        -ramp, --rampup s                   (required) Rampup time in seconds.
        -cl, --ecs-cluster string           (required) ECS Cluster to run on.
        -p, --profile PROFILE               (optional) The profile to use from ~/.aws/credentials.
EOF
    exit 1
}

while [ $# -gt 0 ]; do
    arg=$1
    case $arg in
        -h|--help)
            help_text
        ;;
        -p|--profile)
            export AWS_DEFAULT_PROFILE="$2"
            shift; shift
        ;;
        -c|--containers)
            DOCKER_NR_CONTAINERS="$2"
            shift; shift;
        ;;
        -u|--users)
            export GATLING_NR_USERS="$2"
            shift; shift;
        ;;
        -d|--duration)
            export GATLING_MAX_DURATION="$2"
            shift; shift;
        ;;
        -ramp|--rampup)
            export GATLING_RAMPUP_TIME="$2"
            shift; shift;
        ;;
        -cl|--ecs-cluster)
            export AWS_ECS_CLUSTER="$2"
            shift; shift;
        ;;
        *)
            echo "ERROR: Unrecognised option: ${arg}"
            help_text
            exit 1
        ;;
    esac
done

if [[ -z "$DOCKER_NR_CONTAINERS" ]]
then
    echo "Number of concurrent Docker containers required."
    help_text
    exit 1
fi
if [[ -z "$GATLING_NR_USERS" ]]
then
    echo "Nr users required."
    help_text
    exit 1
fi
if [[ -z "$GATLING_MAX_DURATION" ]]
then
    echo "Max duration required."
    help_text
    exit 1
fi
if [[ -z "$GATLING_RAMPUP_TIME" ]]
then
    echo "Rampup time is required."
    help_text
    exit 1
fi
if [[ -z "$AWS_ECS_CLUSTER" ]]
then
    echo "ECS cluster is required."
    help_text
    exit 1
fi

# Determine script dir
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd ${DIR}/..

# Check no tasks are running on cluster
RUNNING_TASKS=$(ecs-cli ps --cluster ${AWS_ECS_CLUSTER} --desired-status RUNNING)
if [[ ${RUNNING_TASKS} != "" ]]
then
    echo "There are still tasks running on cluster, load test aborted:"
    echo "$RUNNING_TASKS"
    exit 1
fi

# Remove existing logs from S3 bucket
echo "Removing existing log files from S3 bucket"
aws s3 rm s3://${AWS_REPORT_BUCKET}/logs --recursive

# Run loadtest
## Start all containers
# Run loadtest
echo "Running loadtest with ${DOCKER_NR_CONTAINERS} containers with ${GATLING_NR_USERS} users each"
echo "For a total of $(($DOCKER_NR_CONTAINERS * $GATLING_NR_USERS)) concurrent users"
ecs-cli compose scale ${DOCKER_NR_CONTAINERS} --cluster ${AWS_ECS_CLUSTER} --launch-type FARGATE

# Wait until all containers are stopped
MAX_WAIT_TIME=180 # 90 min * 60 / 30s
COUNT=0
until [[ $(ecs-cli ps --cluster ${AWS_ECS_CLUSTER} --desired-status RUNNING) == "" ]]
do
    echo "Tasks are running on cluster..."
    sleep 30s
    COUNT=$[COUNT + 1]
    if [[ COUNT > MAX_WAIT_TIME ]]
    then
        echo "Load tests timed out, exiting"
        exit 1
    fi
done
