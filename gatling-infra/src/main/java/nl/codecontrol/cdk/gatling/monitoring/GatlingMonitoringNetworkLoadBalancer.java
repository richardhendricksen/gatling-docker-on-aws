package nl.codecontrol.cdk.gatling.monitoring;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ecs.FargateService;
import software.amazon.awscdk.services.ecs.IEcsLoadBalancerTarget;
import software.amazon.awscdk.services.ecs.LoadBalancerTargetOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.BaseNetworkListenerProps;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkListener;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkLoadBalancer;
import software.amazon.awscdk.services.elasticloadbalancingv2.NetworkTargetGroup;
import software.amazon.awscdk.services.elasticloadbalancingv2.Protocol;
import software.amazon.awscdk.services.elasticloadbalancingv2.TargetType;

import static java.util.Collections.singletonList;

public class GatlingMonitoringNetworkLoadBalancer extends Construct {

    public GatlingMonitoringNetworkLoadBalancer(Construct scope, String id, Builder builder) {
        super(scope, id);

        NetworkLoadBalancer gatlingMonitoringLB = NetworkLoadBalancer.Builder.create(this, "gatlingMonitoringNLB")
                .loadBalancerName("gatling-monitoring")
                .vpc(builder.vpc)
                .internetFacing(true)
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(builder.vpc.getPublicSubnets())
                        .build())
                .build();

        // Network Target Group
        final IEcsLoadBalancerTarget gatlingLoadBalancerTarget = builder.gatlingMonitoringService.loadBalancerTarget(LoadBalancerTargetOptions.builder()
                .containerName("grafanaContainer")
                .containerPort(3000)
                .build());

        final IEcsLoadBalancerTarget influxdbLoadBalancerTarget = builder.gatlingMonitoringService.loadBalancerTarget(LoadBalancerTargetOptions.builder()
                .containerName("influxdbContainer")
                .containerPort(2003)
                .build());

        final NetworkTargetGroup grafanaNetworkTargetGroup = NetworkTargetGroup.Builder.create(this, "grafanaTargetGroup")
                .port(3000)
                .vpc(builder.vpc)
                .targetType(TargetType.IP)
                .targets(singletonList(gatlingLoadBalancerTarget))
                .build();

        final NetworkTargetGroup influxdbNetworkTargetGroup = NetworkTargetGroup.Builder.create(this, "influxdbTargetGroup")
                .port(2003)
                .vpc(builder.vpc)
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private FargateService gatlingMonitoringService;
        private IVpc vpc;

        public Builder gatlingMonitoringService(FargateService gatlingMonitoringService) {
            this.gatlingMonitoringService = gatlingMonitoringService;
            return this;
        }

        public Builder vpc(IVpc vpc) {
            this.vpc = vpc;
            return this;
        }

        public GatlingMonitoringNetworkLoadBalancer build(Construct scope, String id) {
            return new GatlingMonitoringNetworkLoadBalancer(scope, id, this);
        }
    }
}
