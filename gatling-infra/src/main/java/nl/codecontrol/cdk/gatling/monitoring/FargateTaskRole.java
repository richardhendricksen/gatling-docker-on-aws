package nl.codecontrol.cdk.gatling.monitoring;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.RoleProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonList;

public class FargateTaskRole extends Role {

    public FargateTaskRole(Construct scope, String id, String roleNamePrefix) {
        this(scope, id, RoleProps.builder()
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .inlinePolicies(createInlinePolicies())
                .roleName(roleNamePrefix + "-ecs-task-role")
                .build());
    }

    public FargateTaskRole(Construct scope, String id, RoleProps props) {
        super(scope, id, props);
    }

    private static Map<String, PolicyDocument> createInlinePolicies() {
        Map<String, PolicyDocument> inlinePolicies = new HashMap<>();
        inlinePolicies.put("cloudwatch-logs", PolicyDocument.Builder.create()
                .statements(singletonList(PolicyStatement.Builder.create()
                        .effect(Effect.ALLOW)
                        .actions(singletonList("logs:*"))
                        .resources(singletonList("*"))
                        .build()))
                .build());
        return inlinePolicies;
    }
}
