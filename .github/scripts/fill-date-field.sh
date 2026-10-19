#!/bin/bash
# 디스커션 본문(표준입력)에서 날짜 칸(DATE_HEADING)이 비어 있으면("_No response_") DATE_LABEL로 채운 본문을 출력한다
# - 회의록: "📆 날짜(요일)·시간" → 261019(월) 14:05 / 멘토링: "📆 날짜·시간" → 261019(월) 14:00
# 날짜 칸이 이미 채워져 있으면 아무것도 출력하지 않는다(워크플로가 수정을 건너뜀)
# 입력(환경변수): DATE_LABEL, DATE_HEADING(선택, "|"로 여러 개. 기본 "📆 날짜(요일)·시간")
set -euo pipefail

BODY="$(cat | tr -d '\r')"
RESULT="$(printf '%s\n' "$BODY" | awk -v label="$DATE_LABEL" -v hs="${DATE_HEADING:-📆 날짜(요일)·시간}" '
  BEGIN { n = split(hs, heads, "|") }
  /^#+[[:space:]]*/ {
    title = $0; sub(/^#+[[:space:]]*/, "", title); sub(/[[:space:]]+$/, "", title)
    # "==" 대신 index로 비교: 일부 awk는 로케일 정렬 규칙으로 한글·이모지 문자열을 같다고 판단함
    in_date = 0
    for (i = 1; i <= n; i++) if (length(title) == length(heads[i]) && index(title, heads[i]) == 1) in_date = 1
  }
  in_date && !done && $0 == "_No response_" { print label; done = 1; changed = 1; next }
  { print }
  END { if (!changed) exit 3 }
')" || exit 0
printf '%s' "$RESULT"
