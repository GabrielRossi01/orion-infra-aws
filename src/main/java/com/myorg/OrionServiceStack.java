package com.myorg;

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
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;

public class OrionServiceStack extends Stack {

    public OrionServiceStack(final Construct scope, final String id, final Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public OrionServiceStack(final Construct scope, final String id, final StackProps props, final Cluster cluster) {
        super(scope, id, props);

        Map<String, String> databaseEnv = new HashMap<>();
        databaseEnv.put(
                "SPRING_DATASOURCE_URL",
                "jdbc:mysql://" + Fn.importValue("orion-db-endpoint") +
                        ":3306/oriondb?createDatabaseIfNotExist=true"
        );
        databaseEnv.put("SPRING_DATASOURCE_USERNAME", "admin");
        databaseEnv.put("SPRING_DATASOURCE_PASSWORD", Fn.importValue("orion-db-password"));

        IRepository repository = Repository.fromRepositoryName(
                this,
                "OrionEcrRepository",
                "orion-dev-app"
        );

        ApplicationLoadBalancedFargateService orionService =
                ApplicationLoadBalancedFargateService.Builder.create(this, "OrionAppService")
                        .serviceName("orion-dev-service")
                        .cluster(cluster)
                        .cpu(512)
                        .desiredCount(1)
                        .listenerPort(8080)
                        .assignPublicIp(true)
                        .taskImageOptions(
                                ApplicationLoadBalancedTaskImageOptions.builder()
                                        .image(ContainerImage.fromEcrRepository(repository))
                                        .containerPort(8080)
                                        .containerName("orion-dev-app")
                                        .environment(databaseEnv)
                                        .logDriver(LogDriver.awsLogs(
                                                AwsLogDriverProps.builder()
                                                        .logGroup(LogGroup.Builder.create(this, "OrionLogGroup")
                                                                .logGroupName("/ecs/orion-dev-app")
                                                                .removalPolicy(RemovalPolicy.DESTROY)
                                                                .build())
                                                        .streamPrefix("orion")
                                                        .build()))
                                        .build())
                        .memoryLimitMiB(1024)
                        .publicLoadBalancer(true)
                        .build();

        Tags.of(orionService.getService()).add("project", "orion");
        Tags.of(orionService.getService()).add("environment", "dev");

        ScalableTaskCount scalableTarget =
                orionService.getService().autoScaleTaskCount(
                        EnableScalingProps.builder()
                                .minCapacity(1)
                                .maxCapacity(2)
                                .build()
                );

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