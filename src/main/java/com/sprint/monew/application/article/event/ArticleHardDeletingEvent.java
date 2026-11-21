package com.sprint.monew.application.article.event;

import java.util.UUID;

public record ArticleHardDeletingEvent(UUID articleId) {}
