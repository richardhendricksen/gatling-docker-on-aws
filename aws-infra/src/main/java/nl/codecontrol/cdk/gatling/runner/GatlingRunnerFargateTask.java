package nl.codecontrol.cdk.gatling.runner;

import nl.codecontrol.cdk.gatling.GatlingEcsServiceProps;
import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.RemovalPolicy;
import software.amazon.awscdk.services.ecr.assets.DockerImageAsset;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.Ulimit;
import software.amazon.awscdk.services.ecs.UlimitName;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static nl.codecontrol.cdk.gatling.runner.GatlingRunnerEcsStack.S3_BUCKET_NAME;


public class GatlingRunnerFargateTask extends Construct {

    public GatlingRunnerFargateTask(Construct scope, String id, Builder builder) {
        super(scope, id);

        // Task definition
        final FargateTaskDefinition fargateTaskDefinition = FargateTaskDefinition.Builder.create(this, "GatlingRunnerTaskDefinition")
                .cpu(4096)
                .memoryLimitMiB(8192)
                .executionRole(builder.serviceProps.getFargateExecutionRole())
                .taskRole(builder.serviceProps.getFargateTaskRole())
                .family("gatling-runner")
                .build();

        // Containers
        final ContainerDefinitionOptions gatlingRunnerContainerDefinitionOptions = new GatlingRunnerContainerOptions(this, "GatlingRunnerContainerOptions", builder)
                .getContainerDefinitionOptions();

        final Ulimit nprocUlimit = Ulimit.builder().name(UlimitName.NPROC).hardLimit(65535).softLimit(65535).build();
        final Ulimit nofileUlimit = Ulimit.builder().name(UlimitName.NOFILE).hardLimit(65535).softLimit(65535).build();

        fargateTaskDefinition.addContainer("gatlingRunnerContainer", gatlingRunnerContainerDefinitionOptions).addUlimits(nprocUlimit, nofileUlimit);
    }

    static class GatlingRunnerContainerOptions extends Construct {
        private final ContainerDefinitionOptions containerDefinitionOptions;

        public GatlingRunnerContainerOptions(Construct scope, String id, GatlingRunnerFargateTask.Builder builder) {
            super(scope, id);

            Map<String, String> environmentVariables = new HashMap<>();
            environmentVariables.put("GATLING_BASEURL", "");
            environmentVariables.put("GATLING_MAX_DURATION", "5");
            environmentVariables.put("GATLING_NR_STUDENTS", "10");
            environmentVariables.put("GATLING_RAMPUP_TIME", "30");
            environmentVariables.put("GATLING_STUDENT_FEEDER_FROM", "0");

            DockerImageAsset gatlingRunnerAsset = DockerImageAsset.Builder.create(this, "gatlingRunnerAsset")
                    .directory("../gatling-runner")
                    .build();

            this.containerDefinitionOptions = ContainerDefinitionOptions.builder()
                    .image(ContainerImage.fromDockerImageAsset(gatlingRunnerAsset))
                    .command(Arrays.asList("-r", S3_BUCKET_NAME))
                    .logging(LogDriver.awsLogs(AwsLogDriverProps.builder()
                            .logGroup(LogGroup.Builder.create(this, "gatlingRunnerFargateLogGroup")
                                    .logGroupName(String.format("/ecs/%s/%s", builder.serviceProps.getClusterNamespace(), "gatling-runner"))
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

        public Builder gatlingServiceProps(GatlingEcsServiceProps props) {
            this.serviceProps = props;
            return this;
        }

        public GatlingRunnerFargateTask build(Construct scope, String id) {
            return new GatlingRunnerFargateTask(scope, id, this);
        }
    }
}
