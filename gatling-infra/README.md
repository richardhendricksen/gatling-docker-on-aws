# AWS CDK project for Gatling realtime monitoring and Gatling runner infra
This module contains the [AWS CDK](https://docs.aws.amazon.com/cdk/latest/guide/home.html) code for the stacks:
1. GatlingMonitoringEcsStack: contains an ECS cluster with a service and load balancer for monitoring of gatling consisting of grafana and influxdb
2. GatlingRunnerEcsStack: contains an ECS cluster with a task definition consisting of the gatling runner docker container

## AWS CDK installation
The AWS CDK command line tool (cdk) and the AWS Construct Library are developed in TypeScript and run on Node.js.
Therefore you must have Node.js version >= 10.3.0 installed. Then install the AWS CDK by running the following command:

`npm install -g aws-cdk`

Then verify your installation:

`cdk --version`

### Prerequisites
Make sure the following is in place:

- Maven 3.6.3 or higher
- Java 8 or higher
- Docker

and in general:

- [Specify AWS credentials and region](https://docs.aws.amazon.com/cdk/latest/guide/getting_started.html#getting_started_credentials)

The configuration of AWS credentials and region are very important, it's key to make the infrastructure deployment work correctly.

**Note**: the aws-mfa tool may help when MFA is enforced on your AWS account: https://github.com/broamski/aws-mfa

### Bootstrap error
When running for the first time you can get an error referring to the toolkit stack:  
`GatlingMonitoringEcsStack failed: Error: This stack uses assets, so the toolkit stack must be deployed to the environment (Run "cdk bootstrap aws://<id>/region")`  
Run the mentioned command to fix this.

## Useful commands
 * `mvn package` compile and run tests
 * `cdk ls --profile <profile-name>` list all stacks in the app
 * `cdk synth GatlingMonitoringEcsStack --profile <profile-name>` emits the synthesized CloudFormation template for the GatlingMonitoringEcsStack
 * `cdk synth GatlingRunnerEcsStack --profile <profile-name>` emits the synthesized CloudFormation template for the GatlingRunnerEcsStack
 * `cdk deploy GatlingMonitoringEcsStack --profile <profile-name>` deploy the GatlingMonitoringEcsStack to the AWS account/region as specified by the provided profile
 * `cdk deploy GatlingRunnerEcsStack --profile <profile-name>` deploy the GatlingRunnerEcsStack to the AWS account/region as specified by the provided profile
 * `cdk diff` compare deployed stack with current state
 * `cdk docs` open CDK documentation
 
 ## Example
 The stacks won't create their own VPC and S3 Bucket, so pass those using env vars:
 `VPC_ID=vpc-bf6957d9 REPORT_BUCKET=gatling-runner cdk deploy GatlingMonitoringEcsStack --profile <profile>`
