package nl.codecontrol.cdk.gatling.monitoring;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SecurityGroupProps;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.ICluster;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.iam.Role;

public class GatlingMonitoringFargateService extends Construct {

    public GatlingMonitoringFargateService(Construct scope, String id, Builder builder) {
        super(scope, id);

        // Security group
        final SecurityGroup securityGroup = new SecurityGroup(this, "GatlingMonitoringSecurityGroup", SecurityGroupProps.builder()
                .vpc(builder.vpc)
                .description(String.format("%s security group", builder.serviceName))
                .build());
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3000), "The default port that runs the Grafana UI.");
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(2003), "The default port that runs the Graphite service.");

        // Task definition
        final FargateTaskDefinition fargateTaskDefinition = FargateTaskDefinition.Builder.create(this, "GatlingMonitoringTaskDefinition")
                .cpu(1024)
                .memoryLimitMiB(2048)
                .executionRole(builder.fargateExecutionRole)
                .taskRole(builder.fargateTaskRole)
                .build();

        // Containers
        final ContainerDefinitionOptions grafanaContainerDefinitionOptions = new GrafanaContainerOptions(this, "GrafanaContainerOptions", builder.clusterNamespace, builder.serviceName)
                .getContainerDefinitionOptions();
        final ContainerDefinitionOptions influxdbContainerDefinitionOptions = new InfluxContainerOptions(this, "InfluxDBContainerOptions", builder.clusterNamespace, builder.serviceName)
                .getContainerDefinitionOptions();

        fargateTaskDefinition.addContainer("grafanaContainer", grafanaContainerDefinitionOptions).addPortMappings(PortMapping.builder().containerPort(3000).build());
        fargateTaskDefinition.addContainer("influxdbContainer", influxdbContainerDefinitionOptions).addPortMappings(PortMapping.builder().containerPort(2003).build());

        // Fargate Service
        final FargateService gatlingMonitoringService = FargateService.Builder.create(this, id)
                .serviceName(builder.serviceName)
                .taskDefinition(fargateTaskDefinition)
                .desiredCount(1)
                .cluster(builder.ecsCluster)
                .securityGroup(securityGroup)
                .assignPublicIp(true)
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(builder.vpc.getPublicSubnets())
                        .build())
                .build();

        // Load balancer
        GatlingMonitoringNetworkLoadBalancer.builder()
                .gatlingMonitoringService(gatlingMonitoringService)
                .vpc(builder.vpc)
                .build(this, "gatlingMonitoringNetworkLoadBalancer");

    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String serviceName;
        private String clusterNamespace;
        private ICluster ecsCluster;
        private Role fargateExecutionRole;
        private Role fargateTaskRole;
        private IVpc vpc;

        public Builder serviceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder clusterNamespace(String clusterNamespace) {
            this.clusterNamespace = clusterNamespace;
            return this;
        }

        public Builder ecsCluster(ICluster ecsCluster) {
            this.ecsCluster = ecsCluster;
            return this;
        }

        public Builder fargateExecutionRole(Role fargateExecutionRole) {
            this.fargateExecutionRole = fargateExecutionRole;
            return this;
        }

        public Builder fargateTaskRole(Role fargateTaskRole) {
            this.fargateTaskRole = fargateTaskRole;
            return this;
        }

        public Builder vpc(IVpc vpc) {
            this.vpc = vpc;
            return this;
        }

        public GatlingMonitoringFargateService build(Construct scope, String id) {
            return new GatlingMonitoringFargateService(scope, id, this);
        }
    }
}
