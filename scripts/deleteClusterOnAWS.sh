#!/usr/bin/env bash
set -e

function help_text {
    cat <<EOF
    Usage: $0 [ -cln|--cluster-name ECS_CLUSTER_NAME ] [ -r|--region AWS_REGION ] [ -tdf|--task-definition-family TASK_DEFINITION_FAMILY ] [ -lg|--log-group LOG_GROUP ] [ -p|--profile AWS_PROFILE] [-h]

        --cluster-name ECS_CLUSTER_NAME                       (required) ECS cluster name.
        --region AWS_REGION                                   (required) AWS region.
        --task-definition-family TASK_DEFINITION_FAMILY       (optional) task definition family for de-registering active definitions
        --log-group LOG_GROUP                                 (optional) CloudWatch log group for deleting logs
        --profile AWS_PROFILE                                 (optional) AWS profile used, default if omitted.

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
        -cln|--cluster-name)
            ECS_CLUSTER_NAME="$2"
            shift; shift
        ;;
        -r|--region)
            AWS_REGION="$2"
            shift; shift;
        ;;
        -tdf|--task-definition-family)
            TASK_DEFINITION_FAMILY="$2"
            shift; shift;
        ;;
        -lg|--log-group)
            LOG_GROUP="$2"
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

if [[ -z $ECS_CLUSTER_NAME || -z $AWS_REGION ]]
then
    echo "Missing arguments."
    help_text
    exit 1
fi

# Determine script dir
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd ${DIR}/..

## Deletes created cluster
ecs-cli down --force --cluster ${ECS_CLUSTER_NAME} --region ${AWS_REGION} ${ECS_PROFILE}
echo "Cluster and it's resources successfully deleted."

if [[ -n $TASK_DEFINITION_FAMILY ]]
then
  ## De-registers active task definitions and makes them inactive. Currently there isn't a way to delete inactive definitions.
  taskDefinitionArns=$(aws ecs list-task-definitions --family-prefix ${TASK_DEFINITION_FAMILY} --output text --query taskDefinitionArns)

  for i in $taskDefinitionArns
  do
    aws ecs deregister-task-definition --task-definition ${i##*/} --output text --query taskDefinition.[taskDefinitionArn,status]
  done

  echo "Deleted task definitions."
fi

if [[ -n $LOG_GROUP ]]
then
  ## Deletes the log group associated with the cluster
  aws logs delete-log-group --log-group-name ${LOG_GROUP}
  echo "Deleted log group."
fi
