#!/bin/bash

set -euo pipefail

if [[ -n "${DATE_OVERRIDE:-}" ]]; then
  DATE="$DATE_OVERRIDE"
else
  DATE="$(TZ=Asia/Seoul date -d "$CREATED_AT" +%Y-%m-%d)"
fi

WEEKDAYS=(월 화 수 목 금 토 일)
WEEKDAY="${WEEKDAYS[$(( $(date -d "$DATE" +%u) - 1 ))]}"
DATE_LABEL="$(date -d "$DATE" +%y.%m.%d)($WEEKDAY)"
TITLE="[$DATE_LABEL]"
if [[ -n "${MEETING_TYPE:-}" ]]; then
  TITLE="$TITLE $MEETING_TYPE"
fi

jq -n --arg title "$TITLE" --arg date "$DATE" --arg date_label "$DATE_LABEL" '{title: $title, date: $date, date_label: $date_label}'
