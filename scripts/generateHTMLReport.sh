#!/bin/bash
set -e

function help_text {
    cat <<EOF
    Usage: $0 [ -c|--clear-logs CLEAR_LOGS ] [ -u|--upload-report UPLOAD_REPORT ] [ -r|--report-bucket REPORT_BUCKET ] [ -p|--profile AWS_PROFILE ] [-h]

        --clear-logs                            (optional) Clear the log folder in the S3 bucket after creating the report.
        --upload-report                         (optional) Upload HTML report to S3 bucket.
        --report-bucket REPORT_BUCKET           (required) name of the S3 bucket to download logs from and upload the reports to.
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
        -r|--report-bucket)
            REPORT_BUCKET="$2"
            shift; shift
        ;;
        -c|--clear-logs)
            CLEAR_LOGS=true
            shift; shift;
        ;;
        -u|--upload-report)
            UPLOAD_REPORT=true
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

if [[ -z $REPORT_BUCKET ]]
then
    echo "Report bucket required."
    help_text
    exit 1
fi

# Determine script dir
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd ${DIR}/..

FOLDER="report"

rm -f -r target/gatling/*
## Download all logs for all test gatling clients
aws s3 cp s3://${REPORT_BUCKET}/logs/ target/gatling/ --recursive --no-progress

## Consolidate reports from these clients
mvn -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn gatling:test -DgenerateReport=true

## Workaround for Gatling plugin Jenkins, mv html report to simulation folder
mkdir -p target/temp
mv target/gatling/* target/temp

mkdir -p target/gatling/${FOLDER}
mv target/temp/* target/gatling/${FOLDER}
echo "Moved final HTML report to target/gatling/${FOLDER}"

if [ "${CLEAR_LOGS}" = true ]
then
  ## Delete everything in the logs subdirectory of $REPORT_BUCKET in S3
  aws s3 rm s3://${REPORT_BUCKET}/logs --recursive
  echo "Deleted simulation logs."
fi

if [ "${UPLOAD_REPORT}" = true ]
then
  ## Upload final HTML report back to S3
  aws s3 cp target/gatling/${FOLDER}/index.html s3://${REPORT_BUCKET}/logs/index.html
  echo "Uploaded final HTML report back to S3 bucket: ${REPORT_BUCKET}/logs"
fi
