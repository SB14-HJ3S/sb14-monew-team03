package com.sprint.monew.adapter.out.notification;

import com.sprint.monew.application.notification.required.SubscriberReader;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SubscriberReaderAdapter implements SubscriberReader {

    // TODO: 관심사 담당이 실제 구독자 조회로 교체 예정
    @Override
    public List<UUID> findSubscriberIds(UUID interestId) {
        log.warn("SubscriberReader 임시 구현 호출: interestId={}", interestId);
        return List.of();
    }
}
