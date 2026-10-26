package com.sprint.monew.adapter.in.controller.interest;

import com.sprint.monew.adapter.in.controller.interest.dto.request.InterestRegisterRequest;
import com.sprint.monew.adapter.in.controller.interest.dto.request.InterestUpdateRequest;
import com.sprint.monew.adapter.in.controller.interest.dto.response.InterestDto;
import com.sprint.monew.application.interest.provided.InterestUseCase;
import com.sprint.monew.application.interest.provided.dto.InterestQuery;
import com.sprint.monew.global.annotation.RequestId;
import com.sprint.monew.global.annotation.RestWebAdapter;
import com.sprint.monew.global.pagination.PageResult;
import com.sprint.monew.global.pagination.SortDirection;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestWebAdapter(path = "/api/interests")
@RequiredArgsConstructor
public class InterestController {
    private final InterestUseCase interestUseCase;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public InterestDto registerNewInterest(@RequestBody @Valid InterestRegisterRequest request) {
        return InterestDto.from(interestUseCase.register(request.name(), request.keywords()));
    }

    @RequestId
    @ResponseStatus(HttpStatus.OK)
    @GetMapping
    public PageResult<InterestDto> findInterestPagesByQuery(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "orderBy", required = true) InterestQuery.SortOrder sortOrder,
            @RequestParam(name = "direction", required = true) SortDirection sortDirection,
            @RequestParam(name = "cursor", required = false) String cursor,
            @RequestParam(name = "after", required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    Instant after,
            @RequestParam(name = "limit", required = true) int limit) {
        InterestQuery query =
                InterestQuery.of(keyword, sortOrder, sortDirection, cursor, after, limit);

        return interestUseCase.findByQuery(query).map(InterestDto::from);
    }

    @ResponseStatus(HttpStatus.OK)
    @PatchMapping("/{interestId}")
    public InterestDto updateKeyword(
            @PathVariable UUID interestId, @Valid @RequestBody InterestUpdateRequest request) {
        return InterestDto.from(interestUseCase.update(interestId, request.keywords()));
    }
}
