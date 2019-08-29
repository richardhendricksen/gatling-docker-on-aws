#!/usr/bin/env bash
set -e

function help_text {
    cat <<EOF
    Usage: $0 [ -n|--name ECR_REPOSITORY_NAME] [-h]

        --name ECR_REPOSITORY_NAME        (required) ECR repository name.
EOF
    exit 1
}

while [ $# -gt 0 ]; do
    arg=$1
    case $arg in
        -h|--help)
            help_text
        ;;
        -n|--name)
            export ECR_REPOSITORY_NAME="$2"
            shift; shift
        ;;
        *)
            echo "ERROR: Unrecognised option: ${arg}"
            help_text
            exit 1
        ;;
    esac
done

if [[ -z $ECR_REPOSITORY_NAME ]]
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

# Create docker image
docker build -t gatling-runner .

# Push docker image to AWS:
$(aws ecr get-login --no-include-email)
docker tag gatling-runner ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/${ECR_REPOSITORY_NAME}
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/${ECR_REPOSITORY_NAME}
docker logout ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/${ECR_REPOSITORY_NAME}
