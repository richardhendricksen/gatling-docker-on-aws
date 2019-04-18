##Using ecs-cli:

### Install:
install ecs-cli: https://github.com/aws/amazon-ecs-cli  
`ecs-cli configure profile --profile-name xx --access-key xx --secret-key xxx`  

### Create cluster:

`ecs-cli configure --cluster gatlingCluster --region eu-west-1 --config-name gatlingConfiguration`  
`ecs-cli up --capability-iam --instance-type t3.large --size 1`  

Add to the generated IAM role AmazonS3FullAccess to enable S3 access  

### Running gatling task:

`ecs-cli compose scale 2`  
`ecs-cli compose ps`  
`ecs-cli logs --task-id 6a843067-5073-42c1-ae55-3902f7ae73ce`  

### Remove cluster:

`ecs-cli down`  

##Creating docker image:

`docker build -t gatling .`     

###Test local:
`docker run --rm gatling -o`  

###Push to AWS:
`export AWS_ACCESS_KEY_ID=EXAMPLE`  
`export AWS_SECRET_ACCESS_KEY=EXAMPLEKEY`  
`export AWS_DEFAULT_PROFILE=profile`  
`$(aws ecr get-login --no-include-email)`
`docker tag <image> xx.dkr.ecr.eu-west-1.amazonaws.com/gatling`  
`docker push xx.dkr.ecr.eu-west-1.amazonaws.com/gatling`  
`docker logout https://xxx.dkr.ecr.eu-west-1.amazonaws.com`
