package com.sprint.monew.adapter.in.event.notification;

import com.sprint.monew.application.article.event.ArticlesSavedEvent;
import com.sprint.monew.application.notification.provided.NotificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationUseCase notification;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onArticlesSaved(ArticlesSavedEvent event) {
        try {
            notification.createArticleNotifications(
                    event.interestId(), event.interestName(), event.articleCount());
        } catch (Exception e) {
            log.error("기사 알림 생성 실패: {}", event, e);
        }
    }
}
