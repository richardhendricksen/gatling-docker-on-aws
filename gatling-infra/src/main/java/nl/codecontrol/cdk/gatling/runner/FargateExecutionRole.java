package nl.codecontrol.cdk.gatling.runner;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.RoleProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;

import static java.util.Collections.singletonList;

public class FargateExecutionRole extends Role {

    public FargateExecutionRole(Construct scope, String id, String roleNamePrefix) {
        this(scope, id, RoleProps.builder()
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .managedPolicies(singletonList(ManagedPolicy.fromAwsManagedPolicyName("service-role/AmazonECSTaskExecutionRolePolicy")))
                .roleName(roleNamePrefix + "-ecs-execution-role")
                .build());
    }

    public FargateExecutionRole(Construct scope, String id, RoleProps props) {
        super(scope, id, props);
    }
}
