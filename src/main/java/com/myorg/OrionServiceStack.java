package com.myorg;

import software.amazon.awscdk.CfnParameter;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.Fn;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecr.IRepository;
import software.amazon.awscdk.services.ecr.Repository;
import software.amazon.awscdk.services.ecs.AwsLogDriverProps;
import software.amazon.awscdk.services.ecs.Cluster;
import software.amazon.awscdk.services.ecs.ContainerImage;
import software.amazon.awscdk.services.ecs.CpuUtilizationScalingProps;
import software.amazon.awscdk.services.ecs.LogDriver;
import software.amazon.awscdk.services.ecs.MemoryUtilizationScalingProps;
import software.amazon.awscdk.services.ecs.ScalableTaskCount;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.logs.LogGroup;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;

public class OrionServiceStack extends Stack {

    public OrionServiceStack(final Construct scope, final String id, final Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public OrionServiceStack(final Construct scope, final String id, final StackProps props, final Cluster cluster) {
        super(scope, id, props);

        CfnParameter dbPassword = CfnParameter.Builder.create(this, "orionDbPassword")
                .type("String")
                .description("Senha do banco MySQL Orion")
                .noEcho(true)
                .build();

        Map<String, String> envVars = new HashMap<>();
        envVars.put("SPRING_DATASOURCE_URL", "jdbc:mysql://" + Fn.importValue("orion-db-endpoint") + ":3306/oriondb");
        envVars.put("SPRING_DATASOURCE_USERNAME", "admin");
        envVars.put("SPRING_DATASOURCE_PASSWORD", dbPassword.getValueAsString());

        IRepository repository = Repository.fromRepositoryName(
                this,
                "OrionEcrRepository",
                "orion-dev-app"
        );

        LogGroup logGroup = LogGroup.Builder.create(this, "OrionLogGroup")
                .logGroupName("/ecs/orion-dev-app")
                .retention(RetentionDays.ONE_WEEK)
                .removalPolicy(RemovalPolicy.DESTROY)
                .build();

        ApplicationLoadBalancedFargateService orionService =
                ApplicationLoadBalancedFargateService.Builder.create(this, "OrionAppService")
                        .serviceName("orion-dev-service")
                        .cluster(cluster)
                        .cpu(256)
                        .memoryLimitMiB(512)
                        .desiredCount(1)
                        .listenerPort(8080)
                        .assignPublicIp(true)
                        .publicLoadBalancer(true)
                        .taskImageOptions(
                                ApplicationLoadBalancedTaskImageOptions.builder()
                                        .image(ContainerImage.fromEcrRepository(repository))
                                        .containerPort(8080)
                                        .containerName("orion-dev-app")
                                        .environment(envVars)
                                        .logDriver(LogDriver.awsLogs(
                                                AwsLogDriverProps.builder()
                                                        .logGroup(logGroup)
                                                        .streamPrefix("orion")
                                                        .build()))
                                        .build())
                        .build();

        Tags.of(orionService.getService()).add("project", "orion");
        Tags.of(orionService.getService()).add("environment", "dev");

        ScalableTaskCount scalableTarget = orionService.getService()
                .autoScaleTaskCount(EnableScalingProps.builder()
                        .minCapacity(1)
                        .maxCapacity(2)
                        .build());

        scalableTarget.scaleOnCpuUtilization(
                "OrionCpuScaling",
                CpuUtilizationScalingProps.builder()
                        .targetUtilizationPercent(70)
                        .scaleInCooldown(Duration.minutes(3))
                        .scaleOutCooldown(Duration.minutes(2))
                        .build()
        );

        scalableTarget.scaleOnMemoryUtilization(
                "OrionMemoryScaling",
                MemoryUtilizationScalingProps.builder()
                        .targetUtilizationPercent(65)
                        .scaleInCooldown(Duration.minutes(3))
                        .scaleOutCooldown(Duration.minutes(2))
                        .build()
        );
    }
}