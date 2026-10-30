package com.sprint.monew.adapter.out.article.naver;

import java.util.List;

public record NaverNewsResponse(
        String lastBuildDate, int total, int start, int display, List<ArticleItem> items) {
    public record ArticleItem(
            String title, String originallink, String link, String description, String pubDate) {}
}
