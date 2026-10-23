package com.sprint.monew.application.article.event;

import java.util.UUID;

public record ArticlesSavedEvent(UUID interestId, String interestName, int articleCount) {}
