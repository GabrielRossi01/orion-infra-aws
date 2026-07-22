package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.ecs.Cluster;
import software.constructs.Construct;

public class OrionClusterStack extends Stack {

    private final Cluster cluster;

    public OrionClusterStack(final Construct scope, final String id, final Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public OrionClusterStack(final Construct scope, final String id, final StackProps props, final Vpc vpc) {
        super(scope, id, props);

        this.cluster = Cluster.Builder.create(this, "OrionCluster")
                .clusterName("orion-dev-cluster")
                .vpc(vpc)
                .build();

        Tags.of(this.cluster).add("project", "orion");
        Tags.of(this.cluster).add("environment", "dev");
    }

    public Cluster getCluster() {
        return cluster;
    }
}
