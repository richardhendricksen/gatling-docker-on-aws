package nl.codecontrol.cdk.gatling.monitoring;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.util.HashMap;
import java.util.Map;

class GrafanaContainerOptions extends Construct {
    private final ContainerDefinitionOptions containerDefinitionOptions;

    public GrafanaContainerOptions(Construct scope, String id, String clusterNamespace, String serviceName) {
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
                                .logGroupName(String.format("/ecs/%s/%s", clusterNamespace, "grafana"))
                                .retention(RetentionDays.TWO_WEEKS)
                                .removalPolicy(RemovalPolicy.DESTROY)
                                .build())
                        .streamPrefix(serviceName)
                        .build()))
                .environment(environmentVariables)
                .build();
    }

    public ContainerDefinitionOptions getContainerDefinitionOptions() {
        return this.containerDefinitionOptions;
    }
}
