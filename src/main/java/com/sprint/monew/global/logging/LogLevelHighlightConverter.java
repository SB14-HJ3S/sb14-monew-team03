package com.sprint.monew.global.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.CompositeConverter;
import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiOutput;
import org.springframework.boot.ansi.AnsiStyle;

// 콘솔 로그의 레벨(ERROR·WARN·INFO 등)을 색깔별로 보여 준다 — 배포 로그에는 색이 안 붙음
public class LogLevelHighlightConverter extends CompositeConverter<ILoggingEvent> {

    @Override
    protected String transform(ILoggingEvent event, String in) {
        return switch (event.getLevel().toInt()) {
            case Level.ERROR_INT -> AnsiOutput.toString(AnsiStyle.BOLD, AnsiColor.RED, in);
            case Level.WARN_INT -> AnsiOutput.toString(AnsiStyle.BOLD, AnsiColor.YELLOW, in);
            case Level.INFO_INT -> AnsiOutput.toString(AnsiStyle.BOLD, AnsiColor.GREEN, in);
            case Level.DEBUG_INT -> AnsiOutput.toString(AnsiStyle.BOLD, AnsiColor.BLUE, in);
            default -> AnsiOutput.toString(AnsiColor.BLUE, in);
        };
    }
}
