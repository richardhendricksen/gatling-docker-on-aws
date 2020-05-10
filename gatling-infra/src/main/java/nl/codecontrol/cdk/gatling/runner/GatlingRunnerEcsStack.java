package nl.codecontrol.cdk.gatling.runner;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
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

    private GatlingRunnerEcsStack(Construct scope, String id, StackProps stackProps, Builder builder) {
        super(scope, id, stackProps);

        // VPC and subnets lookup
        final IVpc vpc = Vpc.fromLookup(this, "GatlingRunnerVpc", VpcLookupOptions.builder()
                .vpcId(builder.vpcId)
                .build());

        // ECS Cluster setup
        final Cluster ecsCluster = Cluster.Builder.create(this, "GatlingRunnerCluster")
                .clusterName(builder.ecsClusterName)
                .vpc(vpc)
                .build();

        // IAM Roles needed to execute AWS ECS Fargate tasks
        Role fargateExecutionRole = new FargateExecutionRole(this, "FargateEcsExecutionRole", builder.namespace);
        Role fargateTaskRole = new FargateTaskRole(this, "FargateEcsTaskRole", builder.bucketName, builder.namespace);

        // Create task definition
        GatlingRunnerFargateTaskDefinition.builder()
                .taskDefinitionName("gatling-runner")
                .clusterNamespace(builder.namespace)
                .bucketName(builder.bucketName)
                .fargateExecutionRole(fargateExecutionRole)
                .fargateTaskRole(fargateTaskRole)
                .build(this, "GatlingRunnerTaskDefinition");

    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String bucketName;
        private String vpcId;
        private String ecsClusterName;
        private String namespace;

        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
            return this;
        }
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

        public GatlingRunnerEcsStack build(Construct scope, String id, StackProps stackProps) {
            return new GatlingRunnerEcsStack(scope, id, stackProps, this);
        }
    }
}
