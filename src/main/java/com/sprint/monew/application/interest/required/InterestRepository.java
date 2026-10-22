package com.sprint.monew.application.interest.required;

import com.sprint.monew.domain.interest.entity.Interest;
import java.util.List;

public interface InterestRepository {
    Interest save(Interest entity);

    List<String> findAllNames();
}
