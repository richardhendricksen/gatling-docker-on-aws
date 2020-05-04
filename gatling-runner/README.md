# gatling-runner

## Run
### Maven
`mvn clean gatling:test`

### From Jar file
```shell script
USER_ARGS=""
COMPILATION_CLASSPATH=`find -L ./target -maxdepth 1 -name "*.jar" -type f -exec printf :{} ';'`
JAVA_OPTS="-server -XX:+UseThreadPriorities -XX:ThreadPriorityPolicy=42 -Xms512M -Xmx2048M -XX:+HeapDumpOnOutOfMemoryError -XX:+AggressiveOpts -XX:+OptimizeStringConcat -XX:+UseFastAccessorMethods -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false ${JAVA_OPTS}"
java $JAVA_OPTS $USER_ARGS -cp $COMPILATION_CLASSPATH io.gatling.app.Gatling -s nl.codecontrol.gatling.simulations.BasicSimulation
```

### Docker

#### Creating docker image
`docker build -t gatling-runner .`     

#### Test docker image locally
Use docker volume to add your AWS credentials that has permission to write to the S3 bucket. You can also optionally provide the AWS profile:  
`docker run --rm -v ${HOME}/.aws/credentials:/root/.aws/credentials:ro gatling-runner -r gatling-loadtest [-p <profile]`  

### Running on AWS
See `aws-loadtest-runner` module.
