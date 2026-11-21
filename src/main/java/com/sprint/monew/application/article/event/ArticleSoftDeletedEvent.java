package com.sprint.monew.application.article.event;

import java.util.UUID;

public record ArticleSoftDeletedEvent(UUID articleId) {}
