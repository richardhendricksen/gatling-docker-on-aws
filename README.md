# Distributed Gatling testing using Docker on AWS [![CI](https://github.com/richardhendricksen/gatling-docker-on-aws/workflows/CI/badge.svg)](https://github.com/richardhendricksen/gatling-docker-on-aws/actions?query=workflow%3ACI)

This is the improved setup, for the code for the old setup described in my [original blog](https://medium.com/@richard.hendricksen/distributed-load-testing-with-gatling-using-docker-and-aws-d497605692db) post see the [v1](https://github.com/richardhendricksen/gatling-docker-on-aws/tree/v1) branch.  
Blog describing the new setup is underway.

## Prerequisites  
### Create  
* S3 bucket for Gatling logs  

### Install  
* aws-cli  
* Docker  
* Maven >= 3.5  
* Java >= 8  
* Node >= 10.3.0  

## Projects in this repository

### gatling-infra
Contains AWS CDK project for creating needed infra for running loadtest on AWS.  

See [README.md](gatling-runner/README.md) in project. 

### gatling-monitoring
Dockerfiles for realtime Gatling Monitoring  

See [README.md](gatling-monitoring/README.md) in project. 

### gatling-runner
Mavenized Gatling project containing loadtest code.  
Contains Dockerfile to build image to run on AWS.  

See [README.md](gatling-runner/README.md) in project. 


### gatling-runner-aws
AWS SDK project for running the loadtest on AWS.  

See [README.md](aws-loadtest-runner/README.md) in project. 

## How to use

### 1. Build gatling-runner project
Build the `gatling-runner` project, so it creates the required jars needed for the Docker image.  

### 2. Build gatling-infra project  
Build the `gatling-infra` project before calling `cdk deploy`. The cdk tooling will not compile the code.  

### 3. Deploy the infra
Now deploy the infra from the `gatling-infra` project:  
`VPC_ID=<id> REPORT_BUCKET=<bucket> cdk deploy GatlingMonitoringEcsStack --profile <profile>`  
`VPC_ID=<id> REPORT_BUCKET=<bucket> cdk deploy GatlingRunnerEcsStack --profile <profile>`  

### 4. Run the test
Now run the loadtest on AWS using the `gatling-runner-aws` project:  
`AWS_PROFILE=<profile> VPC_ID=vpc-bf6957d9 CLUSTER=gatling-cluster TASK_DEFINITION=gatling-runner CONTAINERS=10 USERS=10 SIMULATION=nl.codecontrol.gatling.simulations.BasicSimulation mvn clean compile exec:exec`

### Important
When making changes to the Gatling code in the `gatling-runner` project, don't forget to:  
 1. build your `gatling-runner` project using Maven  
 2. and re-deploy your `GatlingRunnerEcsStack` from `gatling-infra` so AWS CDK will update your Docker image containing the Gatling code  
