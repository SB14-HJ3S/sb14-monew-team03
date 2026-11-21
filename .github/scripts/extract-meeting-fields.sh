#!/bin/bash
set -euo pipefail

BODY="$(cat)"
CLEAN="$(printf '%s\n' "$BODY" | tr -d '\r')"

FIELD_HEADINGS='🗂️ 노션 카테고리|📌 회의 종류|📆 회의 날짜(요일) · 회의 시간|📆 날짜(요일)·시간|🕘 오전/오후|⏰ 회의 시간|😄 참여자 목록|⚠️ 특이사항|📝 김민재|📝 이승현|📝 박수민|📝 정수환|📝 이수찬|📑 기타|📝 댓글 양식 (복사해서 쓰세요)'

extract_heading() {
  local heading="$1"
  printf '%s\n' "$CLEAN" | awk -v h="$heading" -v fields="$FIELD_HEADINGS" '
    BEGIN { n = split(fields, list, "|"); for (i = 1; i <= n; i++) is_field[list[i]] = 1 }
    /^#+[[:space:]]*/ {
      title = $0; sub(/^#+[[:space:]]*/, "", title); sub(/[[:space:]]+$/, "", title)
      # "==" 대신 index로 비교: 일부 awk는 로케일 정렬 규칙으로 한글·이모지 문자열을 같다고 판단함
      if (length(title) == length(h) && index(title, h) == 1) { capture = 1; next }
      if (capture && (title in is_field)) { capture = 0; next }
    }
    capture { print }
  '
}

NOTE="$(extract_heading "⚠️ 특이사항" | sed -E '/^[[:space:]]*$/d; /^[[:space:]]*-{3,}[[:space:]]*$/d' | paste -sd' ' -)"
if [[ -z "$NOTE" ]]; then
  NOTE="$(printf '%s\n' "$CLEAN" | { grep -m1 -E '^특이사항[[:space:]]*[:：]' || true; } | sed -E 's/^특이사항[[:space:]]*[:：][[:space:]]*//')"
fi
if [[ "$NOTE" == "_No response_" ]]; then
  NOTE=""
fi

CATEGORY="$(extract_heading "🗂️ 노션 카테고리" | sed '/^[[:space:]]*$/d' | head -1 | sed -E 's/^[[:space:]]+|[[:space:]]+$//g')"
case "$CATEGORY" in
  "[1차] 팀 미팅"|"[2차] 팀 미팅"|"[3차] 팀 미팅"|"[4차] 팀 미팅") ;;
  *) CATEGORY="[미정]" ;;
esac

MEETING_TYPE="$(extract_heading "📌 회의 종류" | sed '/^[[:space:]]*$/d' | head -1 | sed -E 's/^[[:space:]]+|[[:space:]]+$//g')"
if [[ "$MEETING_TYPE" != "데일리 스크럼" && "$MEETING_TYPE" != "정기 회의" && "$MEETING_TYPE" != "비정기 회의" ]]; then
  MEETING_TYPE="데일리 스크럼"
fi

DATE_OVERRIDE="$({ extract_heading "📆 회의 날짜(요일) · 회의 시간"; extract_heading "📆 날짜(요일)·시간"; } | sed '/^[[:space:]]*$/d' | head -1 | sed -E 's/^[[:space:]]+|[[:space:]]+$//g')"
DATE_OVERRIDE="$(printf '%s' "$DATE_OVERRIDE" | sed -E 's/[[:space:]]+[0-9]{1,2}:[0-9]{2}$//; s/[[:space:]]*\([^)]*\)$//')"
if [[ "$DATE_OVERRIDE" =~ ^([0-9]{2})[.-]?([0-9]{2})[.-]?([0-9]{2})$ ]]; then
  DATE_OVERRIDE="20${BASH_REMATCH[1]}-${BASH_REMATCH[2]}-${BASH_REMATCH[3]}"
elif [[ ! "$DATE_OVERRIDE" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  DATE_OVERRIDE=""
fi

PARTICIPANTS_JSON="$(extract_heading "😄 참여자 목록" | sed '/^[[:space:]]*$/d' | head -1 \
  | jq -R -c 'if . == "_No response_" or . == "None" then [] else split(",") | map(gsub("^\\s+|\\s+$"; "")) | map(select(length > 0)) end')"
PARTICIPANTS_JSON="${PARTICIPANTS_JSON:-[]}"

jq -n --arg note "$NOTE" --argjson participants "$PARTICIPANTS_JSON" --arg date_override "$DATE_OVERRIDE" \
  --arg meeting_type "$MEETING_TYPE" --arg category "$CATEGORY" \
  '{note: $note, participants: $participants, date_override: $date_override, meeting_type: $meeting_type, category: $category}'
