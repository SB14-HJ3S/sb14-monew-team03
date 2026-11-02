package com.sprint.monew.application.log.required;

import java.nio.file.Path;

public interface LogStoragePort {

    /** 저장소에서 업로드 성공이 확인되면 true, 예상 가능한 업로드 실패 시 false를 반환한다. */
    boolean upload(Path file);
}
