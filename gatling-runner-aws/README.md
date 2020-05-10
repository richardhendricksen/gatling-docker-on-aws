# AWS SDK project for starting loadtest on AWS

### Prerequisites
The code assumes the required infra is available, so make sure that it is created using `gatling-infra` project.

### Commands

* `mvn clean compile exec:exec` compile and run

### Parameters
Params are set using env vars.
Vars for Infra:
* `VPC_ID`: Vpc to get subnets from
* `CLUSTER`: Name of the Fargate cluster to run task(s) on
* `TASK_DEFINITION`: Name of the Fargate task definition to run on the cluster
Vars for Gatling:
* `CONTAINERS`: The number of Docker containers that will be started. Default `1`
* `USERS`: The number of users per Docker container. Default `10`
* `STARTUSERID`: The starting userId that is used by Gatling. Default `0`
* `BASEURL`: The baseurl used by Gatling. Default `<none>`
* `DURATION`: The duration of the Gatling test in minutes. Default `5`
* `RAMPUPTIME`: The time Gatling takes to rampup the users in seconds. Default `1`
* `SIMULATION`: Override default Gatling simulation.
* `REPORTBUCKET`: Override the S3 bucket that the Docker container will write its result too. Default `gatling-loadtest`
* `DRYRUN`: Show output, but don't run test. Default `false`

#### Example command
`AWS_PROFILE=<profile> VPC_ID=vpc-bf6957d9 CLUSTER=gatling-cluster TASK_DEFINITION=gatling-runner CONTAINERS=10 USERS=2000 DRYRUN=true mvn clean compile exec:exec`
