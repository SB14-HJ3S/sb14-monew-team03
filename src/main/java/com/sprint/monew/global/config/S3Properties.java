package com.sprint.monew.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml의 {@code monew.aws.s3.*} 값을 담는 설정 클래스. S3Config가 이 값으로 S3Client를 만들고, S3를 쓰는
 * 기능(#40·#41·#61)은 여기서 버킷 이름을 꺼내 쓴다.
 *
 * <ul>
 *   <li>bucket: 버킷 이름 — 로컬은 .env의 AWS_S3_BUCKET, 배포는 ECS 환경변수 BACKUP_BUCKET_NAME
 *   <li>region: 리전 — 기본값 서울(ap-northeast-2)
 *   <li>accessKeyId·secretAccessKey·sessionToken: 로컬 자격증명(.env). 배포에서는 비워 두고 ECS 역할 권한을 씀
 * </ul>
 *
 * <p>버킷 이름이 비어 있어도 앱은 뜨고, 실제로 S3를 부르는 순간 실패한다 — S3 기능이 생기기 전에도 로컬 실행이 막히지 않게 하려고.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "monew.aws.s3")
public class S3Properties {

    private String bucket;

    private String region = "ap-northeast-2";

    private String accessKeyId;

    private String secretAccessKey;

    private String sessionToken;
}
