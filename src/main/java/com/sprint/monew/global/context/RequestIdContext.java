package com.sprint.monew.global.context;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RequestIdContext {

    private static final ThreadLocal<UUID> REQUEST_ID = new ThreadLocal<>();

    public static void set(UUID requestId) {
        REQUEST_ID.set(requestId);
    }

    public static UUID get() {
        return REQUEST_ID.get();
    }

    public static void clear() {
        REQUEST_ID.remove();
    }
}
