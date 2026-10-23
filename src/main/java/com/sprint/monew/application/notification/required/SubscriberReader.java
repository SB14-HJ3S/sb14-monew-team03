package com.sprint.monew.application.notification.required;

import java.util.List;
import java.util.UUID;

public interface SubscriberReader {

    List<UUID> findSubscriberIds(UUID interestId); // 구독자 목록조회
}
