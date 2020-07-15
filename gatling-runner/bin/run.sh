#!/bin/sh
# ENV VARS:
# - REPORT_BUCKET: S3 bucket where to copy the simulation.log file to
# - SIMULATION: Full classpath of simulation file to run, e.g. nl.codecontrol.gatling.simulations.BasicSimulation

# Run Gatling from jar
USER_ARGS=""
COMPILATION_CLASSPATH=`find -L ./target -maxdepth 1 -name "*.jar" -type f -exec printf :{} ';'`
JAVA_OPTS="-server -Xmx1G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:+HeapDumpOnOutOfMemoryError -XX:MaxInlineLevel=20 -XX:MaxTrivialSize=12 -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false ${JAVA_OPTS}"
java $JAVA_OPTS $USER_ARGS -cp $COMPILATION_CLASSPATH io.gatling.app.Gatling -s $SIMULATION

# Upload simulation.log to S3
for _dir in results/*/
do
   aws s3 cp ${_dir}simulation.log s3://${REPORT_BUCKET}/logs/${HOSTNAME}-simulation.log
done
