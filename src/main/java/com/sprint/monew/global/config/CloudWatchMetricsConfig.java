package com.sprint.monew.global.config;

import io.micrometer.cloudwatch2.CloudWatchConfig;
import io.micrometer.cloudwatch2.CloudWatchMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

@Configuration
@Profile("prod")
public class CloudWatchMetricsConfig {

    private static final String BATCH_JOB_METER = "spring.batch.job";

    @Bean
    public CloudWatchAsyncClient cloudWatchAsyncClient(
            @Value("${AWS_REGION:ap-northeast-2}") String region) {
        return CloudWatchAsyncClient.builder().region(Region.of(region)).build();
    }

    @Bean
    public CloudWatchMeterRegistry cloudWatchMeterRegistry(
            CloudWatchAsyncClient client,
            @Value("${monew.cloudwatch.namespace}") String namespace,
            @Value("${monew.cloudwatch.metrics}") String metrics) {
        CloudWatchConfig config =
                new CloudWatchConfig() {
                    @Override
                    public String get(String key) {
                        return null;
                    }

                    @Override
                    public String namespace() {
                        return namespace;
                    }

                    @Override
                    public Duration step() {
                        return Duration.ofMinutes(1);
                    }
                };
        CloudWatchMeterRegistry registry =
                new CloudWatchMeterRegistry(config, Clock.SYSTEM, client);
        registry.config().meterFilter(MeterFilter.denyUnless(isAllowed(parse(metrics))));
        return registry;
    }

    static Set<String> parse(String metrics) {
        return Arrays.stream(metrics.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .collect(Collectors.toSet());
    }

    static Predicate<Meter.Id> isAllowed(Set<String> entries) {
        return id -> {
            if (BATCH_JOB_METER.equals(id.getName())) {
                return "COMPLETED".equals(id.getTag("spring.batch.job.status"))
                        && entries.contains(
                                BATCH_JOB_METER + ":" + id.getTag("spring.batch.job.name"));
            }
            return entries.contains(id.getName());
        };
    }
}
