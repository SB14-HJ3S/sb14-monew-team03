package com.sprint.monew.application.article;

import com.sprint.monew.application.article.provided.dto.ArticleBackupItem;
import com.sprint.monew.application.article.required.ArticleInterestRepositoryPort;
import com.sprint.monew.application.article.required.ArticleRepositoryPort;
import com.sprint.monew.application.interest.InterestService;
import com.sprint.monew.domain.article.entity.Article;
import com.sprint.monew.domain.article.entity.ArticleInterest;
import com.sprint.monew.domain.interest.entity.Interest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleRestoreService {

    private final ArticleRepositoryPort articleService;
    private final InterestService interestService;
    private final ArticleInterestRepositoryPort articleInterestService;

    @Transactional
    public List<UUID> restore(List<ArticleBackupItem> entries) {
        if (entries.isEmpty()) {
            return List.of();
        }
        Set<UUID> interestIds = new HashSet<>();
        for (ArticleBackupItem entry : entries) {
            interestIds.addAll(entry.interestIds());
        }
        if (interestIds.isEmpty()) {
            return List.of();
        }
        Map<UUID, Interest> byId = new HashMap<>();
        for (Interest interest : interestService.findAllById(interestIds)) {
            byId.put(interest.getId(), interest);
        }

        List<String> urls = entries.stream().map(ArticleBackupItem::sourceUrl).toList();
        Set<String> existingUrls = new HashSet<>();

        articleService
                .findAllBySourceUrlIn(urls)
                .forEach(article -> existingUrls.add(article.getSourceUrl()));

        List<Article> candidates = new ArrayList<>();
        Map<String, List<Interest>> interestsByUrl = new HashMap<>();
        for (ArticleBackupItem entry : entries) {
            List<Interest> remaining =
                    entry.interestIds().stream()
                            .distinct()
                            .filter(byId::containsKey)
                            .map(byId::get)
                            .toList();
            if (!remaining.isEmpty() && existingUrls.add(entry.sourceUrl())) {
                candidates.add(
                        Article.restore(
                                entry.source(),
                                entry.sourceUrl(),
                                entry.title(),
                                entry.publishedAt(),
                                entry.summary()));
                interestsByUrl.put(entry.sourceUrl(), remaining);
            }
        }
        List<Article> saved = articleService.saveAll(candidates);
        List<ArticleInterest> links = new ArrayList<>();
        for (Article article : saved) {
            for (Interest interest : interestsByUrl.get(article.getSourceUrl())) {
                links.add(ArticleInterest.create(article, interest));
            }
        }
        articleInterestService.saveAll(links);
        for (Article a : saved) {
            log.info(
                    "기사 복구 완료. 제목={},  게시일시={},  id={}",
                    a.getTitle(),
                    a.getPublishedAt(),
                    a.getId());
        }
        return saved.stream().map(Article::getId).toList();
    }
}
