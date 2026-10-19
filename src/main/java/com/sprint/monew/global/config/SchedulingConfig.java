package com.sprint.monew.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * {@code @Scheduled}(정해진 시각에 자동 실행)를 켜는 설정. 아래 배치 작업들이 이게 있어야 돈다.
 *
 * <ul>
 *   <li>#39 뉴스기사 자동 수집 (매시간)
 *   <li>#40 뉴스기사 S3 백업 (날짜 단위)
 *   <li>#21 사용자 완전 삭제 (논리 삭제 후 1일 경과)
 *   <li>#53 알림 삭제 (확인 후 1주 경과)
 *   <li>#61 날짜별 로그 S3 적재 (매일)
 * </ul>
 *
 * <p>테스트에서는 꺼진다(application-test.yaml의 {@code monew.scheduling.enabled: false}). 켜져 있으면 테스트 도중 배치가
 * 멋대로 실행돼 테스트 데이터를 바꾸거나 외부 API를 불러서 결과가 들쭉날쭉해진다. 로컬·배포는 설정이 없으면 켜진 상태.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
        name = "monew.scheduling.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class SchedulingConfig {}
