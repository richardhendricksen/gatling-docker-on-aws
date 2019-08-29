#!/usr/bin/env bash
set -e

function help_text {
    cat <<EOF
    Usage: $0 [ -r|--report-bucket string ] [ -c|--containers n ] [ -u|--users n ] [ -d|--duration m ] [ -ramp|--ramp-up s ] [ -cl|--ecs-cluster ] [ -n|--name ] [-h]

        -r, -report-bucket string           (required) Name of the S3 bucket to upload/download logs from and upload the reports to. Must be in same AWS account as profile.
        -c, --containers n                  (required) Number of concurrent Docker containers.
        -u, --users n                       (required) Number of concurrent users.
        -d, --duration m                    (required) Max duration of loadtest in minutes.
        -ramp, --rampup s                   (required) Rampup time in seconds.
        -cl, --ecs-cluster string           (required) ECS Cluster to run on.
        -n, --name {NAME}}                  (required) ECR repository name.
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
        -r|--report-bucket)
            export AWS_REPORT_BUCKET="$2"
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
        -n|--name)
            export ECR_REPOSITORY_NAME="$2"
            shift; shift;
        ;;
        *)
            echo "ERROR: Unrecognised option: ${arg}"
            help_text
            exit 1
        ;;
    esac
done

if [[ -z $AWS_REPORT_BUCKET || -z $DOCKER_NR_CONTAINERS || -z $GATLING_NR_USERS || -z $GATLING_MAX_DURATION || -z $GATLING_RAMPUP_TIME || -z $AWS_ECS_CLUSTER || -z $ECR_REPOSITORY_NAME ]]
then
    echo "Missing arguments."
    help_text
    exit 1
fi

# Determine script dir
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd ${DIR}/..

# Get AWS account id
export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --output text --query Account)

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
# Start all containers
echo "Running loadtest with ${DOCKER_NR_CONTAINERS} containers with ${GATLING_NR_USERS} users each"
echo "For a total of $(($DOCKER_NR_CONTAINERS * $GATLING_NR_USERS)) concurrent users"
ecs-cli compose scale ${DOCKER_NR_CONTAINERS} --cluster ${AWS_ECS_CLUSTER} --launch-type FARGATE

# Wait until all containers are stopped
until [[ $(ecs-cli ps --cluster ${AWS_ECS_CLUSTER} --desired-status RUNNING) == "" ]]
do
    echo "Tasks are running on cluster..."
    sleep 60s
done
