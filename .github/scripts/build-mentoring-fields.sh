#!/bin/bash
# 멘토링·피드백 디스커션 본문(표준입력)에서 피드백 종류·날짜·시간·참여자를 뽑고 제목을 만든다
# 입력(환경변수): CREATED_AT(디스커션 생성 시각, UTC ISO)
# 출력: {"title": "[261019(월)] 강사님 피드백", "feedback_type": "강사님", "date": "2026-10-19",
#        "notion_date": "2026-10-19T14:00:00+09:00", "date_label": "261019(월) 14:00", "participants": [...], "note": "..."}
# 날짜·시간 칸을 비우면 글을 올린 날짜·시간(한국 시간)을 쓴다
set -euo pipefail

CLEAN="$(cat | tr -d '\r')"
FIELD_HEADINGS='🧑‍🏫 피드백 종류|📆 날짜·시간|😄 질문자 및 참여자|⚠️ 특이사항|📝 내용'

# extract-meeting-fields.sh와 같은 방식: 폼 항목 제목만 경계로 쓰고, 글자 그대로 비교
extract_heading() {
  printf '%s\n' "$CLEAN" | awk -v h="$1" -v fields="$FIELD_HEADINGS" '
    BEGIN { n = split(fields, list, "|"); for (i = 1; i <= n; i++) is_field[list[i]] = 1 }
    /^#+[[:space:]]*/ {
      title = $0; sub(/^#+[[:space:]]*/, "", title); sub(/[[:space:]]+$/, "", title)
      if (length(title) == length(h) && index(title, h) == 1) { capture = 1; next }
      if (capture && (title in is_field)) { capture = 0; next }
    }
    capture { print }
  ' | sed '/^[[:space:]]*$/d' | sed -E 's/^[[:space:]]+|[[:space:]]+$//g'
}

FEEDBACK_TYPE="$(extract_heading "🧑‍🏫 피드백 종류" | head -1)"
if [[ "$FEEDBACK_TYPE" != "강사님" && "$FEEDBACK_TYPE" != "멘토님" && "$FEEDBACK_TYPE" != "기타" ]]; then
  FEEDBACK_TYPE=""
fi

# "261019 14:00" / "26-10-19 14:00" / "2026-10-19 14:00" / 날짜만 → 날짜·시간 분리
RAW="$(extract_heading "📆 날짜·시간" | head -1)"
# 봇이 채운 "261019(월) 14:00"처럼 요일이 붙어 있어도 읽는다
RAW="$(printf '%s' "$RAW" | sed -E 's/\([^)]*\)//')"
DATE=""
TIME=""
if [[ "$RAW" =~ ^([0-9]{2}|[0-9]{4})-?([0-9]{2})-?([0-9]{2})([[:space:]]+([0-9]{1,2}):([0-9]{2}))?$ ]]; then
  YEAR="${BASH_REMATCH[1]}"
  [[ ${#YEAR} -eq 2 ]] && YEAR="20$YEAR"
  DATE="$YEAR-${BASH_REMATCH[2]}-${BASH_REMATCH[3]}"
  if [[ -n "${BASH_REMATCH[4]}" ]] && (( 10#${BASH_REMATCH[5]} <= 23 && 10#${BASH_REMATCH[6]} <= 59 )); then
    TIME="$(printf '%02d:%s' "$((10#${BASH_REMATCH[5]}))" "${BASH_REMATCH[6]}")"
  fi
  # 261399처럼 형식만 맞고 없는 날짜면 아래에서 글 올린 날짜·시간으로 대신한다
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
DAY_LABEL="$(date -d "$DATE" +%y%m%d)($WEEKDAY)"
TITLE="[$DAY_LABEL]"
[[ -n "$FEEDBACK_TYPE" ]] && TITLE="$TITLE $FEEDBACK_TYPE 피드백"
DATE_LABEL="$DAY_LABEL"
NOTION_DATE="$DATE"
if [[ -n "$TIME" ]]; then
  DATE_LABEL="$DAY_LABEL $TIME"
  NOTION_DATE="${DATE}T${TIME}:00+09:00"
fi

# 여러 명 선택 드롭다운은 "이승현, 김민재"처럼 쉼표로 이어서 들어온다
PARTICIPANTS_JSON="$(extract_heading "😄 질문자 및 참여자" | head -1 \
  | jq -R -c 'if . == "_No response_" or . == "None" then [] else split(",") | map(gsub("^\\s+|\\s+$"; "")) | map(select(length > 0)) end')"
PARTICIPANTS_JSON="${PARTICIPANTS_JSON:-[]}"

# 특이사항: 여러 줄이면 한 줄로 합침. 비워 두면 GitHub이 "_No response_"로 넣으므로 빈 값 취급
NOTE="$(extract_heading "⚠️ 특이사항" | paste -sd' ' -)"
[[ "$NOTE" == "_No response_" ]] && NOTE=""

jq -n --arg note "$NOTE" --arg title "$TITLE" --arg feedback_type "$FEEDBACK_TYPE" --arg date "$DATE" \
  --arg notion_date "$NOTION_DATE" --arg date_label "$DATE_LABEL" --argjson participants "$PARTICIPANTS_JSON" \
  '{title: $title, note: $note, feedback_type: $feedback_type, date: $date, notion_date: $notion_date, date_label: $date_label, participants: $participants}'
