#!/usr/bin/env bash
set -e

function help_text {
    cat <<EOF
    Usage: $0 [ -cl|--ecs-cluster AWS_ECS_CLUSTER ] [ -r|--region AWS_REGION ] [ -p|--profile AWS_PROFILE ] [-h]

        --ecs-cluster AWS_ECS_CLUSTER           (required) ECS Cluster.
        --region AWS_REGION                     (required) AWS Region.
        --profile AWS_PROFILE                   (optional) The profile to use from ~/.aws/credentials.
EOF
    exit 1
}

while [ $# -gt 0 ]; do
    arg=$1
    case $arg in
        -h|--help)
            help_text
        ;;
        -cl|--ecs-cluster)
            export AWS_ECS_CLUSTER="$2"
            shift; shift;
        ;;
        -r|--region)
            export AWS_REGION="$2"
            shift; shift;
        ;;
        -p|--profile)
            export AWS_DEFAULT_PROFILE="$2"
            shift; shift
        ;;
        *)
            echo "ERROR: Unrecognised option: ${arg}"
            help_text
            exit 1
        ;;
    esac
done

if [[ -z $AWS_ECS_CLUSTER || -z $AWS_REGION ]]
then
    echo "Missing arguments."
    help_text
    exit 1
fi

until [[ $(aws ecs list-tasks --cluster ${AWS_ECS_CLUSTER} --region ${AWS_REGION} --output text --query "taskArns") == "" ]]
do
    echo "There are still tasks running on cluster, stopping next task..."
    aws ecs stop-task --cluster ${AWS_ECS_CLUSTER} --region ${AWS_REGION} --task "$(aws ecs list-tasks --cluster ${AWS_ECS_CLUSTER} --region ${AWS_REGION} --output text --query "taskArns[0]")"
done

echo "No tasks (left) running on cluster, done..."
