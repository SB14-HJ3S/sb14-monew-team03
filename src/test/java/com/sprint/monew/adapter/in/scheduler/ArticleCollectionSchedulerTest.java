package com.sprint.monew.adapter.in.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;

import com.sprint.monew.adapter.in.batch.ArticleCollectionRunner;
import com.sprint.monew.global.config.SchedulingConfig;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.CronTask;
import org.springframework.scheduling.support.CronExpression;

@ExtendWith(MockitoExtension.class)
class ArticleCollectionSchedulerTest {

    @Mock private ArticleCollectionRunner runner;

    private ApplicationContextRunner contextRunner() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application.yaml"));
        String cron = yaml.getObject().getProperty("monew.news.collection.cron");
        return new ApplicationContextRunner()
                .withUserConfiguration(SchedulingConfig.class, ArticleCollectionScheduler.class)
                .withBean(ArticleCollectionRunner.class, () -> runner)
                .withPropertyValues("monew.news.collection.cron=" + cron);
    }

    @Test
    @DisplayName("기본 설정으로 실행하면 서울 시간 기준 매시간 정각 스케줄을 등록한다.")
    void givenDefaultSchedule_whenContextStarts_thenRegisterHourlySeoulSchedule()
            throws NoSuchMethodException {
        // given
        ApplicationContextRunner application = contextRunner();
        String zone =
                ArticleCollectionScheduler.class
                        .getMethod("collect")
                        .getAnnotation(Scheduled.class)
                        .zone();
        // when
        application.run(
                context -> {
                    // then
                    assertThat(context).hasNotFailed();
                    assertThat(zone).isEqualTo("Asia/Seoul");
                    var tasks =
                            context.getBean(ScheduledAnnotationBeanPostProcessor.class)
                                    .getScheduledTasks();
                    assertThat(tasks).hasSize(1);
                    CronTask task = (CronTask) tasks.iterator().next().getTask();
                    CronExpression cron = CronExpression.parse(task.getExpression());
                    ZonedDateTime before =
                            ZonedDateTime.of(2026, 10, 27, 12, 37, 15, 0, ZoneId.of(zone));
                    ZonedDateTime next = before.withHour(13).withMinute(0).withSecond(0);
                    assertThat(cron.next(before)).isEqualTo(next);
                    assertThat(cron.next(next)).isEqualTo(next.plusHours(1));
                });
    }

    @Test
    @DisplayName("스케줄링을 비활성화하면 수집 스케줄을 등록하지 않는다.")
    void givenDisabledScheduling_whenContextStarts_thenDoNotRegisterCollector() {
        // given
        ApplicationContextRunner application =
                contextRunner().withPropertyValues("monew.scheduling.enabled=false");

        // when
        application.run(
                context -> {
                    assertThat(context).hasNotFailed();
                    // then
                    assertThat(context).doesNotHaveBean(ArticleCollectionScheduler.class);
                    assertThat(context).doesNotHaveBean(ScheduledAnnotationBeanPostProcessor.class);
                    verifyNoInteractions(runner);
                });
    }
}
