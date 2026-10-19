#!/bin/bash
# 디스커션 본문(표준입력)에서 "특이사항"/"회의 시간"/"참여자"/"날짜"/"회의 종류"/"회의 시간" 값을 추출해서
# {"note": "...", "ampm": "...", "participants": [...], "date_override": "...", "meeting_type": "..."} JSON으로 출력한다.
# "### 제목" 형식(디스커션 폼이 만드는 형식)을 우선 쓰고, 특이사항은 "특이사항: ..." 콜론 형식도 지원한다.
set -euo pipefail

BODY="$(cat)"
CLEAN="$(printf '%s\n' "$BODY" | tr -d '\r')"

# 템플릿(.github/DISCUSSION_TEMPLATE/b-daily-scrum.yml)의 실제 필드 제목 목록.
# 캡처 중단 기준을 "아무 '#' 제목"이 아니라 "이 목록에 있는 제목"으로만 잡아서,
# 사용자가 답변 안에 "### 다음 단계" 같은 마크다운 제목을 적어도 안 끊기게 함.
FIELD_HEADINGS='📌 회의 종류|📆 날짜(요일)·시간|🕘 오전/오후|⏰ 회의 시간|😄 참여자 목록|⚠️ 특이사항|📝 김민재|📝 이승현|📝 박수민|📝 정수환|📝 이수찬|📑 기타'

extract_heading() {
  local heading="$1"
  # 제목에 "(요일)" 같은 괄호와 이모지가 들어가서 정규식 대신 글자 그대로 비교한다
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

NOTE="$(extract_heading "⚠️ 특이사항" | sed '/^[[:space:]]*$/d' | paste -sd' ' -)"
if [[ -z "$NOTE" ]]; then
  NOTE="$(printf '%s\n' "$CLEAN" | { grep -m1 -E '^특이사항[[:space:]]*[:：]' || true; } | sed -E 's/^특이사항[[:space:]]*[:：][[:space:]]*//')"
fi
# 폼에서 textarea를 비워두면 GitHub이 "_No response_"로 렌더링함 -> 빈 값 취급
if [[ "$NOTE" == "_No response_" ]]; then
  NOTE=""
fi

# 오전/오후는 노션 "오전/오후" 속성에, 회의 시간(선택)은 "오전 11시"처럼 오전/오후 뒤에 붙여 노션 본문 날짜 줄에 넣는다
# - 오전만 고르고 시간을 안 적으면 "오전", 시간을 적으면 "오전 11시"
clean_value() { sed '/^[[:space:]]*$/d' | head -1 | sed -E 's/^[[:space:]]+|[[:space:]]+$//g'; }
AMPM="$(extract_heading "🕘 오전/오후" | clean_value)"
TIME_TEXT="$(extract_heading "⏰ 회의 시간" | clean_value)"
[[ "$AMPM" != "오전" && "$AMPM" != "오후" ]] && AMPM=""
[[ "$TIME_TEXT" == "_No response_" ]] && TIME_TEXT=""
MEETING_TIME="$(printf '%s %s' "$AMPM" "$TIME_TEXT" | sed -E 's/^[[:space:]]+|[[:space:]]+$//g')"

MEETING_TYPE="$(extract_heading "📌 회의 종류" | sed '/^[[:space:]]*$/d' | head -1 | sed -E 's/^[[:space:]]+|[[:space:]]+$//g')"
# 폼의 세 가지 선택지가 아니면(선택 안 함 포함) 기본값 데일리 스크럼
if [[ "$MEETING_TYPE" != "데일리 스크럼" && "$MEETING_TYPE" != "정기 회의" && "$MEETING_TYPE" != "비정기 회의" ]]; then
  MEETING_TYPE="데일리 스크럼"
fi

DATE_OVERRIDE="$(extract_heading "📆 날짜(요일)·시간" | sed '/^[[:space:]]*$/d' | head -1 | sed -E 's/^[[:space:]]+|[[:space:]]+$//g')"
# 봇이 채운 "261019(월) 14:05"처럼 요일·시간이 붙어 있어도 날짜만 읽는다
DATE_OVERRIDE="$(printf '%s' "$DATE_OVERRIDE" | sed -E 's/[[:space:]]+[0-9]{1,2}:[0-9]{2}$//; s/[[:space:]]*\([^)]*\)$//')"
# 261007 / 26-10-07 / 2026-10-07 모두 받아서 2026-10-07 형식으로 맞춘다. 그 외 형식은 무시(올린 날짜 사용)
if [[ "$DATE_OVERRIDE" =~ ^([0-9]{2})-?([0-9]{2})-?([0-9]{2})$ ]]; then
  DATE_OVERRIDE="20${BASH_REMATCH[1]}-${BASH_REMATCH[2]}-${BASH_REMATCH[3]}"
elif [[ ! "$DATE_OVERRIDE" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]]; then
  DATE_OVERRIDE=""
fi

# 참여자: 여러 명 선택 드롭다운이라 "이승현, 김민재"처럼 쉼표로 이어서 들어온다
PARTICIPANTS_JSON="$(extract_heading "😄 참여자 목록" | sed '/^[[:space:]]*$/d' | head -1 \
  | jq -R -c 'if . == "_No response_" or . == "None" then [] else split(",") | map(gsub("^\\s+|\\s+$"; "")) | map(select(length > 0)) end')"
PARTICIPANTS_JSON="${PARTICIPANTS_JSON:-[]}"

jq -n --arg note "$NOTE" --arg ampm "$AMPM" --argjson participants "$PARTICIPANTS_JSON" --arg date_override "$DATE_OVERRIDE" \
  --arg meeting_type "$MEETING_TYPE" --arg meeting_time "$MEETING_TIME" \
  '{note: $note, ampm: $ampm, participants: $participants, date_override: $date_override, meeting_type: $meeting_type, meeting_time: $meeting_time}'
