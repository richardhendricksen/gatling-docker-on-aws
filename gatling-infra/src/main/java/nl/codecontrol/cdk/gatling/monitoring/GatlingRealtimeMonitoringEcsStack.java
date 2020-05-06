package nl.codecontrol.cdk.gatling.monitoring;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.core.StackProps;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.iam.Role;

/**
 * Creates the CloudFormation for the AWS ECS Cluster and Service for the Gatling Monitoring stack.
 * The Gatling ECS cluster uses Fargate to run stateless services.
 */
public class GatlingRealtimeMonitoringEcsStack extends Stack {
    private GatlingRealtimeMonitoringEcsStack(Construct scope, String id, StackProps stackProps, Builder builder) {
        super(scope, id, stackProps);

        // VPC and subnets lookup
        IVpc vpc = Vpc.fromLookup(this, "gatlingVpc", VpcLookupOptions.builder()
                .vpcId(builder.vpcId)
                .build());

        // ECS Cluster setup
        Cluster ecsCluster = Cluster.Builder.create(this, "GatlingCluster")
                .clusterName(builder.ecsClusterName)
                .vpc(vpc)
                .build();

        // IAM Roles needed to execute AWS ECS Fargate tasks
        Role fargateExecutionRole = new FargateExecutionRole(this, "FargateEcsExecutionRole", builder.namespace);
        Role fargateTaskRole = new FargateTaskRole(this, "FargateEcsTaskRole", builder.namespace);

        // Fargate service for Gatling Realtime Monitoring
        GatlingMonitoringFargateService.builder()
                .serviceName("gatling-monitoring")
                .clusterNamespace(builder.namespace)
                .ecsCluster(ecsCluster)
                .fargateExecutionRole(fargateExecutionRole)
                .fargateTaskRole(fargateTaskRole)
                .vpc(vpc)
                .build(this, "GatlingMonitoringFargateService");
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
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

        public GatlingRealtimeMonitoringEcsStack build(Construct scope, String id, StackProps stackProps) {
            return new GatlingRealtimeMonitoringEcsStack(scope, id, stackProps, this);
        }
    }
}
