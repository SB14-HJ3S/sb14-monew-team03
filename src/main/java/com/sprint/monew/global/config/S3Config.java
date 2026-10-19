package com.sprint.monew.global.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

/**
 * AWS S3에 파일을 올리고 내려받는 S3Client를 하나 만들어 두는 설정 (#65). 아래 기능에서 주입받아 같이 쓴다.
 *
 * <ul>
 *   <li>#40 뉴스기사 데이터 S3 백업 배치 — 날짜별 기사 데이터를 S3에 저장
 *   <li>#41 뉴스기사 복구 API — S3 백업에서 기사를 되살림
 *   <li>#61 배치 날짜별 로그 S3 적재
 * </ul>
 *
 * <p>자격증명: 로컬은 .env의 AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY를 쓰고, 값이 없으면(배포 환경) ECS 역할 권한을 자동으로 쓴다.
 * 버킷 이름·리전은 S3Properties(monew.aws.s3.*)에서 읽는다.
 */
@Configuration
@EnableConfigurationProperties(S3Properties.class)
public class S3Config {

    private static final Logger log = LoggerFactory.getLogger(S3Config.class);

    @Bean
    public S3Client s3Client(S3Properties properties) {
        S3ClientBuilder builder = S3Client.builder().region(Region.of(properties.getRegion()));
        boolean hasAccessKeyId = hasText(properties.getAccessKeyId());
        boolean hasSecretAccessKey = hasText(properties.getSecretAccessKey());
        if (hasAccessKeyId && hasSecretAccessKey) {
            builder.credentialsProvider(
                    StaticCredentialsProvider.create(createCredentials(properties)));
        } else if (hasAccessKeyId || hasSecretAccessKey || hasText(properties.getSessionToken())) {
            // 키가 하나만 있으면 보통 .env 오타라서, 기본 체인으로 넘어가되 경고를 남긴다
            log.warn(
                    "S3 자격증명이 일부만 설정돼 있어 AWS SDK 기본 자격증명 체인(ECS 역할·AWS 프로필 등)을 씁니다. "
                            + ".env의 AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY를 확인하세요. "
                            + "(accessKeyId 설정됨={}, secretAccessKey 설정됨={})",
                    hasAccessKeyId,
                    hasSecretAccessKey);
        }
        return builder.build();
    }

    private AwsCredentials createCredentials(S3Properties properties) {
        if (hasText(properties.getSessionToken())) {
            return AwsSessionCredentials.create(
                    properties.getAccessKeyId(),
                    properties.getSecretAccessKey(),
                    properties.getSessionToken());
        }
        return AwsBasicCredentials.create(
                properties.getAccessKeyId(), properties.getSecretAccessKey());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
