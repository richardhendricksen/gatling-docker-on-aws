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

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static org.slf4j.LoggerFactory.getLogger;

public class AWSLoadTestRunner {

    private static final Logger LOG = getLogger(AWSLoadTestRunner.class);
    private static final int SLEEP_TIME = 60_000;

    private final Config config;
    private final EcsClient ecsClient;
    private final Ec2Client ec2Client;
    private final String vpcId;
    private final String clusterName;
    private final String taskDefinitionName;

    public static void main(String[] args) {
        AWSLoadTestRunner awsLoadTestRunner = new AWSLoadTestRunner();

        awsLoadTestRunner.runLoadTest();
    }

    public AWSLoadTestRunner() {
        this.vpcId = Objects.requireNonNull(System.getenv("VPC_ID"), "VPC_ID is required.");
        this.clusterName = Objects.requireNonNull(System.getenv("CLUSTER"), "CLUSTER_NAME is required.");
        this.taskDefinitionName = Objects.requireNonNull(System.getenv("TASK_DEFINITION"), "TASK_DEFINITION_NAME is required.");

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

        LOG.info("Starting loadtest on AWS with {} users: {} containers with {} users each", config.nrContainers * config.usersPerContainer, config.nrContainers, config.usersPerContainer);
        LOG.info("Starting from userID {}", config.startUserId);
        LOG.info("Running for {} minutes", config.duration);
        int startFromUserId = config.startUserId;
        for (int i = 0; i < config.nrContainers; i++) {
            final RunTaskRequest runTaskRequest = createRunTaskRequest(startFromUserId);

            LOG.info("Starting container {}/{}, starting from userID {}",  i+1, config.nrContainers, startFromUserId);
            if(!config.dryrun) {
                ecsClient.runTask(runTaskRequest);
            }

            startFromUserId += config.usersPerContainer;
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

    private RunTaskRequest createRunTaskRequest(int startFromUserId) {
        final NetworkConfiguration networkConfiguration = getNetworkConfiguration();

        final TaskOverride taskOverride = TaskOverride.builder()
                .containerOverrides(ContainerOverride.builder()
                        .name("gatlingRunnerContainer")
                        .command("-r", config.reportBucket, "-s", config.simulation)
                        .environment(
                                KeyValuePair.builder().name("GATLING_NR_STUDENTS").value(String.valueOf(config.usersPerContainer)).build(),
                                KeyValuePair.builder().name("GATLING_MAX_DURATION").value(config.duration).build(),
                                KeyValuePair.builder().name("GATLING_RAMPUP_TIME").value(config.rampUpTime).build(),
                                KeyValuePair.builder().name("GATLING_BASEURL").value(config.baseUrl).build(),
                                KeyValuePair.builder().name("GATLING_STUDENT_FEEDER_FROM").value(String.valueOf(startFromUserId)).build())
                        .build())
                .build();

        return RunTaskRequest.builder()
                .launchType(LaunchType.FARGATE)
                .cluster(clusterName)
                .taskDefinition(taskDefinitionName)
                .count(1)
                .networkConfiguration(networkConfiguration)
                .overrides(taskOverride)
                .build();
    }

    private AwsVpcConfiguration getAwsVpcConfiguration() {
        return AwsVpcConfiguration.builder()
                .assignPublicIp(AssignPublicIp.ENABLED)
                .subnets(getSubnets(vpcId))
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
        DescribeClustersRequest request = DescribeClustersRequest.builder().clusters(clusterName).build();
        return ecsClient.describeClusters(request).clusters().get(0);
    }

    private boolean anyTasksActive() {
        Cluster cluster = getClusterState();

        return cluster.runningTasksCount() != 0 && cluster.pendingTasksCount() != 0;
    }

    static class Config {
        final String reportBucket = getEnvVarOrDefault("REPORTBUCKET", "gatling-loadtest");
        final String baseUrl = getEnvVarOrDefault("BASEURL", "");
        final int nrContainers = parseInt(getEnvVarOrDefault("CONTAINERS", "1"));
        final int usersPerContainer = parseInt(getEnvVarOrDefault("USERS", "10"));
        final int startUserId = parseInt(getEnvVarOrDefault("STARTUSERID", "0"));
        final String duration = getEnvVarOrDefault("DURATION", "5"); // minutes
        final String rampUpTime = getEnvVarOrDefault("RAMPUPTIME", "1"); // seconds
        final String simulation = getEnvVarOrDefault("SIMULATION", ""); // seconds
        final boolean dryrun = parseBoolean(getEnvVarOrDefault("DRYRUN", "false"));


        String getEnvVarOrDefault(String var, String defaultValue) {
            if (System.getenv(var) == null) {
                return defaultValue;
            } else {
                return System.getenv(var);
            }
        }
    }
}
