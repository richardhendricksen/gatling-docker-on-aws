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

class InfluxContainerOptions extends Construct {
    private final ContainerDefinitionOptions containerDefinitionOptions;

    public InfluxContainerOptions(Construct scope, String id, String clusterNamespace, String serviceName) {
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
                                .logGroupName(String.format("/ecs/%s/%s", clusterNamespace, "influxdb"))
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
