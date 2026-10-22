package com.sprint.monew.application.interest;

import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;

public interface InterestService {
    Interest create(String name, List<String> keywords);
}
