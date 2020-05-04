#!/bin/sh
help_text() {
    cat <<EOF
    Usage: $0 [ -p|--profile PROFILE ] [ -r|--report-bucket REPORT_BUCKET ] [-h]

        --profile PROFILE                     (optional) The profile to use from ~/.aws/credentials.
        --report-bucket REPORT_BUCKET         (required) name of the S3 bucket to upload the reports to. Must be in same AWS account as profile.
                                              It must be provided.
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
        -r|--report-bucket)
            REPORT_BUCKET="$2"
            shift; shift
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
        echo "Report bucket required. Please make sure its empty."
        help_text
        exit 1
fi

# Run Gatling from jar
USER_ARGS=""
COMPILATION_CLASSPATH=`find -L ./target -maxdepth 1 -name "*.jar" -type f -exec printf :{} ';'`
JAVA_OPTS="-server -XX:+UseThreadPriorities -XX:ThreadPriorityPolicy=42 -Xms512M -Xmx2048M -XX:+HeapDumpOnOutOfMemoryError -XX:+AggressiveOpts -XX:+OptimizeStringConcat -XX:+UseFastAccessorMethods -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false ${JAVA_OPTS}"
java $JAVA_OPTS $USER_ARGS -cp $COMPILATION_CLASSPATH io.gatling.app.Gatling -s nl.codecontrol.gatling.simulations.BasicSimulation

# Upload reports
for _dir in results/*/
do
   aws s3 cp ${_dir}simulation.log s3://${REPORT_BUCKET}/logs/$HOSTNAME-simulation.log
done
