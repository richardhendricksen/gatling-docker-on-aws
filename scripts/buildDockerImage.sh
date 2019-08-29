#!/usr/bin/env bash
set -e

function help_text {
    cat <<EOF
    Usage: $0 [ -i|--aws_account_id AWS_ACCOUNT_ID ] [ -r|--region ECR_REGION] [ -p|--profile PROFILE ] [-h]

        --aws_account_id AWS_ACCOUNT_ID   (required) AWS account id
        --region REGION                   (required) Region of ECR repository
        --profile PROFILE                 (optional) The profile to use from ~/.aws/credentials.

EOF
    exit 1
}

while [ $# -gt 0 ]; do
    arg=$1
    case $arg in
        -h|--help)
            help_text
        ;;
        -i|--aws_account_id)
            export AWS_ACCOUNT_ID="$2"
            shift; shift
        ;;
        -r|--region)
            export ECR_REGION="$2"
            shift; shift
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

if [ -z "$ECR_REGION" ]
then
    echo "ECR region required."
    help_text
    exit 1
fi

if [ -z "$AWS_ACCOUNT_ID" ]
then
    echo "AWS Account id required."
    help_text
    exit 1
fi

# Determine script dir
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd ${DIR}/..

# Create docker image
docker build -t gatling-runner .

# Push docker image to AWS:
$(aws ecr get-login --no-include-email)
docker tag gatling-runner ${AWS_ACCOUNT_ID}.dkr.ecr.${ECR_REGION}.amazonaws.com/gatling-runner
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${ECR_REGION}.amazonaws.com/gatling-runner
docker logout ${AWS_ACCOUNT_ID}.dkr.ecr.${ECR_REGION}.amazonaws.com/gatling-runner
