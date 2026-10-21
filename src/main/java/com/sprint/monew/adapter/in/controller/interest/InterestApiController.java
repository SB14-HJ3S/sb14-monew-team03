package com.sprint.monew.adapter.in.controller.interest;

import com.sprint.monew.adapter.in.controller.interest.dto.request.InterestRegisterRequest;
import com.sprint.monew.adapter.in.controller.interest.dto.response.InterestDto;
import com.sprint.monew.application.interest.provided.InterestUseCase;
import com.sprint.monew.global.annotation.RestWebAdapter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;

@RestWebAdapter(path = "/api/interests")
@RequiredArgsConstructor
public class InterestApiController {

    private final InterestUseCase interestUseCase;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public InterestDto registerNewInterest(@RequestBody @Valid InterestRegisterRequest request) {
        return InterestDto.from(interestUseCase.register(request.name(), request.keywords()));
    }
}
