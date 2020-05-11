# AWS SDK project for starting loadtest on AWS

### Prerequisites
The code assumes the required infra is available, so make sure that it is created using `gatling-infra` project.

### Commands

* `mvn clean compile exec:exec` compile and run

### Parameters
Params are set using env vars.  
Required:  
* `VPC_ID`: Vpc to get subnets from  
* `CLUSTER`: Name of the Fargate cluster to run task(s) on  
* `TASK_DEFINITION`: Name of the Fargate task definition to run on the cluster  
* `REPORT_BUCKET`: S3 bucket that the Docker container will write its result too.  

Optional with default values:  
* `CONTAINERS`: The number of Docker containers that will be started. Default `1`  
* `USERS`: The number of users per Docker container. Default `10`  
* `FEEDER_START`: The starting value for the feeder. Default `0`  
* `DRYRUN`: Show output, but don't run test. Default `false`  

Optional:  
* `BASEURL`: Override the baseurl used by Gatling.  
* `MAX_DURATION`: Override the max duration of the Gatling test in minutes.  
* `RAMPUP_TIME`: Override the rampup time of the Gatling test in seconds.  


#### Example command
`AWS_PROFILE=<profile> VPC_ID=vpc-bf6957d9 CLUSTER=gatling-cluster TASK_DEFINITION=gatling-runner CONTAINERS=10 USERS=2000 DRYRUN=true mvn clean compile exec:exec`
