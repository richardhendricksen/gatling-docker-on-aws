# Distributed Gatling testing using Docker on AWS [![Build Status](https://travis-ci.org/richardhendricksen/gatling-docker-on-aws.svg?branch=master)](https://travis-ci.org/richardhendricksen/gatling-docker-on-aws)

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
