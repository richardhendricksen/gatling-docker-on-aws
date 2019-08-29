# Distributed Gatling testing using Docker on AWS [![Build Status](https://travis-ci.org/richardhendricksen/gatling-docker-on-aws.svg?branch=master)](https://travis-ci.org/richardhendricksen/gatling-docker-on-aws)


Read more about it in my blog post [here](https://medium.com/@richard.hendricksen/distributed-load-testing-with-gatling-using-docker-and-aws-d497605692db).

## Running locally
Run test using `mvn clean gatling:test`  

## Creating docker image:
`docker build -t gatling-runner .`     

### Test docker image locally:
Use docker volume to add your AWS credentials that has permission to write to the S3 bucket. You can also optionally provide the AWS profile:  
`docker run --rm -v ${HOME}/.aws/credentials:/root/.aws/credentials:ro gatling-runner -r <bucketname> [-p <profile]`  

### Push docker image to Amazon ECR:
First login:  
`$(aws ecr get-login --no-include-email)`  
Check repositories available:  
`aws ecr describe-repositories`  
Tag and push local image to ECR:  
`docker tag gatling-runner <id>.dkr.ecr.eu-west-1.amazonaws.com/gatling-runner`  
`docker push <id>.dkr.ecr.eu-west-1.amazonaws.com/gatling-runner`  
Logout from Amazon ECR:  
`docker logout https://<id>.dkr.ecr.eu-west-1.amazonaws.com`

## Running on AWS

### Setting up ECS

### Install ecs-cli tools:
Download from [here](https://github.com/aws/amazon-ecs-cli#latest-version).  

Configure your profile:  
`ecs-cli configure profile --profile-name default --access-key $AWS_ACCESS_KEY_ID --secret-key $AWS_SECRET_ACCESS_KEY`  
Check [here](https://docs.aws.amazon.com/AmazonECS/latest/developerguide/cmd-ecs-cli-configure-profile.html) for more info.  

### Create cluster using EC2:

`ecs-cli configure --cluster gatlingCluster --region eu-west-1 --config-name gatlingConfiguration`  
`ecs-cli up --capability-iam --instance-type t3.large --size 1`  

IMPORTANT: The command will generate all the needed stuff, but make sure to add to the generated IAM role the policy for writing to your S3 bucket.

### OR Create cluster using Fargate:
`ecs-cli configure --cluster gatlingCluster --region eu-west-1 --default-launch-type FARGATE --config-name gatlingConfiguration`  
`ecs-cli up`  

Create `ecs-params.yml` file:  
```yaml
version: 1
task_definition:
  task_role_arn: <role with S3 rights>
  task_execution_role: ecsTaskExecutionRole
  ecs_network_mode: awsvpc
  task_size:
    mem_limit: 2GB
    cpu_limit: 1024
run_params:
  network_configuration:
    awsvpc_configuration:
      subnets:
        - "<subnet1 from ecs-cli up>"
        - "<subnet2 from ecs-cli up>"
      security_groups:
        - "<a security group>"
      assign_public_ip: ENABLED

```

### Running docker container as task on the cluster:
`ecs-cli compose up`  

### To scale the load test, use this:  
`ecs-cli compose scale 2`  

### Checking progress of tasks:
`ecs-cli compose ps`  

### Getting logging from a task:
`ecs-cli logs --task-id <task_id> --follow`  

### Remove cluster:
`ecs-cli down --force`  

## Creating HTML report
`./consolidate_reports -r <bucket_name> [-p <aws_profile>]`
