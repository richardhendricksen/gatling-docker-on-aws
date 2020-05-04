package nl.codecontrol.cdk.gatling.runner;

import nl.codecontrol.cdk.gatling.GatlingEcsServiceProps;
import nl.codecontrol.cdk.gatling.StackBuilder;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;

/**
 * Creates the CloudFormation for the AWS ECS Cluster and Task definition for the Gatling Runner stack.
 * The Gatling ECS cluster uses Fargate to run stateless services.
 */
public class GatlingRunnerEcsStack extends Stack {

    static final String S3_BUCKET_NAME = "gatling-runner";

    private GatlingRunnerEcsStack(Builder builder) {
        super(builder.getScope(), builder.getId(), builder.getStackProps());

        // VPC and subnets lookup
        final IVpc vpc = Vpc.fromLookup(this, "GatlingRunnerVpc", VpcLookupOptions.builder()
                .vpcId(builder.vpcId)
                .build());

        // ECS Cluster setup
        final Cluster ecsCluster = Cluster.Builder.create(this, "GatlingRunnerCluster")
                .clusterName(builder.ecsClusterName)
                .vpc(vpc)
                .build();

        // S3 bucket for results
        Bucket.Builder.create(this, "GatlingRunnerBucket")
                .bucketName(S3_BUCKET_NAME)
                .blockPublicAccess(BlockPublicAccess.BLOCK_ALL)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        // IAM Roles needed to execute AWS ECS Fargate tasks
        Role fargateExecutionRole = new FargateExecutionRole(this, "FargateEcsExecutionRole", builder.namespace);
        Role fargateTaskRole = new FargateTaskRole(this, "FargateEcsTaskRole", S3_BUCKET_NAME, builder.namespace);

        // Create task definition
        GatlingRunnerFargateTask.builder()
                .gatlingServiceProps(
                        GatlingEcsServiceProps.builder()
                                .serviceName("gatling-runner")
                                .clusterNamespace(builder.namespace)
                                .ecsCluster(ecsCluster)
                                .fargateExecutionRole(fargateExecutionRole)
                                .fargateTaskRole(fargateTaskRole)
                                .vpc(vpc)
                                .build()
                ).build(this, "GatlingRunnerTaskDefinition");

    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends StackBuilder<Builder> {
        private String vpcId;
        private String ecsClusterName;
        private String namespace;

        public Builder vpcId(String vpcId) {
            this.vpcId = vpcId;
            return this;
        }

        public Builder ecsClusterName(String ecsClusterName) {
            this.ecsClusterName = ecsClusterName;
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public GatlingRunnerEcsStack build() {
            return new GatlingRunnerEcsStack(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
