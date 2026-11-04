package com.sprint.monew.application.user.event;

import java.util.List;
import java.util.UUID;

public record UserHardDeletingEvent(List<UUID> userIds) {}
