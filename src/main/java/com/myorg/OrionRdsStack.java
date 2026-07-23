package com.myorg;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.CfnParameter;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.SecretValue;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.ec2.InstanceClass;
import software.amazon.awscdk.services.ec2.InstanceSize;
import software.amazon.awscdk.services.ec2.InstanceType;
import software.amazon.awscdk.services.ec2.Peer;
import software.amazon.awscdk.services.ec2.Port;
import software.amazon.awscdk.services.ec2.SecurityGroup;
import software.amazon.awscdk.services.ec2.SubnetSelection;
import software.amazon.awscdk.services.ec2.SubnetType;
import software.amazon.awscdk.services.ec2.Vpc;
import software.amazon.awscdk.services.rds.Credentials;
import software.amazon.awscdk.services.rds.CredentialsFromUsernameOptions;
import software.amazon.awscdk.services.rds.DatabaseInstance;
import software.amazon.awscdk.services.rds.DatabaseInstanceEngine;
import software.amazon.awscdk.services.rds.MySqlInstanceEngineProps;
import software.amazon.awscdk.services.rds.MysqlEngineVersion;
import software.constructs.Construct;

public class OrionRdsStack extends Stack {

    public OrionRdsStack(final Construct scope, final String id, final Vpc vpc) {
        this(scope, id, null, vpc);
    }

    public OrionRdsStack(final Construct scope, final String id, final StackProps props, final Vpc vpc) {
        super(scope, id, props);

        CfnParameter dbPassword = CfnParameter.Builder.create(this, "orionDbPassword")
                .type("String")
                .description("Senha do banco MySQL Orion")
                .noEcho(true)
                .build();

        SecurityGroup rdsSecurityGroup = SecurityGroup.Builder.create(this, "OrionRdsSecurityGroup")
                .vpc(vpc)
                .description("Security group do RDS Orion")
                .allowAllOutbound(true)
                .build();

        rdsSecurityGroup.addIngressRule(
                Peer.ipv4(vpc.getVpcCidrBlock()),
                Port.tcp(3306),
                "Permite acesso MySQL somente a partir da VPC"
        );

        DatabaseInstance database = DatabaseInstance.Builder.create(this, "OrionMysqlDb")
                .instanceIdentifier("orion-dev-mysql-db")
                .engine(DatabaseInstanceEngine.mysql(
                        MySqlInstanceEngineProps.builder()
                                .version(MysqlEngineVersion.VER_8_0)
                                .build()))
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder()
                        .subnetType(SubnetType.PRIVATE_ISOLATED)
                        .build())
                .securityGroups(java.util.List.of(rdsSecurityGroup))
                .credentials(Credentials.fromUsername(
                        "admin",
                        CredentialsFromUsernameOptions.builder()
                                .password(SecretValue.unsafePlainText(dbPassword.getValueAsString()))
                                .build()))
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.MICRO))
                .multiAz(false)
                .allocatedStorage(20)
                .maxAllocatedStorage(20)
                .backupRetention(Duration.days(0))
                .deleteAutomatedBackups(true)
                .deletionProtection(false)
                .publiclyAccessible(false)
                .removalPolicy(RemovalPolicy.DESTROY)
                .databaseName("oriondb")
                .build();

        Tags.of(database).add("project", "orion");
        Tags.of(database).add("environment", "dev");

        CfnOutput.Builder.create(this, "orion-db-endpoint")
                .exportName("orion-db-endpoint")
                .value(database.getDbInstanceEndpointAddress())
                .build();

        CfnOutput.Builder.create(this, "orion-db-port")
                .exportName("orion-db-port")
                .value(database.getDbInstanceEndpointPort())
                .build();
    }
}