#!/usr/bin/env bash
set -e

function help_text {
    cat <<EOF
    Usage: $0 [ -cln|--cluster-name ECS_CLUSTER_NAME ] [ -con|--config-name ECS_CONFIG_NAME ] [ -r|--region AWS_REGION ] [ -p|--profile AWS_PROFILE] [-h]

        --cluster-name ECS_CLUSTER_NAME     (required) ECS cluster name.
        --config-name ECS_CONFIG_NAME       (required) ECS config name.
        --region AWS_REGION                 (required) AWS region.
        --profile AWS_PROFILE               (optional) AWS profile used, default if omitted.

    Note: envsubst utility is not availabe on MacOS by default. You can install it via Homebrew. To install:

        1. brew install gettext
        2. brew link --force gettext
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
        -con|--config-name)
            ECS_CONFIG_NAME="$2"
            shift; shift
        ;;
        -r|--region)
            AWS_REGION="$2"
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

if [[ -z $ECS_CLUSTER_NAME || -z $ECS_CONFIG_NAME || -z $AWS_REGION ]]
then
    echo "Missing arguments."
    help_text
    exit 1
fi

# Determine script dir
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd ${DIR}/..

# Configure ECS Fargate cluster
ecs-cli configure --cluster ${ECS_CLUSTER_NAME} --region ${AWS_REGION} --default-launch-type FARGATE --config-name ${ECS_CONFIG_NAME}

# Start cluster
ecs-cli up --cluster-config ${ECS_CONFIG_NAME} ${ECS_PROFILE}

echo "Saving VPC_ID, SubnetIds and SecurityGroup as environment variables"

VPC_ID=$(aws ec2 describe-vpcs --filters "Name=tag:aws:cloudformation:stack-name,Values=amazon-ecs-cli-setup-${ECS_CLUSTER_NAME}" --output text --query "Vpcs[0].VpcId")

SubnetIds=$(aws ec2 describe-subnets --filters "Name=vpc-id,Values=${VPC_ID}" --output text --query "Subnets[*].SubnetId")
SubnetArray=(${SubnetIds//' '/})
export subnet_1=${SubnetArray[0]}
export subnet_2=${SubnetArray[1]}

security_group=$(aws ec2 describe-security-groups --filters "Name=vpc-id,Values=${VPC_ID}" --output text --query "SecurityGroups[0].GroupId")
export security_group

# create ecs-params.yml
touch ecs-params.yml

# Templates our ecs-params.yml file with our current values
envsubst < ecs-params.yml.template >ecs-params.yml

echo "Printing ecs-params.yml file"
cat ecs-params.yml

echo "Cluster creation script execution successful."
