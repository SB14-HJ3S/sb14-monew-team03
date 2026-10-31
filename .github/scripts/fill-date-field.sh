#!/bin/bash
set -euo pipefail

BODY="$(cat | tr -d '\r')"
RESULT="$(printf '%s\n' "$BODY" | awk -v label="$DATE_LABEL" -v hs="${DATE_HEADING:-📆 회의 날짜(요일) · 회의 시간|📆 날짜(요일)·시간}" '
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
