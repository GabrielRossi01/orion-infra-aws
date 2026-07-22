package com.myorg;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnParameter;
import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.ec2.ISecurityGroup;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.CredentialsFromUsernameOptions;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.MySqlInstanceEngineProps;
import software.amazon.awscdk.services.rds.MysqlEngineVersion;
import software.constructs.Construct;

import java.util.Collections;

public class OrionRdsStack extends Stack {

    public OrionRdsStack(final Construct scope, final String id, final Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public OrionRdsStack(final Construct scope, final String id, final StackProps props, final Vpc vpc) {
        super(scope, id, props);

        CfnParameter senha = CfnParameter.Builder.create(this, "orionDbPassword")
                .type("String")
                .description("Senha do banco MySQL Orion")
                .build();

        ISecurityGroup securityGroup = SecurityGroup.fromSecurityGroupId(
                this,
                "OrionDefaultSecurityGroup",
                vpc.getVpcDefaultSecurityGroup()
        );
        securityGroup.addIngressRule(Peer.anyIpv4(), Port.tcp(3306));

        DatabaseInstance database = DatabaseInstance.Builder
                .create(this, "OrionMysqlDb")
                .instanceIdentifier("orion-dev-mysql-db")
                .engine(DatabaseInstanceEngine.mysql(
                        MySqlInstanceEngineProps.builder()
                                .version(MysqlEngineVersion.VER_8_0)
                                .build()))
                .vpc(vpc)
                .credentials(Credentials.fromUsername(
                        "admin",
                        CredentialsFromUsernameOptions.builder()
                                .password(SecretValue.unsafePlainText(senha.getValueAsString()))
                                .build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE2, InstanceSize.MICRO))
                .multiAz(false)
                .allocatedStorage(10)
                .securityGroups(Collections.singletonList(securityGroup))
                .vpcSubnets(SubnetSelection.builder()
                        .subnets(vpc.getPrivateSubnets())
                        .build())
                .build();

        Tags.of(database).add("project", "orion");
        Tags.of(database).add("environment", "dev");

        CfnOutput.Builder.create(this, "orion-db-endpoint")
                .exportName("orion-db-endpoint")
                .value(database.getDbInstanceEndpointAddress())
                .build();

        CfnOutput.Builder.create(this, "orion-db-password")
                .exportName("orion-db-password")
                .value(senha.getValueAsString())
                .build();
    }
}