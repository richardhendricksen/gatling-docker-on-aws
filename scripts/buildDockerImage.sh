#!/usr/bin/env bash
set -e

function help_text {
    cat <<EOF
    Usage: $0 [ -n|--name IMAGE_NAME ] [ -r|--region AWS_REGION ] [ -p|--profile AWS_PROFILE ] [-h]

        --name IMAGE_NAME                   (required) Image name.
        --region AWS_REGION                 (required) AWS region.
        --profile AWS_PROFILE               (optional) The profile to use from ~/.aws/credentials.
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
            export IMAGE_NAME="$2"
            shift; shift
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

if [[ -z $IMAGE_NAME || -z $AWS_REGION ]]
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
docker build --build-arg TIME_ZONE="$(curl -s https://ipapi.co/timezone)" -t ${IMAGE_NAME} .

# Push docker image to AWS:
$(aws ecr get-login --no-include-email)
docker tag ${IMAGE_NAME} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${IMAGE_NAME}
docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${IMAGE_NAME}
docker logout ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${IMAGE_NAME}
