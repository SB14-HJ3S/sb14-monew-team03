package com.sprint.monew.global.web;

import com.sprint.monew.global.pagination.SortOrder;
import java.util.List;
import org.jspecify.annotations.NonNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;

public class StringToSortOrderConverterFactory implements ConverterFactory<String, SortOrder> {

    @NonNull
    @Override
    public <T extends SortOrder> Converter<String, T> getConverter(@NonNull Class<T> targetType) {
        validateInputIsEnum(targetType);

        List<T> constants = List.of(targetType.getEnumConstants());

        return source ->
                constants.stream()
                        .filter(constant -> constant.getValue().equals(source.trim()))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "유효하지 않은 SortOrder 입니다. " + source));
    }

    private <T extends SortOrder> void validateInputIsEnum(Class<T> type) {
        if (!type.isEnum()) {
            throw new IllegalArgumentException("SortOrder의 구현 타입은 Enum이여야 합니다. " + type.getName());
        }
    }
}
