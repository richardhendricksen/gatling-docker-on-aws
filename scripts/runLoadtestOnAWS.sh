#!/usr/bin/env bash

function help_text {
    cat <<EOF
    Usage: $0 [ -r|--report-bucket string ] [ -c|--containers n ] [ -s|--students n ] [ -d|--duration m ] [ -ramp|--ramp-up s ] [ -b|--base-url string ] [ -p|--profile PROFILE ] [-h]

        -r, -report-bucket string           (required) Name of the S3 bucket to upload/download logs from and upload the reports to. Must be in same AWS account as profile.
        -c, --containers n                  (required) Number of concurrent Docker containers
        -s, --students n                    (required) Number of concurrent students
        -d, --duration m                    (required) Max duration of loadtest in minutes
        -ramp, --rampup s                   (required) Rampup time in seconds
        -b, --base-url string               (required) Baseurl for Gatling
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
        -r|--report-bucket)
            export AWS_REPORT_BUCKET="$2"
            shift; shift
        ;;
        -c|--containers)
            DOCKER_NR_CONTAINERS="$2"
            shift; shift;
        ;;
        -s|--students)
            export GATLING_NR_STUDENTS="$2"
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
        -b|--base-url)
            export GATLING_BASEURL="$2"
            shift; shift;
        ;;
        *)
            echo "ERROR: Unrecognised option: ${arg}"
            help_text
            exit 1
        ;;
    esac
done

if [[ -z "$AWS_REPORT_BUCKET" ]]
then
    echo "Report bucket required."
    help_text
    exit 1
fi
if [[ -z "$DOCKER_NR_CONTAINERS" ]]
then
    echo "Number of concurrent Docker containers required."
    help_text
    exit 1
fi
if [[ -z "$GATLING_NR_STUDENTS" ]]
then
    echo "Nr students required."
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
if [[ -z "$GATLING_BASEURL" ]]
then
    echo "Baseurl is required."
    help_text
    exit 1
fi

# Determine script dir
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd ${DIR}/..

# Check no tasks are running on cluster
RUNNING_TASKS=$(ecs-cli ps --cluster <cluster_name> --desired-status RUNNING)
if [[ ${RUNNING_TASKS} != "" ]]
then
    echo "There are still tasks running on cluster, load test aborted:"
    echo "$RUNNING_TASKS"
    exit 1
fi

# Remove existing logs from S3 bucket
echo "Removing existing log files from S3 bucket"
aws s3 rm s3://${AWS_REPORT_BUCKET}/logs --recursive

# Build docker image
${DIR}/buildDockerImage.sh

# Run loadtest
## Start all containers
# Run loadtest
echo "Running loadtest with ${DOCKER_NR_CONTAINERS} containers with ${GATLING_NR_STUDENTS} students each"
echo "For a total of $(($DOCKER_NR_CONTAINERS * $GATLING_NR_STUDENTS)) concurrent users"
ecs-cli compose scale ${DOCKER_NR_CONTAINERS} --cluster <cluster_name> --launch-type FARGATE

# Wait until all containers are stopped
MAX_WAIT_TIME=180 # 90 min * 60 / 30s
COUNT=0
until [[ $(ecs-cli ps --cluster <cluster_name> --desired-status RUNNING) == "" ]]
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

# Generate HTML report
${DIR}/generateHTMLReport.sh -r ${AWS_REPORT_BUCKET}
