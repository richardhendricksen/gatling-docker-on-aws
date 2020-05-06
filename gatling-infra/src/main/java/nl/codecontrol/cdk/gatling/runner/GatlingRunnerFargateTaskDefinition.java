package nl.codecontrol.cdk.gatling.runner;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.ecs.ContainerDefinitionOptions;
import software.amazon.awscdk.services.ecs.FargateTaskDefinition;
import software.amazon.awscdk.services.ecs.Ulimit;
import software.amazon.awscdk.services.ecs.UlimitName;
import software.amazon.awscdk.services.iam.Role;

public class GatlingRunnerFargateTaskDefinition extends Construct {

    public GatlingRunnerFargateTaskDefinition(Construct scope, String id, Builder builder) {
        super(scope, id);

        // Task definition
        final FargateTaskDefinition fargateTaskDefinition = FargateTaskDefinition.Builder.create(this, "GatlingRunnerTaskDefinition")
                .cpu(4096)
                .memoryLimitMiB(8192)
                .executionRole(builder.fargateExecutionRole)
                .taskRole(builder.fargateTaskRole)
                .family(builder.taskDefinitionName)
                .build();

        // Container
        final ContainerDefinitionOptions gatlingRunnerContainerDefinitionOptions = new GatlingRunnerContainerOptions(this, "GatlingRunnerContainerOptions", builder.clusterNamespace, builder.taskDefinitionName, builder.bucketName)
                .getContainerDefinitionOptions();

        final Ulimit nprocUlimit = Ulimit.builder().name(UlimitName.NPROC).hardLimit(65535).softLimit(65535).build();
        final Ulimit nofileUlimit = Ulimit.builder().name(UlimitName.NOFILE).hardLimit(65535).softLimit(65535).build();

        fargateTaskDefinition.addContainer("gatlingRunnerContainer", gatlingRunnerContainerDefinitionOptions).addUlimits(nprocUlimit, nofileUlimit);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String taskDefinitionName;
        private String clusterNamespace;
        private String bucketName;
        private Role fargateExecutionRole;
        private Role fargateTaskRole;

        public Builder taskDefinitionName(String taskDefinitionName) {
            this.taskDefinitionName = taskDefinitionName;
            return this;
        }

        public Builder clusterNamespace(String clusterNamespace) {
            this.clusterNamespace = clusterNamespace;
            return this;
        }

        public Builder bucketName(String bucketName) {
            this.bucketName = bucketName;
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

        public GatlingRunnerFargateTaskDefinition build(Construct scope, String id) {
            return new GatlingRunnerFargateTaskDefinition(scope, id, this);
        }
    }
}
