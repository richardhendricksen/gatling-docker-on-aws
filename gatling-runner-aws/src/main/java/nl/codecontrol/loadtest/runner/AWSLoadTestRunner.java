package nl.codecontrol.loadtest.runner;

import org.slf4j.Logger;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.Filter;
import software.amazon.awssdk.services.ec2.model.Subnet;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.AssignPublicIp;
import software.amazon.awssdk.services.ecs.model.AwsVpcConfiguration;
import software.amazon.awssdk.services.ecs.model.Cluster;
import software.amazon.awssdk.services.ecs.model.ContainerOverride;
import software.amazon.awssdk.services.ecs.model.DescribeClustersRequest;
import software.amazon.awssdk.services.ecs.model.KeyValuePair;
import software.amazon.awssdk.services.ecs.model.LaunchType;
import software.amazon.awssdk.services.ecs.model.NetworkConfiguration;
import software.amazon.awssdk.services.ecs.model.RunTaskRequest;
import software.amazon.awssdk.services.ecs.model.TaskOverride;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.System.getenv;
import static org.slf4j.LoggerFactory.getLogger;

public class AWSLoadTestRunner {

    private static final Logger LOG = getLogger(AWSLoadTestRunner.class);
    private static final int SLEEP_TIME = 60_000;

    private final Config config;
    private final EcsClient ecsClient;
    private final Ec2Client ec2Client;

    public static void main(String[] args) {
        AWSLoadTestRunner awsLoadTestRunner = new AWSLoadTestRunner();

        awsLoadTestRunner.runLoadTest();
    }

    public AWSLoadTestRunner() {
        this.config = new Config();
        this.ecsClient = EcsClient.builder().build();
        this.ec2Client = Ec2Client.builder().build();
    }

    private void runLoadTest() {
        if(config.dryrun)
            LOG.warn("Running in dryrun mode, only output is shown, no tests are started");

        if (anyTasksActive()) {
            throw new IllegalStateException("There are already tasks active on the cluster!");
        }

        LOG.info("Starting loadtest on AWS with {} users: {} containers with {} users each", config.nrContainers * parseInt(config.usersPerContainer), config.nrContainers, config.usersPerContainer);
        LOG.info("Starting from userID {}", config.feederStart);
        LOG.info("Running for {} minutes", config.gatlingOverrideMaxDuration);
        int currentFeeder = config.feederStart;
        for (int i = 0; i < config.nrContainers; i++) {
            final RunTaskRequest runTaskRequest = createRunTaskRequest(currentFeeder);

            LOG.info("Starting container {}/{}, starting from userID {}",  i+1, config.nrContainers, currentFeeder);
            if(!config.dryrun) {
                ecsClient.runTask(runTaskRequest);
            }

            currentFeeder += parseInt(config.usersPerContainer);
        }

        LOG.info("Waiting until all tasks on cluster are completed...");
        while (true) {
            try {
                Cluster cluster = getClusterState();
                if (cluster.runningTasksCount() == 0 && cluster.pendingTasksCount() == 0) {
                    break;
                }
                LOG.info("Status: {} pending tasks and {} running tasks", cluster.pendingTasksCount(), cluster.runningTasksCount());

                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private RunTaskRequest createRunTaskRequest(int currentFeeder) {
        final NetworkConfiguration networkConfiguration = getNetworkConfiguration();

        List<KeyValuePair> environmentVariables = new ArrayList<>();
        environmentVariables.add(KeyValuePair.builder().name("REPORT_BUCKET").value(config.gatlingReportBucket).build());
        environmentVariables.add(KeyValuePair.builder().name("GATLING_USERS").value(String.valueOf(config.usersPerContainer)).build());
        environmentVariables.add(KeyValuePair.builder().name("GATLING_FEEDER_START").value(String.valueOf(currentFeeder)).build());
        // optional, don't set if null
        if(config.gatlingOverrideMaxDuration != null) {
            environmentVariables.add(KeyValuePair.builder().name("GATLING_MAX_DURATION").value(config.gatlingOverrideMaxDuration).build());
        }
        if(config.gatlingOverrideRampUpTime != null) {
            environmentVariables.add(KeyValuePair.builder().name("GATLING_RAMPUP_TIME").value(config.gatlingOverrideMaxDuration).build());
        }
        if(config.gatlingOverrideBaseUrl != null) {
            environmentVariables.add(KeyValuePair.builder().name("GATLING_BASEURL").value(config.gatlingOverrideBaseUrl).build());
        }

        final TaskOverride taskOverride = TaskOverride.builder()
                .containerOverrides(ContainerOverride.builder()
                        .name("gatlingRunnerContainer")
                        .environment(environmentVariables)
                        .build())
                .build();

        return RunTaskRequest.builder()
                .launchType(LaunchType.FARGATE)
                .cluster(config.clusterName)
                .taskDefinition(config.taskDefinitionName)
                .count(1)
                .networkConfiguration(networkConfiguration)
                .overrides(taskOverride)
                .build();
    }

    private AwsVpcConfiguration getAwsVpcConfiguration() {
        return AwsVpcConfiguration.builder()
                .assignPublicIp(AssignPublicIp.ENABLED)
                .subnets(getSubnets(config.vpcId))
                .build();
    }

    private NetworkConfiguration getNetworkConfiguration() {
        return NetworkConfiguration.builder()
                .awsvpcConfiguration(getAwsVpcConfiguration())
                .build();
    }

    private List<String> getSubnets(String vpcId) {
        final DescribeSubnetsRequest describeSubnetsRequest = DescribeSubnetsRequest.builder()
                .filters(Filter.builder().name("vpc-id").values(vpcId).build())
                .build();

        final DescribeSubnetsResponse describeSubnetsResponse = ec2Client.describeSubnets(describeSubnetsRequest);

        return describeSubnetsResponse.subnets().stream().map(Subnet::subnetId).collect(Collectors.toList());
    }

    private Cluster getClusterState() {
        DescribeClustersRequest request = DescribeClustersRequest.builder().clusters(config.clusterName).build();
        return ecsClient.describeClusters(request).clusters().get(0);
    }

    private boolean anyTasksActive() {
        Cluster cluster = getClusterState();

        return cluster.runningTasksCount() != 0 && cluster.pendingTasksCount() != 0;
    }

    static class Config {
        // Required params
        final String vpcId = Objects.requireNonNull(getenv("VPC_ID"), "VPC_ID is required.");
        final String clusterName = Objects.requireNonNull(getenv("CLUSTER"), "CLUSTER_NAME is required.");
        final String taskDefinitionName = Objects.requireNonNull(getenv("TASK_DEFINITION"), "TASK_DEFINITION_NAME is required.");
        final String gatlingReportBucket = Objects.requireNonNull(System.getenv("REPORT_BUCKET"), "REPORT_BUCKET is required.");

        //Optional with defaults
        final int nrContainers = parseInt(getEnvVarOrDefault("CONTAINERS", "1"));
        final int feederStart = parseInt(getEnvVarOrDefault("FEEDER_START", "0"));
        final boolean dryrun = parseBoolean(getEnvVarOrDefault("DRYRUN", "false"));
        final String usersPerContainer = getEnvVarOrDefault("USERS", "10");

        //Optional
        final String gatlingOverrideBaseUrl = getenv("BASEURL");
        final String gatlingOverrideMaxDuration = getenv("MAX_DURATION"); // minutes
        final String gatlingOverrideRampUpTime = getenv("RAMPUP_TIME"); // seconds


        String getEnvVarOrDefault(String var, String defaultValue) {
            if (getenv(var) == null) {
                return defaultValue;
            } else {
                return getenv(var);
            }
        }
    }
}
