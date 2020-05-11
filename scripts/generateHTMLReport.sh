#!/bin/bash
function help_text {
    cat <<EOF
    Usage: $0 [ -c|--clear-logs ] [ -p|--profile PROFILE ] [ -u|--upload-report ] [ -r|--report-bucket REPORT_BUCKET ] [-h]

        --clear-logs                    (optional) Clear the log folder in the S3 bucket after creating the report
        --profile PROFILE               (optional) The profile to use from ~/.aws/credentials.
        --upload-report                 (optional) Upload HTML report to S3 bucket
        --report-bucket REPORT_BUCKET   (required) name of the S3 bucket to download logs from and upload the reports to. Must be in same AWS account as profile.
                                                   It must be provided.
EOF
    exit 1
}

CLEAR_LOGS=false
UPLOAD_REPORT=false

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
        *)
            echo "ERROR: Unrecognised option: ${arg}"
            help_text
            exit 1
        ;;
    esac
done

if [ -z "$REPORT_BUCKET" ]
then
    echo "Report bucket required."
    help_text
    exit 1
fi

# Determine script dir
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
cd ${DIR}/../gatling-runner

FOLDER="bingel-loadtest"

rm -f -r target/gatling/*
## Download all simulation logs
aws s3 cp s3://${REPORT_BUCKET}/logs/ target/gatling/$FOLDER --recursive --no-progress

## Consolidate logs to html report
mvn -B -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn gatling:test -DgenerateReport=true
