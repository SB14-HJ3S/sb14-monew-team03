package com.sprint.monew.global.util.similarity;

import java.text.Normalizer;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.text.similarity.LevenshteinDistance;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class LevenshteinDistanceUtils {

    public static double calculateSimilarity(String str1, String str2) {
        String str1Normalized = normalize(str1);
        String str2Normalized = normalize(str2);

        if (str1Normalized.isEmpty() && str2Normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    "비교 문자열이 전부 비어있는 경우 유사도를 계산할 수 없음. " + str1Normalized + str2Normalized);
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
