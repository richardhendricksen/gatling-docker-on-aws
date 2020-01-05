# Distributed Gatling testing using Docker on AWS [![Build Status](https://travis-ci.org/richardhendricksen/gatling-docker-on-aws.svg?branch=master)](https://travis-ci.org/richardhendricksen/gatling-docker-on-aws)


Read more about it in my blog post [here](https://medium.com/@richard.hendricksen/distributed-load-testing-with-gatling-using-docker-and-aws-d497605692db).

## Prerequisites  
### Create  
* ECR repository for Gatling Docker image  
* S3 bucket for Gatling logs  
* ECS cluster for running Docker containers, see below  

### Install  
* aws-cli. Install using `pip install awscli`  
* ecs-cli. Download from [here](https://github.com/aws/amazon-ecs-cli#latest-version)  
* Docker

### Create task execution IAM role
For a quick tutorial on AWS Fargate see [here](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/ecs-cli-tutorial-fargate.html).

See Step 1 on the page (Create the task execution IAM role) for details on creating the role and attaching the task execution role policy.

Finally, attach an appropriate S3 policy to the created role for writing to your S3 bucket.

## Running  
The test consists of 3 steps:  
* Building the Docker image  
* Running the load test on AWS  
* Creating the HTML report  

Since all steps use aws-cli or ecs-cli, make sure to setup your AWS env variables:  
* `export AWS_DEFAULT_REGION=<region>`  
* `export AWS_ACCESS_KEY_ID=<id>`  
* `export AWS_SECRET_ACCESS_KEY=<key>`  

You can run the scripts provided below individually or as part of a build pipeline. See [Travis](https://github.com/richardhendricksen/gatling-docker-on-aws/blob/master/.travis.yml) 
and [Jenkins](https://github.com/richardhendricksen/gatling-docker-on-aws/blob/master/Jenkinsfile) pipelines for examples.

### Building Docker image
`sh scripts/buildDockerImage.sh  --name <IMAGE_NAME> --region <AWS_REGION>`

### Create ECS Fargate cluster
`sh scripts/createClusterOnAWS.sh --cluster-name <ECS_CLUSTER_NAME> --config-name <ECS_CONFIG_NAME> --region <AWS_REGION>`

### Running load test on AWS
`sh scripts/runLoadtestOnAWS.sh -r <S3_BUCKET> -c <NR_CONTAINERS> -u <NR_USERS_PER_CONTAINER> -d <DURATION_IN_MIN> -ramp <RAMPUP_TIME_IN_SEC> -cl <ECS_CLUSTER>`

### Stop tasks (if necessary)
`sh scripts/stopAllTasksOnCluster.sh --ecs-cluster <ECS_CLUSTER_NAME> --region <AWS_REGION>`

### Creating HTML report
Generate final HTML report:
`sh scripts/generateHTMLReport.sh --report-bucket na-gatling-results`

Optionally, you can choose to delete the simulation logs and upload the final HTML report to the S3 bucket in addition to generating the final report.
`sh scripts/generateHTMLReport.sh --report-bucket na-gatling-results --clear-logs true --upload-report true`

### Delete ECS Fargate cluster
To just delete the cluster:
`sh scripts/deleteClusterOnAWS.sh --cluster-name <ECS_CLUSTER_NAME> --region <AWS_REGION>`

Optionally, you can choose to de-register task definitions and delete log group in addition to deleting cluster
`sh scripts/deleteClusterOnAWS.sh --cluster-name gatlingCluster --region us-west-2 --task-definition-family ${PWD##*/} --log-group /ecs/gatling-runner`

## Developing

### Run Gatling tests directly without Docker
`mvn clean gatling:test`  

### Creating docker image locally
Use default time zone:
`docker build -t gatling-runner .` 

Set local time zone:
`docker build --build-arg TIME_ZONE="$(curl -s https://ipapi.co/timezone)" -t gatling-runner .`    

### Run docker image locally
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

#### Remove cluster:
`ecs-cli down --force`  
