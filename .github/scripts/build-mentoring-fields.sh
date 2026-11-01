#!/bin/bash
set -euo pipefail

CLEAN="$(cat | tr -d '\r')"
FIELD_HEADINGS='🗂️ 노션 카테고리|🧑‍🏫 피드백 종류|📆 날짜·시간|😄 질문자 및 참여자|⚠️ 특이사항|📝 내용'

extract_heading() {
  printf '%s\n' "$CLEAN" | awk -v h="$1" -v fields="$FIELD_HEADINGS" '
    BEGIN { n = split(fields, list, "|"); for (i = 1; i <= n; i++) is_field[list[i]] = 1 }
    /^#+[[:space:]]*/ {
      title = $0; sub(/^#+[[:space:]]*/, "", title); sub(/[[:space:]]+$/, "", title)
      if (length(title) == length(h) && index(title, h) == 1) { capture = 1; next }
      if (capture && (title in is_field)) { capture = 0; next }
    }
    capture { print }
  ' | sed -E '/^[[:space:]]*$/d; /^[[:space:]]*-{3,}[[:space:]]*$/d; s/^[[:space:]]+|[[:space:]]+$//g'
}

FEEDBACK_TYPE="$(extract_heading "🧑‍🏫 피드백 종류" | head -1)"
if [[ "$FEEDBACK_TYPE" != "강사님" && "$FEEDBACK_TYPE" != "멘토님" && "$FEEDBACK_TYPE" != "기타" ]]; then
  FEEDBACK_TYPE=""
fi

RAW="$(extract_heading "📆 날짜·시간" | head -1)"
RAW="$(printf '%s' "$RAW" | sed -E 's/\([^)]*\)//')"
DATE=""
TIME=""
if [[ "$RAW" =~ ^([0-9]{2}|[0-9]{4})[.-]?([0-9]{2})[.-]?([0-9]{2})([[:space:]]+([0-9]{1,2}):([0-9]{2}))?$ ]]; then
  YEAR="${BASH_REMATCH[1]}"
  [[ ${#YEAR} -eq 2 ]] && YEAR="20$YEAR"
  DATE="$YEAR-${BASH_REMATCH[2]}-${BASH_REMATCH[3]}"
  if [[ -n "${BASH_REMATCH[4]}" ]] && (( 10#${BASH_REMATCH[5]} <= 23 && 10#${BASH_REMATCH[6]} <= 59 )); then
    TIME="$(printf '%02d:%s' "$((10#${BASH_REMATCH[5]}))" "${BASH_REMATCH[6]}")"
  fi
  if [[ "$(date -d "$DATE" +%Y-%m-%d 2>/dev/null)" != "$DATE" ]]; then
    DATE=""
    TIME=""
  fi
fi
if [[ -z "$DATE" ]]; then
  DATE="$(TZ=Asia/Seoul date -d "$CREATED_AT" +%Y-%m-%d)"
  TIME="$(TZ=Asia/Seoul date -d "$CREATED_AT" +%H:%M)"
fi

WEEKDAYS=(월 화 수 목 금 토 일)
WEEKDAY="${WEEKDAYS[$(( $(date -d "$DATE" +%u) - 1 ))]}"
DAY_LABEL="$(date -d "$DATE" +%y.%m.%d)($WEEKDAY)"
TITLE="[$DAY_LABEL]"
[[ -n "$FEEDBACK_TYPE" ]] && TITLE="$TITLE $FEEDBACK_TYPE 피드백"
DATE_LABEL="$DAY_LABEL"
NOTION_DATE="$DATE"
if [[ -n "$TIME" ]]; then
  DATE_LABEL="$DAY_LABEL $TIME"
  NOTION_DATE="${DATE}T${TIME}:00+09:00"
fi

PARTICIPANTS_JSON="$(extract_heading "😄 질문자 및 참여자" | head -1 \
  | jq -R -c 'if . == "_No response_" or . == "None" then [] else split(",") | map(gsub("^\\s+|\\s+$"; "")) | map(select(length > 0)) end')"
PARTICIPANTS_JSON="${PARTICIPANTS_JSON:-[]}"

NOTE="$(extract_heading "⚠️ 특이사항" | paste -sd' ' -)"
[[ "$NOTE" == "_No response_" ]] && NOTE=""

CATEGORY="$(extract_heading "🗂️ 노션 카테고리" | head -1)"
case "$CATEGORY" in
  "[1차] 멘토링"|"[2차] 멘토링"|"[3차] 멘토링"|"[4차] 멘토링") ;;
  *) CATEGORY="[미정]" ;;
esac

jq -n --arg note "$NOTE" --arg category "$CATEGORY" --arg title "$TITLE" --arg feedback_type "$FEEDBACK_TYPE" --arg date "$DATE" \
  --arg notion_date "$NOTION_DATE" --arg date_label "$DATE_LABEL" --argjson participants "$PARTICIPANTS_JSON" \
  '{title: $title, note: $note, category: $category, feedback_type: $feedback_type, date: $date, notion_date: $notion_date, date_label: $date_label, participants: $participants}'
