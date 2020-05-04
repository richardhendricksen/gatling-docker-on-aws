package nl.codecontrol.cdk.gatling.monitoring;

import nl.codecontrol.cdk.gatling.GatlingEcsServiceProps;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SecurityGroupProps;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.IEcsLoadBalancerTarget;
import software.amazon.awscdk.services.ecs.LoadBalancerTargetOptions;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.PortMapping;
import software.amazon.awscdk.services.elasticloadbalancingv2.BaseNetworkListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.TargetType;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;

public class GatlingMonitoringFargateService extends Construct {

    public GatlingMonitoringFargateService(Construct scope, String id, Builder builder) {
        super(scope, id);

        // Security group
        final SecurityGroup securityGroup = new SecurityGroup(this, "GatlingMonitoringSecurityGroup", SecurityGroupProps.builder()
                .vpc(builder.serviceProps.getVpc())
                .description(String.format("%s security group", builder.serviceProps.getServiceName()))
                .build());
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3000), "The default port that runs the Grafana UI.");
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(2003), "The default port that runs the Graphite service.");

        // Task definition
        final FargateTaskDefinition fargateTaskDefinition = FargateTaskDefinition.Builder.create(this, "GatlingMonitoringTaskDefinition")
                .cpu(1024)
                .memoryLimitMiB(2048)
                .executionRole(builder.serviceProps.getFargateExecutionRole())
                .taskRole(builder.serviceProps.getFargateTaskRole())
                .build();

        // Containers
        final ContainerDefinitionOptions grafanaContainerDefinitionOptions = new GrafanaContainerOptions(this, "GrafanaContainerOptions", builder)
                .getContainerDefinitionOptions();
        final ContainerDefinitionOptions influxdbContainerDefinitionOptions = new InfluxContainerOptions(this, "InfluxDBContainerOptions", builder)
                .getContainerDefinitionOptions();

        fargateTaskDefinition.addContainer("grafanaContainer", grafanaContainerDefinitionOptions).addPortMappings(PortMapping.builder().containerPort(3000).build());
        fargateTaskDefinition.addContainer("influxdbContainer", influxdbContainerDefinitionOptions).addPortMappings(PortMapping.builder().containerPort(2003).build());

        // Fargate Service
        final FargateService gatlingMonitoringService = FargateService.Builder.create(this, id)
                .serviceName(builder.serviceProps.getServiceName())
                .taskDefinition(fargateTaskDefinition)
                .desiredCount(1)
                .cluster(builder.serviceProps.getEcsCluster())
                .securityGroup(securityGroup)
                .assignPublicIp(true)
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(builder.serviceProps.getVpc().getPublicSubnets())
                        .build())
                .build();

        // Network LB
        NetworkLoadBalancer gatlingMonitoringLB = NetworkLoadBalancer.Builder.create(this, "gatlingMonitoringNLB")
                .loadBalancerName("gatling-monitoring")
                .vpc(builder.serviceProps.getVpc())
                .internetFacing(true)
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(builder.serviceProps.getVpc().getPublicSubnets())
                        .build())
                .build();

        // Network Target Group
        final IEcsLoadBalancerTarget gatlingLoadBalancerTarget = gatlingMonitoringService.loadBalancerTarget(LoadBalancerTargetOptions.builder()
                .containerName("grafanaContainer")
                .containerPort(3000)
                .build());

        final IEcsLoadBalancerTarget influxdbLoadBalancerTarget = gatlingMonitoringService.loadBalancerTarget(LoadBalancerTargetOptions.builder()
                .containerName("influxdbContainer")
                .containerPort(2003)
                .build());

        final NetworkTargetGroup grafanaNetworkTargetGroup = NetworkTargetGroup.Builder.create(this, "grafanaTargetGroup")
                .port(3000)
                .vpc(builder.serviceProps.getVpc())
                .targetType(TargetType.IP)
                .targets(singletonList(gatlingLoadBalancerTarget))
                .build();

        final NetworkTargetGroup influxdbNetworkTargetGroup = NetworkTargetGroup.Builder.create(this, "influxdbTargetGroup")
                .port(2003)
                .vpc(builder.serviceProps.getVpc())
                .targetType(TargetType.IP)
                .targets(singletonList(influxdbLoadBalancerTarget))
                .build();

        // Networklisteners
        final NetworkListener gatlingListener = gatlingMonitoringLB.addListener("grafana", BaseNetworkListenerProps.builder()
                .protocol(Protocol.TCP)
                .port(80)
                .build());

        final NetworkListener influxdbListener = gatlingMonitoringLB.addListener("influxdb", BaseNetworkListenerProps.builder()
                .protocol(Protocol.TCP)
                .port(2003)
                .build());

        gatlingListener.addTargetGroups("grafana", grafanaNetworkTargetGroup);
        influxdbListener.addTargetGroups("influxdb", influxdbNetworkTargetGroup);

    }

    static class GrafanaContainerOptions extends Construct {
        private final ContainerDefinitionOptions containerDefinitionOptions;

        public GrafanaContainerOptions(Construct scope, String id, GatlingMonitoringFargateService.Builder builder) {
            super(scope, id);

            Map<String, String> environmentVariables = new HashMap<>();
            environmentVariables.put("GF_SECURITY_ADMIN_USER", "admin");
            environmentVariables.put("GF_SECURITY_ADMIN_PASSWORD", "admin");
            environmentVariables.put("GF_INSTALL_PLUGINS", "grafana-clock-panel,grafana-worldmap-panel,grafana-piechart-panel");

            DockerImageAsset grafanaAsset = DockerImageAsset.Builder.create(this, "grafanaAsset")
                    .directory("../gatling-monitoring/grafana")
                    .build();

            this.containerDefinitionOptions = ContainerDefinitionOptions.builder()
                    .image(ContainerImage.fromDockerImageAsset(grafanaAsset))
                    .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                            .logGroup(LogGroup.Builder.create(this, "grafanaFargateLogGroup")
                                    .logGroupName(String.format("/ecs/%s/%s", builder.serviceProps.getClusterNamespace(), "grafana"))
                                    .retention(RetentionDays.TWO_WEEKS)
                                    .removalPolicy(RemovalPolicy.DESTROY)
                                    .build())
                            .streamPrefix(builder.serviceProps.getServiceName())
                            .build()))
                    .environment(environmentVariables)
                    .build();
        }

        public ContainerDefinitionOptions getContainerDefinitionOptions() {
            return this.containerDefinitionOptions;
        }
    }

    static class InfluxContainerOptions extends Construct {
        private final ContainerDefinitionOptions containerDefinitionOptions;

        public InfluxContainerOptions(Construct scope, String id, GatlingMonitoringFargateService.Builder builder) {
            super(scope, id);

            Map<String, String> environmentVariables = new HashMap<>();
            environmentVariables.put("INFLUXDB_ADMIN_USER", "admin");
            environmentVariables.put("INFLUXDB_ADMIN_PASSWORD", "admin");
            environmentVariables.put("INFLUXDB_DB", "influx");
            environmentVariables.put("INFLUXDB_GRAPHITE_ENABLED", "true");

            DockerImageAsset influxdbAsset = DockerImageAsset.Builder.create(this, "influxdbAsset")
                    .directory("../gatling-monitoring/influxdb")
                    .build();

            this.containerDefinitionOptions = ContainerDefinitionOptions.builder()
                    .image(ContainerImage.fromDockerImageAsset(influxdbAsset))
                    .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                            .logGroup(LogGroup.Builder.create(this, "influxdbFargateLogGroup")
                                    .logGroupName(String.format("/ecs/%s/%s", builder.serviceProps.getClusterNamespace(), "influxdb"))
                                    .retention(RetentionDays.TWO_WEEKS)
                                    .removalPolicy(RemovalPolicy.DESTROY)
                                    .build())
                            .streamPrefix(builder.serviceProps.getServiceName())
                            .build()))
                    .environment(environmentVariables)
                    .build();
        }

        public ContainerDefinitionOptions getContainerDefinitionOptions() {
            return this.containerDefinitionOptions;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private GatlingEcsServiceProps serviceProps;

        public Builder gatlingMonitoringServiceProps(GatlingEcsServiceProps props) {
            this.serviceProps = props;
            return this;
        }

        public GatlingMonitoringFargateService build(Construct scope, String id) {
            return new GatlingMonitoringFargateService(scope, id, this);
        }
    }
}
