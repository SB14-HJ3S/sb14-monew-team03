package com.sprint.monew.adapter.in.controller.interest;

import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.monew.adapter.in.controller.interest.dto.request.InterestRegisterRequest;
import com.sprint.monew.adapter.in.controller.interest.dto.request.InterestUpdateRequest;
import com.sprint.monew.application.interest.provided.InterestUseCase;
import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.application.interest.provided.dto.InterestResult;
import com.sprint.monew.application.interest.provided.dto.SubscriptionResult;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.interest.entity.InterestKeyword;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.InterestErrorType;
import com.sprint.monew.global.exception.errortype.SubscriptionErrorType;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.pagination.SortDirection;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(InterestControllerImpl.class)
class InterestControllerImplTest {

    private static final String USER_HEADER = "Monew-Request-User-ID";
    private static final UUID USER_ID = UUID.randomUUID();

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private InterestUseCase interestUseCase;

    @Test
    @DisplayName("관심사 등록에 성공하면 201과 등록한 관심사 정보를 응답한다.")
    void givenValidRequest_whenRegister_thenCreated() throws Exception {
        UUID interestId = UUID.randomUUID();
        List<String> keywords = List.of("축구", "야구");
        when(interestUseCase.register("스포츠", keywords))
                .thenReturn(new InterestResult(interestId, "스포츠", keywords, 0L, false));

        mockMvc.perform(
                        post("/api/interests")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new InterestRegisterRequest("스포츠", keywords))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(interestId.toString()))
                .andExpect(jsonPath("$.name").value("스포츠"))
                .andExpect(jsonPath("$.keywords").value(contains("축구", "야구")))
                .andExpect(jsonPath("$.subscriberCount").value(0))
                .andExpect(jsonPath("$.subscribedByMe").value(false));

        verify(interestUseCase).register("스포츠", keywords);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "123456789012345678901234567890123456789012345678901"})
    @DisplayName("이름이 없거나 공백이거나 최대 길이를 초과하면 등록 요청을 거절한다.")
    void givenInvalidName_whenRegister_thenBadRequest(String name) throws Exception {
        mockMvc.perform(
                        post("/api/interests")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new InterestRegisterRequest(name, List.of("축구")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details.name").exists());

        verifyNoInteractions(interestUseCase);
    }

    @ParameterizedTest
    @MethodSource("invalidKeywords")
    @DisplayName("키워드 목록과 각 키워드가 유효하지 않으면 등록 요청을 거절한다.")
    void givenInvalidKeywords_whenRegister_thenBadRequest(List<String> keywords) throws Exception {
        mockMvc.perform(
                        post("/api/interests")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new InterestRegisterRequest("스포츠", keywords))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details").isNotEmpty());

        verifyNoInteractions(interestUseCase);
    }

    @Test
    @DisplayName("유사한 관심사 이름이 이미 있으면 409와 에러 정보를 응답한다.")
    void givenSimilarName_whenRegister_thenConflict() throws Exception {
        when(interestUseCase.register("스포츠", List.of("축구")))
                .thenThrow(new MonewException(InterestErrorType.SIMILAR_NAME_CONFLICT));

        mockMvc.perform(
                        post("/api/interests")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new InterestRegisterRequest("스포츠", List.of("축구")))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SIMILAR_NAME_CONFLICT"));

        verify(interestUseCase).register("스포츠", List.of("축구"));
    }

    @Test
    @DisplayName("조회 조건과 커서를 전달하고 관심사 목록과 페이지 정보를 응답한다.")
    void givenFiltersAndCursor_whenFind_thenReturnPage() throws Exception {
        UUID interestId = UUID.randomUUID();
        Instant after = Instant.parse("2026-11-01T00:00:00Z");
        Instant nextAfter = Instant.parse("2026-11-02T00:00:00Z");
        List<String> keywords = List.of("축구", "야구");
        InterestQuery query =
                new InterestQuery(
                        "스포츠",
                        InterestQuery.SortOrder.SUBSCRIBER_COUNT,
                        SortDirection.DESC,
                        "10",
                        after,
                        2);
        when(interestUseCase.findByQuery(query))
                .thenReturn(
                        new PageResult<>(
                                List.of(new InterestResult(interestId, "스포츠", keywords, 5L, true)),
                                "5",
                                nextAfter,
                                1,
                                3,
                                true));

        mockMvc.perform(
                        get("/api/interests")
                                .header(USER_HEADER, USER_ID)
                                .param("keyword", "스포츠")
                                .param("orderBy", "subscriberCount")
                                .param("direction", "DESC")
                                .param("cursor", "10")
                                .param("after", after.toString())
                                .param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(interestId.toString()))
                .andExpect(jsonPath("$.content[0].name").value("스포츠"))
                .andExpect(jsonPath("$.content[0].keywords").value(contains("축구", "야구")))
                .andExpect(jsonPath("$.content[0].subscriberCount").value(5))
                .andExpect(jsonPath("$.content[0].subscribedByMe").value(true))
                .andExpect(jsonPath("$.nextCursor").value("5"))
                .andExpect(jsonPath("$.nextAfter").value(nextAfter.toString()))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.hasNext").value(true));

        verify(interestUseCase).findByQuery(query);
    }

    @ParameterizedTest
    @CsvSource({
        "name, ASC, NAME, 1",
        "name, ASC, NAME, 10",
        "subscriberCount, DESC, SUBSCRIBER_COUNT, 10"
    })
    @DisplayName("정렬 파라미터를 변환하고 선택 조건이 없는 첫 페이지를 조회한다.")
    void givenSortParameters_whenFind_thenReturnEmptyPage(
            String orderBy,
            SortDirection direction,
            InterestQuery.SortOrder expectedOrder,
            int limit)
            throws Exception {
        InterestQuery query = new InterestQuery(null, expectedOrder, direction, null, null, limit);
        when(interestUseCase.findByQuery(query))
                .thenReturn(new PageResult<>(List.of(), null, null, 0, 0, false));

        mockMvc.perform(
                        get("/api/interests")
                                .header(USER_HEADER, USER_ID)
                                .param("orderBy", orderBy)
                                .param("direction", direction.name())
                                .param("limit", Integer.toString(limit)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.nextCursor").isEmpty())
                .andExpect(jsonPath("$.nextAfter").isEmpty())
                .andExpect(jsonPath("$.size").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.hasNext").value(false));

        verify(interestUseCase).findByQuery(query);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    @DisplayName("페이지 크기가 1 미만이면 400을 응답하고 조회하지 않는다.")
    void givenNonPositiveLimit_whenFind_thenBadRequest(int limit) throws Exception {
        mockMvc.perform(
                        get("/api/interests")
                                .header(USER_HEADER, USER_ID)
                                .param("orderBy", "name")
                                .param("direction", "ASC")
                                .param("limit", Integer.toString(limit)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details.limit").value("페이지 크기는 1 이상이어야 합니다"));

        verifyNoInteractions(interestUseCase);
    }

    @ParameterizedTest
    @ValueSource(strings = {"orderBy", "direction", "limit"})
    @DisplayName("필수 조회 파라미터가 없으면 400을 응답한다.")
    void givenMissingParameter_whenFind_thenBadRequest(String missing) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/interests").header(USER_HEADER, USER_ID);
        Map.of("orderBy", "name", "direction", "ASC", "limit", "10")
                .forEach(
                        (key, value) -> {
                            if (!key.equals(missing)) request.param(key, value);
                        });

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details." + missing).exists());

        verifyNoInteractions(interestUseCase);
    }

    @ParameterizedTest
    @CsvSource({
        "orderBy, unsupported",
        "direction, sideways",
        "limit, invalid",
        "after, invalid-date"
    })
    @DisplayName("조회 파라미터의 형식이 잘못되면 400을 응답한다.")
    void givenInvalidParameter_whenFind_thenBadRequest(String key, String value) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/interests").header(USER_HEADER, USER_ID);
        request.param("orderBy", key.equals("orderBy") ? value : "name");
        request.param("direction", key.equals("direction") ? value : "ASC");
        request.param("limit", key.equals("limit") ? value : "10");
        if (key.equals("after")) request.param(key, value);

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details." + key).exists());

        verifyNoInteractions(interestUseCase);
    }

    @Test
    @DisplayName("커서에 해당하는 관심사가 없으면 404를 응답한다.")
    void givenMissingCursorInterest_whenFind_thenNotFound() throws Exception {
        when(interestUseCase.findByQuery(any()))
                .thenThrow(new MonewException(InterestErrorType.INTEREST_NOT_FOUND));

        mockMvc.perform(
                        get("/api/interests")
                                .header(USER_HEADER, USER_ID)
                                .param("orderBy", "name")
                                .param("direction", "ASC")
                                .param("cursor", UUID.randomUUID().toString())
                                .param("limit", "10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));
    }

    @Test
    @DisplayName("키워드 수정에 성공하면 200과 수정한 관심사 정보를 응답한다.")
    void givenValidKeywords_whenUpdate_thenOk() throws Exception {
        UUID interestId = UUID.randomUUID();
        List<String> keywords = List.of("축구", "야구");
        when(interestUseCase.update(interestId, keywords))
                .thenReturn(new InterestResult(interestId, "스포츠", keywords, 3L, null));

        mockMvc.perform(
                        patch("/api/interests/{interestId}", interestId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new InterestUpdateRequest(keywords))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(interestId.toString()))
                .andExpect(jsonPath("$.name").value("스포츠"))
                .andExpect(jsonPath("$.keywords").value(contains("축구", "야구")))
                .andExpect(jsonPath("$.subscriberCount").value(3))
                .andExpect(jsonPath("$.subscribedByMe").isEmpty());

        verify(interestUseCase).update(interestId, keywords);
    }

    @ParameterizedTest
    @MethodSource("invalidKeywords")
    @DisplayName("키워드 목록과 각 키워드가 유효하지 않으면 수정 요청을 거절한다.")
    void givenInvalidKeywords_whenUpdate_thenBadRequest(List<String> keywords) throws Exception {
        mockMvc.perform(
                        patch("/api/interests/{interestId}", UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new InterestUpdateRequest(keywords))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details").isNotEmpty());

        verifyNoInteractions(interestUseCase);
    }

    @Test
    @DisplayName("존재하지 않는 관심사의 키워드를 수정하면 404를 응답한다.")
    void givenNonExistingInterest_whenUpdate_thenNotFound() throws Exception {
        UUID interestId = UUID.randomUUID();
        List<String> keywords = List.of("축구");
        when(interestUseCase.update(interestId, keywords))
                .thenThrow(
                        new MonewException(
                                InterestErrorType.INTEREST_NOT_FOUND, Map.of("id", interestId)));

        mockMvc.perform(
                        patch("/api/interests/{interestId}", interestId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new InterestUpdateRequest(keywords))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"))
                .andExpect(jsonPath("$.details.id").value(interestId.toString()));

        verify(interestUseCase).update(interestId, keywords);
    }

    @Test
    @DisplayName("구독에 성공하면 200과 구독 정보를 응답한다.")
    void givenInterest_whenSubscribe_thenOk() throws Exception {
        UUID interestId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-11-01T00:00:00Z");
        List<String> keywords = List.of("축구");
        when(interestUseCase.subscribe(interestId))
                .thenReturn(
                        new SubscriptionResult(
                                subscriptionId, interestId, "스포츠", keywords, 1L, createdAt));

        mockMvc.perform(
                        post("/api/interests/{interestId}/subscriptions", interestId)
                                .header(USER_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(subscriptionId.toString()))
                .andExpect(jsonPath("$.interestId").value(interestId.toString()))
                .andExpect(jsonPath("$.interestName").value("스포츠"))
                .andExpect(jsonPath("$.interestKeywords").value(contains("축구")))
                .andExpect(jsonPath("$.interestSubscriberCount").value(1))
                .andExpect(jsonPath("$.createdAt").value(createdAt.toString()));

        verify(interestUseCase).subscribe(interestId);
    }

    @Test
    @DisplayName("존재하지 않는 관심사를 구독하면 404를 응답한다.")
    void givenNonExistingInterest_whenSubscribe_thenNotFound() throws Exception {
        UUID interestId = UUID.randomUUID();
        when(interestUseCase.subscribe(interestId))
                .thenThrow(new MonewException(InterestErrorType.INTEREST_NOT_FOUND));

        mockMvc.perform(
                        post("/api/interests/{interestId}/subscriptions", interestId)
                                .header(USER_HEADER, USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"));

        verify(interestUseCase).subscribe(interestId);
    }

    @Test
    @DisplayName("이미 구독한 관심사를 다시 구독하면 409를 응답한다.")
    void givenExistingSubscription_whenSubscribe_thenConflict() throws Exception {
        UUID interestId = UUID.randomUUID();
        when(interestUseCase.subscribe(interestId))
                .thenThrow(new MonewException(SubscriptionErrorType.SUBSCRIPTION_ALREADY_EXISTS));

        mockMvc.perform(
                        post("/api/interests/{interestId}/subscriptions", interestId)
                                .header(USER_HEADER, USER_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_ALREADY_EXISTS"));

        verify(interestUseCase).subscribe(interestId);
    }

    @Test
    @DisplayName("구독 취소에 성공하면 200과 빈 본문을 응답한다.")
    void givenSubscription_whenUnsubscribe_thenOk() throws Exception {
        UUID interestId = UUID.randomUUID();

        mockMvc.perform(
                        delete("/api/interests/{interestId}/subscriptions", interestId)
                                .header(USER_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(""));

        verify(interestUseCase).unsubscribe(interestId);
    }

    @Test
    @DisplayName("구독 정보가 없으면 구독 취소 시 404를 응답한다.")
    void givenMissingSubscription_whenUnsubscribe_thenNotFound() throws Exception {
        UUID interestId = UUID.randomUUID();
        doThrow(new MonewException(SubscriptionErrorType.SUBSCRIPTION_NOT_FOUND))
                .when(interestUseCase)
                .unsubscribe(interestId);

        mockMvc.perform(
                        delete("/api/interests/{interestId}/subscriptions", interestId)
                                .header(USER_HEADER, USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_NOT_FOUND"));

        verify(interestUseCase).unsubscribe(interestId);
    }

    @ParameterizedTest
    @CsvSource({
        "GET, /api/interests,",
        "GET, /api/interests, invalid-id",
        "POST, /api/interests/{interestId}/subscriptions,",
        "POST, /api/interests/{interestId}/subscriptions, invalid-id",
        "DELETE, /api/interests/{interestId}/subscriptions,",
        "DELETE, /api/interests/{interestId}/subscriptions, invalid-id"
    })
    @DisplayName("조회와 구독 API의 사용자 헤더가 없거나 UUID가 아니면 400을 응답한다.")
    void givenInvalidUserHeader_whenRequest_thenBadRequest(
            String method, String path, String header) throws Exception {
        MockHttpServletRequestBuilder request =
                request(HttpMethod.valueOf(method), path, UUID.randomUUID())
                        .param("orderBy", "name")
                        .param("direction", "ASC")
                        .param("limit", "10");
        if (header != null) request.header(USER_HEADER, header);

        mockMvc.perform(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details").isNotEmpty());

        verifyNoInteractions(interestUseCase);
    }

    @ParameterizedTest
    @CsvSource({
        "PATCH, /api/interests/{interestId}",
        "POST, /api/interests/{interestId}/subscriptions",
        "DELETE, /api/interests/{interestId}/subscriptions"
    })
    @DisplayName("수정과 구독 API의 관심사 ID가 UUID가 아니면 400을 응답한다.")
    void givenInvalidInterestId_whenUpdateOrSubscribe_thenBadRequest(String method, String path)
            throws Exception {
        mockMvc.perform(
                        request(HttpMethod.valueOf(method), path, "invalid-id")
                                .header(USER_HEADER, USER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        objectMapper.writeValueAsString(
                                                new InterestUpdateRequest(List.of("축구")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details.interestId").exists());

        verifyNoInteractions(interestUseCase);
    }

    @ParameterizedTest
    @CsvSource({
        "POST, /api/interests, ''",
        "POST, /api/interests, '{'",
        "PATCH, /api/interests/{interestId}, ''",
        "PATCH, /api/interests/{interestId}, '{'"
    })
    @DisplayName("요청 본문이 없거나 JSON이 깨져 있으면 400을 응답한다.")
    void givenMalformedBody_whenRegisterOrUpdate_thenBadRequest(
            String method, String path, String body) throws Exception {
        mockMvc.perform(
                        request(HttpMethod.valueOf(method), path, UUID.randomUUID())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        verifyNoInteractions(interestUseCase);
    }

    private static Stream<Arguments> invalidKeywords() {
        return Stream.of(
                Arguments.of((Object) null),
                Arguments.of(List.of()),
                Arguments.of(List.of("")),
                Arguments.of(List.of("   ")),
                Arguments.of(Collections.singletonList(null)),
                Arguments.of(List.of("a".repeat(InterestKeyword.MAX_KEYWORD_LENGTH + 1))),
                Arguments.of(Collections.nCopies(Interest.MAX_KEYWORDS_SIZE + 1, "축구")));
    }

    @Test
    @DisplayName("관심사 삭제에 성공하면 204를 응답한다.")
    void givenExistingInterest_whenDelete_thenNoContent() throws Exception {
        UUID interestId = UUID.randomUUID();

        mockMvc.perform(delete("/api/interests/{interestId}", interestId))
                .andExpect(status().isNoContent());

        verify(interestUseCase).delete(interestId);
    }

    @Test
    @DisplayName("존재하지 않는 관심사를 삭제하면 404와 에러 정보를 응답한다.")
    void givenNonExistingInterest_whenDelete_thenNotFound() throws Exception {
        UUID interestId = UUID.randomUUID();
        doThrow(new MonewException(InterestErrorType.INTEREST_NOT_FOUND, Map.of("id", interestId)))
                .when(interestUseCase)
                .delete(interestId);

        mockMvc.perform(delete("/api/interests/{interestId}", interestId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INTEREST_NOT_FOUND"))
                .andExpect(jsonPath("$.details.id").value(interestId.toString()));

        verify(interestUseCase).delete(interestId);
    }

    @Test
    @DisplayName("관심사 ID가 UUID 형식이 아니면 400을 응답하고 유스케이스를 호출하지 않는다.")
    void givenInvalidInterestId_whenDelete_thenBadRequest() throws Exception {
        mockMvc.perform(delete("/api/interests/{interestId}", "invalid-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.details.interestId").exists());

        verifyNoInteractions(interestUseCase);
    }
}
