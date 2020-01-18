#!/usr/bin/env bash
set -e

function help_text {
    cat <<EOF
    Usage: $0 [ -r|--report-bucket AWS_REPORT_BUCKET ] [ -rgn|--region AWS_REGION ] [ -c|--containers DOCKER_NR_CONTAINERS ] [ -u|--users GATLING_NR_USERS ] [ -d|--duration GATLING_MAX_DURATION ] [ -ramp|--ramp-up GATLING_RAMPUP_TIME ] [ -cl|--ecs-cluster AWS_ECS_CLUSTER ] [ -n|--name IMAGE_NAME ] [ -p|--profile AWS_DEFAULT_PROFILE ] [-h]

        -r, -report-bucket AWS_REPORT_BUCKET    (required) Name of the S3 bucket to upload/download logs from and upload the reports to. Must be in same AWS account as profile.
        -rgn, --region AWS_REGION               (required) ECR and container log region
        -c, --containers DOCKER_NR_CONTAINERS   (required) Number of concurrent Docker containers.
        -u, --users GATLING_NR_USERS            (required) Number of concurrent users.
        -d, --duration GATLING_MAX_DURATION     (required) Max duration of loadtest in minutes.
        -ramp, --ramp-up GATLING_RAMPUP_TIME    (required) Ramp-up time in seconds.
        -cl, --ecs-cluster AWS_ECS_CLUSTER      (required) ECS Cluster to run on.
        -n, --name IMAGE_NAME                   (required) Docker image name.
        -p, --profile AWS_DEFAULT_PROFILE       (optional) The profile to use from ~/.aws/credentials.
EOF
    exit 1
}

ECS_PROFILE=""

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
        -rgn|--region)
            export AWS_REGION="$2"
            shift; shift;
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
        -ramp|--ramp-up)
            export GATLING_RAMPUP_TIME="$2"
            shift; shift;
        ;;
        -cl|--ecs-cluster)
            export AWS_ECS_CLUSTER="$2"
            shift; shift;
        ;;
        -n|--name)
            export IMAGE_NAME="$2"
            shift; shift;
        ;;
        -p|--profile)
            export AWS_DEFAULT_PROFILE="$2"
            ECS_PROFILE="--aws-profile $2"
            shift; shift;
        ;;
        *)
            echo "ERROR: Unrecognised option: ${arg}"
            help_text
            exit 1
        ;;
    esac
done

if [[ -z $AWS_REPORT_BUCKET || -z $AWS_REGION || -z $DOCKER_NR_CONTAINERS || -z $GATLING_NR_USERS || -z $GATLING_MAX_DURATION || -z $GATLING_RAMPUP_TIME || -z $AWS_ECS_CLUSTER || -z $IMAGE_NAME ]]
then
    echo "Missing arguments."
    help_text
    exit 1
fi

# Determine script dir
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd ${DIR}/..

# Get AWS account id
AWS_ACCOUNT_ID=$(aws sts get-caller-identity --output text --query Account)
export AWS_ACCOUNT_ID

## Sets the region environment variable in the docker-compose.yml file
echo "ECR and container log region: ${AWS_REGION}"

# Check no tasks are running on cluster
RUNNING_TASKS=$(ecs-cli ps --cluster ${AWS_ECS_CLUSTER} --desired-status RUNNING ${ECS_PROFILE})
if [[ ${RUNNING_TASKS} != "" ]]
then
    echo "There are still tasks running on cluster, load test aborted:"
    echo "$RUNNING_TASKS"
    exit 1
fi

# Remove existing logs from S3 bucket
echo "Removing existing log files from S3 bucket"
aws s3 rm s3://${AWS_REPORT_BUCKET}/logs --recursive

# Creates a task definition. Also creates a log group if one doesn't already exist.
ecs-cli compose create --create-log-groups ${ECS_PROFILE}

# Start all containers
echo "Running loadtest with ${DOCKER_NR_CONTAINERS} containers with ${GATLING_NR_USERS} users each"
echo "For a total of $(($DOCKER_NR_CONTAINERS * $GATLING_NR_USERS)) concurrent users"
ecs-cli compose scale ${DOCKER_NR_CONTAINERS} --cluster ${AWS_ECS_CLUSTER} --launch-type FARGATE ${ECS_PROFILE}

# Wait until all containers are stopped
until [[ $(ecs-cli ps --cluster ${AWS_ECS_CLUSTER} --desired-status RUNNING ${ECS_PROFILE}) == "" ]]
do
    echo "Tasks are running on cluster..."
    sleep 60s
done

echo "Finished running load test."
