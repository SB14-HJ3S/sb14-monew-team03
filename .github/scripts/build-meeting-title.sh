#!/bin/bash
# 회의록 디스커션 제목과 날짜를 만든다
# 입력(환경변수): DATE_OVERRIDE(폼의 날짜, YYYY-MM-DD 또는 빈 값), CREATED_AT(디스커션 생성 시각, UTC ISO), MEETING_TYPE
# 출력: {"title": "[26.10.19(월)] 데일리 스크럼", "date": "2026-10-19", "date_label": "26.10.19(월)"} JSON
# 날짜를 비우면 생성 시각을 한국 시간으로 바꿔서 쓴다 (UTC 그대로 쓰면 오전 9시 전 글이 전날로 찍힘)
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
