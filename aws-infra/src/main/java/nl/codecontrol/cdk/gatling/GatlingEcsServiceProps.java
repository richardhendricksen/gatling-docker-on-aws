package nl.codecontrol.cdk.gatling;

import software.amazon.awscdk.services.ec2.IVpc;
import software.amazon.awscdk.services.ecs.ICluster;
import software.amazon.awscdk.services.iam.Role;

public interface GatlingEcsServiceProps {
    static Builder builder() {
        return new Builder();
    }

    IVpc getVpc();

    ICluster getEcsCluster();

    Role getFargateExecutionRole();

    Role getFargateTaskRole();

    String getServiceName();

    String getClusterNamespace();

    class Builder {
        private IVpc vpc;
        private ICluster ecsCluster;
        private Role fargateExecutionRole;
        private Role fargateTaskRole;
        private String serviceName;
        private String clusterNamespace;

        public Builder vpc(final IVpc vpc) {
            this.vpc = vpc;
            return this;
        }

        public Builder ecsCluster(final ICluster cluster) {
            this.ecsCluster = cluster;
            return this;
        }

        public Builder fargateExecutionRole(final Role role) {
            this.fargateExecutionRole = role;
            return this;
        }

        public Builder fargateTaskRole(final Role role) {
            this.fargateTaskRole = role;
            return this;
        }

        public Builder serviceName(final String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder clusterNamespace(final String clusterNamespace) {
            this.clusterNamespace = clusterNamespace;
            return this;
        }

        public GatlingEcsServiceProps build() {
            return new GatlingEcsServiceProps() {
                @Override
                public String getServiceName() {
                    return serviceName;
                }

                @Override
                public String getClusterNamespace() {
                    return clusterNamespace;
                }

                @Override
                public IVpc getVpc() {
                    return vpc;
                }

                @Override
                public ICluster getEcsCluster() {
                    return ecsCluster;
                }

                @Override
                public Role getFargateExecutionRole() {
                    return fargateExecutionRole;
                }

                @Override
                public Role getFargateTaskRole() {
                    return fargateTaskRole;
                }
            };
        }

    }
}
