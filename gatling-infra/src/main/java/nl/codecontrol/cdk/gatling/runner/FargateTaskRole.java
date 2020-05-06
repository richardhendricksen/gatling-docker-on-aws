package nl.codecontrol.cdk.gatling.runner;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.PolicyDocument;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.RoleProps;
import software.amazon.awscdk.services.iam.ServicePrincipal;

import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class FargateTaskRole extends Role {

    public FargateTaskRole(Construct scope, String id, String bucketName, String roleNamePrefix) {
        this(scope, id, RoleProps.builder()
                .assumedBy(new ServicePrincipal("ecs-tasks.amazonaws.com"))
                .inlinePolicies(createInlinePolicies(bucketName))
                .roleName(roleNamePrefix + "-ecs-task-role")
                .build());
    }

    public FargateTaskRole(Construct scope, String id, RoleProps props) {
        super(scope, id, props);
    }

    private static Map<String, PolicyDocument> createInlinePolicies(String bucketName) {
        Map<String, PolicyDocument> inlinePolicies = new HashMap<>();
        inlinePolicies.put("cloudwatch-logs", PolicyDocument.Builder.create()
                .statements(singletonList(PolicyStatement.Builder.create()
                        .effect(Effect.ALLOW)
                        .actions(singletonList("logs:*"))
                        .resources(singletonList("*"))
                        .build()))
                .build());
        inlinePolicies.put("s3", PolicyDocument.Builder.create()
                .statements(singletonList(PolicyStatement.Builder.create()
                        .effect(Effect.ALLOW)
                        .actions(singletonList("s3:*"))
                        .resources(asList("arn:aws:s3:::" + bucketName, "arn:aws:s3:::" + bucketName + "/*"))
                        .build()))
                .build());
        return inlinePolicies;
    }
}
