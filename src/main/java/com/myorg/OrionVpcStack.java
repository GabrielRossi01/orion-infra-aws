package com.myorg;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.ec2.SubnetConfiguration;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.constructs.Construct;

import java.util.Arrays;

public class OrionVpcStack extends Stack {

    private final Vpc vpc;

    public OrionVpcStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public OrionVpcStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        this.vpc = Vpc.Builder.create(this, "OrionVpc")
                .vpcName("orion-dev-vpc")
                .maxAzs(2)
                .natGateways(0)
                .subnetConfiguration(Arrays.asList(
                        SubnetConfiguration.builder()
                                .name("orion-dev-public")
                                .subnetType(SubnetType.PUBLIC)
                                .cidrMask(24)
                                .build(),
                        SubnetConfiguration.builder()
                                .name("orion-dev-private")
                                .subnetType(SubnetType.PRIVATE_ISOLATED)
                                .cidrMask(24)
                                .build()
                ))
                .build();

        Tags.of(this.vpc).add("project", "orion");
        Tags.of(this.vpc).add("environment", "dev");
    }

    public Vpc getVpc() {
        return vpc;
    }
}
