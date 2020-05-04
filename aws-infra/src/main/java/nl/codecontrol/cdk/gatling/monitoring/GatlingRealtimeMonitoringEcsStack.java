package nl.codecontrol.cdk.gatling.monitoring;

import nl.codecontrol.cdk.gatling.GatlingEcsServiceProps;
import nl.codecontrol.cdk.gatling.StackBuilder;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ec2.VpcLookupOptions;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.iam.Role;

/**
 * Creates the CloudFormation for the AWS ECS Cluster and Service for the Gatling Real-Time Monitoring stack.
 * The Gatling ECS cluster uses Fargate to run stateless services.
 */
public class GatlingRealtimeMonitoringEcsStack extends Stack {
    private static final String DEFAULT_GATLING_MONITORING_SERVICE_NAME = "gatling-monitoring";

    private GatlingRealtimeMonitoringEcsStack(Builder builder) {
        super(builder.getScope(), builder.getId(), builder.getStackProps());

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
                .gatlingMonitoringServiceProps(
                        GatlingEcsServiceProps.builder()
                                .serviceName(DEFAULT_GATLING_MONITORING_SERVICE_NAME)
                                .clusterNamespace(builder.namespace)
                                .ecsCluster(ecsCluster)
                                .fargateExecutionRole(fargateExecutionRole)
                                .fargateTaskRole(fargateTaskRole)
                                .vpc(vpc)
                                .build()
                ).build(this, "GatlingMonitoringFargateService");

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

        public GatlingRealtimeMonitoringEcsStack build() {
            return new GatlingRealtimeMonitoringEcsStack(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}
