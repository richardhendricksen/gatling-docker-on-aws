#!/usr/bin/env bash
function help_text {
    cat <<EOF
    Usage: $0 [ -p|--profile PROFILE ] [-h]

        --profile PROFILE               (optional) The profile to use from ~/.aws/credentials.

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
        *)
            echo "ERROR: Unrecognised option: ${arg}"
            help_text
            exit 1
        ;;
    esac
done

# Set ECR vars
export ECR_ID=12345
export ECR_REGION=eu-west-1

# Determine script dir
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd ${DIR}/..

# Create docker image
docker build -t gatling-runner .

# Push docker image to AWS:
$(aws ecr get-login --no-include-email)
docker tag gatling-runner ${ECR_ID}.dkr.ecr.${ECR_REGION}.amazonaws.com/bingel/gatling-runner
docker push ${ECR_ID}.dkr.ecr.${ECR_REGION}.amazonaws.com/bingel/gatling-runner
docker logout ${ECR_ID}.dkr.ecr.${ECR_REGION}.amazonaws.com/bingel/gatling-runner
