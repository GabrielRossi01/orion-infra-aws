package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class OrionInfraAwsApp {
    public static void main(final String[] args) {
        App app = new App();

        OrionVpcStack vpcStack = new OrionVpcStack(app, "Vpc");

        OrionClusterStack clusterStack = new OrionClusterStack(app, "Cluster", vpcStack.getVpc());
        clusterStack.addDependency(vpcStack);

        app.synth();
    }
}

