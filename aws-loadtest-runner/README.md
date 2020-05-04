# AWS SDK project for starting loadtest on AWS

### Prerequisites
The code assumes the required infra is available, so make sure that it is created using `aws-cdk` project.

### Commands

* `mvn clean compile exec:exec` compile and run

### Parameters
Params are set using env vars.
Vars:
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
`AWS_PROFILE=<profile> CONTAINERS=10 USERS=2000 DRYRUN=true mvn clean compile exec:exec`
