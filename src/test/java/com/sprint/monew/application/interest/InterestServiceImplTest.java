package com.sprint.monew.application.interest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.sprint.monew.application.interest.required.InterestRepository;
import com.sprint.monew.domain.interest.entity.Interest;
import com.sprint.monew.domain.interest.entity.InterestKeyword;
import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.InterestErrorType;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterestServiceImplTest {

    @Mock InterestRepository repository;

    @InjectMocks InterestServiceImpl service;

    @Test
    @DisplayName("Interest 이름의 유사도가 80 미만인 경우 관심사를 등록할 수 있다.")
    void givenNotSimilarName_whenCreate_thenSaveAndReturnInterest() {
        // given
        String notSimilarName = "박수민";
        List<String> keywords = List.of("1", "2", "3");
        Interest creating = Interest.create(notSimilarName, keywords);

        when(repository.findAllNames()).thenReturn(List.of("유영우"));
        when(repository.save(any(Interest.class))).thenReturn(creating);

        // when
        Interest created = service.create(notSimilarName, keywords);

        // then
        assertThat(created).isEqualTo(creating);

        verify(repository).findAllNames();
        verify(repository)
                .save(
                        argThat(
                                interest ->
                                        interest.getName().equals(notSimilarName)
                                                && interest.getKeywords().stream()
                                                        .map(InterestKeyword::getKeyword)
                                                        .toList()
                                                        .containsAll(keywords)));
    }

    @Test
    @DisplayName("Interest 이름의 유사도가 80 이상인 경우 예외를 던진다.")
    void givenSimilarName_whenCreate_thenThrowException() {
        // given
        String similarName = "박수민1";
        List<String> keywords = List.of("1", "2", "3");

        when(repository.findAllNames()).thenReturn(List.of("박수민2"));

        // when
        MonewException thrown =
                catchThrowableOfType(
                        MonewException.class, () -> service.create(similarName, keywords));

        assertThat(thrown.getErrorType()).isEqualTo(InterestErrorType.SIMILAR_NAME_CONFLICT);

        verify(repository).findAllNames();
        verifyNoMoreInteractions(repository);
    }
}
