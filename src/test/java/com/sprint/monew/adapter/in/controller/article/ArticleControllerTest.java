package com.sprint.monew.adapter.in.controller.article;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sprint.monew.application.article.provided.ArticleUseCase;
import com.sprint.monew.application.article.provided.dto.ArticleQuery;
import com.sprint.monew.application.article.provided.dto.ArticleResult;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.pagination.SortDirection;
import com.sprint.monew.global.type.ArticleSourceType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(ArticleController.class)
@DisplayName("GET /api/articles")
class ArticleControllerTest {

    private static final String USER_HEADER = "Monew-Request-User-ID";
    private static final UUID USER_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ArticleUseCase articleUseCase;

    @Test
    @DisplayName("모든 검색 조건과 커서를 전달하고 Swagger의 페이지 응답 필드를 반환한다")
    void bindsFiltersAndReturnsPage() throws Exception {
        UUID interestId = UUID.randomUUID();
        UUID articleId = UUID.randomUUID();
        Instant after = Instant.parse("2026-10-22T00:00:00Z");
        LocalDateTime publishedAt = LocalDateTime.of(2026, 10, 22, 9, 0);
        ArticleResult item =
                new ArticleResult(
                        articleId,
                        ArticleSourceType.NAVER,
                        "https://example.com/news",
                        "경제 뉴스",
                        publishedAt,
                        "요약",
                        3L,
                        10L,
                        true);
        given(articleUseCase.findArticlePage(any()))
                .willReturn(new PageResult<>(List.of(item), "3", after, 1, 5, true));

        mockMvc.perform(
                        request()
                                .param("keyword", "경제")
                                .param("interestId", interestId.toString())
                                .param("sourceIn", "NAVER", "HANKYUNG")
                                .param("publishDateFrom", "2026-10-20T00:00:00")
                                .param("publishDateTo", "2026-10-22T23:59:59")
                                .param("cursor", "5")
                                .param("after", after.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(articleId.toString()))
                .andExpect(jsonPath("$.content[0].source").value("NAVER"))
                .andExpect(jsonPath("$.content[0].sourceUrl").value("https://example.com/news"))
                .andExpect(jsonPath("$.content[0].title").value("경제 뉴스"))
                .andExpect(jsonPath("$.content[0].publishDate").value("2026-10-22T09:00:00"))
                .andExpect(jsonPath("$.content[0].summary").value("요약"))
                .andExpect(jsonPath("$.content[0].commentCount").value(3))
                .andExpect(jsonPath("$.content[0].viewCount").value(10))
                .andExpect(jsonPath("$.content[0].viewedByMe").value(true))
                .andExpect(jsonPath("$.nextCursor").value("3"))
                .andExpect(jsonPath("$.nextAfter").value(after.toString()))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.hasNext").value(true));

        ArgumentCaptor<ArticleQuery> captor = ArgumentCaptor.forClass(ArticleQuery.class);
        verify(articleUseCase).findArticlePage(captor.capture());
        assertThat(captor.getValue())
                .isEqualTo(
                        new ArticleQuery(
                                "경제",
                                interestId,
                                List.of(ArticleSourceType.NAVER, ArticleSourceType.HANKYUNG),
                                LocalDateTime.parse("2026-10-20T00:00:00"),
                                LocalDateTime.parse("2026-10-22T23:59:59"),
                                ArticleQuery.SortOrder.COMMENT_COUNT,
                                SortDirection.DESC,
                                "5",
                                after,
                                2));
    }

    @ParameterizedTest
    @CsvSource({
        "publishDate, PUBLISH_DATE",
        "commentCount, COMMENT_COUNT",
        "viewCount, VIEW_COUNT"
    })
    @DisplayName("camelCase 정렬값을 enum으로 변환하고 첫 페이지의 커서는 null로 전달한다")
    void convertsSortField(String parameter, ArticleQuery.SortOrder expected) throws Exception {
        given(articleUseCase.findArticlePage(any()))
                .willReturn(new PageResult<>(List.of(), null, null, 0, 0, false));
        MockHttpServletRequestBuilder request =
                get("/api/articles")
                        .header(USER_HEADER, USER_ID)
                        .param("orderBy", parameter)
                        .param("direction", "DESC")
                        .param("limit", "2");
        mockMvc.perform(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.hasNext").value(false));
        ArgumentCaptor<ArticleQuery> captor = ArgumentCaptor.forClass(ArticleQuery.class);
        verify(articleUseCase).findArticlePage(captor.capture());
        assertThat(captor.getValue().orderBy()).isEqualTo(expected);
        assertThat(captor.getValue().cursor()).isNull();
        assertThat(captor.getValue().after()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
        "limit, 0",
        "limit, -1",
        "orderBy, unsupported",
        "direction, sideways",
        "interestId, invalid-uuid",
        "after, invalid-date"
    })
    @DisplayName("잘못된 파라미터는 400이며 유스케이스를 실행하지 않는다")
    void rejectsInvalidParameters(String key, String value) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/articles").header(USER_HEADER, USER_ID);
        request.param("orderBy", key.equals("orderBy") ? value : "commentCount");
        request.param("direction", key.equals("direction") ? value : "DESC");
        request.param("limit", key.equals("limit") ? value : "2");
        if (!List.of("orderBy", "direction", "limit").contains(key)) {
            request.param(key, value);
        }
        mockMvc.perform(request).andExpect(status().isBadRequest());
        verifyNoInteractions(articleUseCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"orderBy", "direction", "limit", "header"})
    @DisplayName("필수 파라미터나 사용자 헤더가 없으면 400이다")
    void rejectsMissingRequiredInput(String missing) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/articles");
        if (!missing.equals("header")) request.header(USER_HEADER, USER_ID);
        if (!missing.equals("orderBy")) request.param("orderBy", "commentCount");
        if (!missing.equals("direction")) request.param("direction", "DESC");
        if (!missing.equals("limit")) request.param("limit", "2");
        mockMvc.perform(request).andExpect(status().isBadRequest());
        verifyNoInteractions(articleUseCase);
    }

    private MockHttpServletRequestBuilder request() {
        return get("/api/articles")
                .header(USER_HEADER, USER_ID)
                .param("orderBy", "commentCount")
                .param("direction", "DESC")
                .param("limit", "2");
    }
}
