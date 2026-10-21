package com.sprint.monew.global.util.similarity;

import com.sprint.monew.global.exception.MonewException;
import com.sprint.monew.global.exception.errortype.CommonErrorType;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.LevenshteinDistance;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class StringSimilarityUtils {

    public static double calculateSimilarity(String str1, String str2) {
        String str1Normalized = normalize(str1);
        String str2Normalized = normalize(str2);

        if (str1Normalized.isEmpty() && str2Normalized.isEmpty()) {
            Map<String, Object> details =
                    new LinkedHashMap<>() {
                        {
                            put("string1", str1);
                            put("string2", str2);
                        }
                    };
            throw new MonewException(CommonErrorType.INVALID_REQUEST, details);
        }

        Integer distance =
                LevenshteinDistance.getDefaultInstance().apply(str1Normalized, str2Normalized);

        int length = Math.max(str1Normalized.length(), str2Normalized.length());

        return 1.0 - (double) distance / length;
    }

    private static String normalize(String str) {
        if (Objects.isNull(str)) {
            return "";
        }

        return Normalizer.normalize(str.trim().toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("\\s+", "");
    }
}
