# Distributed Gatling testing using Docker on AWS [![CI](https://github.com/richardhendricksen/gatling-docker-on-aws/workflows/CI/badge.svg)](https://github.com/richardhendricksen/gatling-docker-on-aws/actions?query=workflow%3ACI)

This is the improved setup, read my updated article about it [here](https://medium.com/@richard.hendricksen/distributed-load-testing-with-gatling-using-docker-and-aws-part-2-5a6df57128aa).  
The old setup described in my [original article](https://medium.com/@richard.hendricksen/distributed-load-testing-with-gatling-using-docker-and-aws-d497605692db) can be found on the [v1](https://github.com/richardhendricksen/gatling-docker-on-aws/tree/v1) branch.  


## Prerequisites  

### Install  
* aws-cli  
* Docker  
* Maven >= 3.5  
* Java >= 8  
* Node >= 10.3.0  

## Projects in this repository

### aws-test-runner
AWS SDK project for running the loadtest on AWS.  

See [README.md](aws-test-runner/README.md) in project. 

### gatling-infra
Contains AWS CDK project for creating needed infra for running loadtest on AWS.  

See [README.md](gatling-infra/README.md) in project. 

### gatling-monitoring
Dockerfiles for realtime Gatling Monitoring  

See [README.md](gatling-monitoring/README.md) in project.  
[<img src="https://raw.githubusercontent.com/richardhendricksen/gatling-docker-on-aws/master/images/dashboard.png" width="600">](https://raw.githubusercontent.com/richardhendricksen/gatling-docker-on-aws/master/images/dashboard.png)

### gatling-runner
Mavenized Gatling project containing loadtest code.  
Contains Dockerfile to build image to run on AWS.  

See [README.md](gatling-runner/README.md) in project. 

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
Now run the loadtest on AWS using the `aws-test-runner` project:  
`AWS_PROFILE=<profile> VPC_ID=<id> REPORT_BUCKET=<bucket> CLUSTER=gatling-cluster TASK_DEFINITION=gatling-runner SIMULATION=nl.codecontrol.gatling.simulations.BasicSimulation CONTAINERS=10 USERS=10  mvn clean compile exec:exec`

### 5. (optionally) Destroy deployed infra
When done with loadtesting the loadtest infra can easily be destroyed, saving on costs:    
`VPC_ID=<id> REPORT_BUCKET=<bucket> cdk destroy GatlingMonitoringEcsStack --profile <profile>`  
`VPC_ID=<id> REPORT_BUCKET=<bucket> cdk destroy GatlingRunnerEcsStack --profile <profile>`  
Make sure the S3 bucket is empty before running `cdk destroy` or it will fail since CloudFormation cannot delete S3 buckets that aren't empty.

### Important
When making changes to the Gatling code in the `gatling-runner` project, don't forget to:  
 1. build your `gatling-runner` project using Maven  
 2. and re-deploy your `GatlingRunnerEcsStack` from `gatling-infra` so AWS CDK will update your Docker image containing the Gatling code  

## IAM Policy Template
Since this project uses many AWS services it can be a hassle to set all IAM permissions to get it to work if you dont want to run with admin rights.  
In the root of this project is a IAM policy template: `iam_policy_template.json`.  
This template contains al the needed permissions to run this project. Just make sure to replace `<result_bucket>` with your actual bucket name.
