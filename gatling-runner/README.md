# gatling-runner

## Run
### Using Maven
`mvn clean gatling:test`

### From Jar file
```shell script
USER_ARGS=""
COMPILATION_CLASSPATH=`find -L ./target -maxdepth 1 -name "*.jar" -type f -exec printf :{} ';'`
JAVA_OPTS="-server -Xmx1G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:+HeapDumpOnOutOfMemoryError -XX:MaxInlineLevel=20 -XX:MaxTrivialSize=12 -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false ${JAVA_OPTS}"
java $JAVA_OPTS $USER_ARGS -cp $COMPILATION_CLASSPATH io.gatling.app.Gatling -s nl.codecontrol.gatling.simulations.BasicSimulation
```

### Using Docker

#### Creating docker image
`docker build -t gatling-runner .`     

#### Test docker image locally
Use docker volume to add your AWS credentials that has permission to write to the S3 bucket. You can also optionally provide the AWS profile:  
`docker run --rm -v ${HOME}/.aws/credentials:/root/.aws/credentials:ro -e SIMULATION=nl.codecontrol.gatling.simulations.BasicSimulation -e REPORT_BUCKET=<S3_BUCKET> [-e AWS_PROFILE=<AWS_PROFILE>] gatling-runner `  

### Running on AWS
See `aws-test-runner` module.
