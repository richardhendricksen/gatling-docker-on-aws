# Distributed Gatling testing using Docker on AWS [![Build Status](https://travis-ci.org/richardhendricksen/gatling-docker-on-aws.svg?branch=master)](https://travis-ci.org/richardhendricksen/gatling-docker-on-aws)


Read more about it in my blog post [here](https://medium.com/@richard.hendricksen/distributed-load-testing-with-gatling-using-docker-and-aws-d497605692db).

## Prerequisites  
### Create  
* ECR repository for Gatling Docker image  
* S3 bucket for Gatling logs  
* ECS cluster for running Docker containers, see below  

### Install  
* aws-cli. Install using `pip install awscli`  
* ecs-cli. Download from [here](https://github.com/aws/amazon-ecs-cli#latest-version).  
* Docker  

## Running  
The test consists of 3 steps:  
* Building the Docker image  
* Running the loadtest on AWS  
* Creating the HTML report  

Since all steps use aws-cli or ecs-cli, make sure to setup your AWS env variables:  
* `export AWS_DEFAULT_REGION=<region>`  
* `export AWS_ACCESS_KEY_ID=<id>`  
* `export AWS_SECRET_ACCESS_KEY=<key>`  

### Building docker image
`./scripts/buildDockerImage.sh -r <ECR_REPOSITORY_NAME>`

### Running loadtest on AWS
`./scripts/runLoadtestOnAWS.sh -r <S3_BUCKET> -c <NR_CONTAINERS> -u <NR_USERS_PER_CONTAINER> -d <DURATION_IN_MIN> -ramp <RAMPUP_TIME_IN_SEC> -cl <ECS_CLUSTER>`

### Creating HTML report
`./consolidate_reports -r <S3_BUCKET>`

### Developing

## Run Gatling tests directly without Docker
`mvn clean gatling:test`  

## Creating docker image locally
`docker build -t gatling-runner .`     

## Run docker image locally
Use docker volume to add your AWS credentials that has permission to write to the S3 bucket. You can also optionally provide the AWS profile:  
`docker run --rm -v ${HOME}/.aws/credentials:/root/.aws/credentials:ro gatling-runner -r <bucketname> [-p <profile]`  

### ECS Cluster

#### Create ECS cluster on AWS

`ecs-cli configure --cluster gatlingCluster --region eu-west-1 --config-name gatlingConfiguration`  
`ecs-cli up --capability-iam --instance-type t3.large --size 1`  

IMPORTANT: The command will generate all the needed stuff, but make sure to add to the generated IAM role the policy for writing to your S3 bucket.

#### OR Create ECS cluster using Fargate:
`ecs-cli configure --cluster gatlingCluster --region eu-west-1 --default-launch-type FARGATE --config-name gatlingConfiguration`  
`ecs-cli up`  

#### Checking progress of tasks:
`ecs-cli compose ps`  

#### Getting logging from a task:
`ecs-cli logs --task-id <task_id> --follow`  

### Remove cluster:
`ecs-cli down --force`  
