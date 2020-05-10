package nl.codecontrol.cdk.gatling;

import nl.codecontrol.cdk.gatling.monitoring.GatlingRealtimeMonitoringEcsStack;
import nl.codecontrol.cdk.gatling.runner.GatlingRunnerEcsStack;
import software.amazon.awscdk.core.App;
import software.amazon.awscdk.core.Environment;
import software.amazon.awscdk.core.StackProps;

import java.util.Objects;

/**
 * AWS CDK app that contains the stacks for Gatling Realtime Monitoring and Gatling Runner
 */
public class GatlingInfraCdkApp {

    public static void main(final String[] args) {
        App app = new App();

        final String account = Objects.requireNonNull(System.getenv("CDK_DEFAULT_ACCOUNT"), "CDK_DEFAULT_ACCOUNT is required.");
        final String region = Objects.requireNonNull(System.getenv("CDK_DEFAULT_REGION"), "CDK_DEFAULT_REGION is required.");
        final String vpcID = Objects.requireNonNull(System.getenv("VPC_ID"), "VPC_ID is required.");
        final String s3BucketName = Objects.requireNonNull(System.getenv("S3_BUCKET_NAME"), "S3_BUCKET_NAME is required.");

        StackProps stackProps = StackProps.builder()
                .env(Environment.builder()
                        .account(account)
                        .region(region)
                        .build())
                .build();

        GatlingRealtimeMonitoringEcsStack.builder()
                .namespace("gatling-monitoring")
                .ecsClusterName("gatling-monitoring-cluster")
                .vpcId(vpcID)
                .build(app, "GatlingMonitoringEcsStack", stackProps);

        GatlingRunnerEcsStack.builder()
                .bucketName(s3BucketName)
                .namespace("gatling-runner")
                .ecsClusterName("gatling-cluster")
                .vpcId(vpcID)
                .build(app, "GatlingRunnerEcsStack", stackProps);

        app.synth();
    }
}
