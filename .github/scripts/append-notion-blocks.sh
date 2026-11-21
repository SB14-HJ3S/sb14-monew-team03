#!/bin/bash
set -euo pipefail

PAGE_ID="$1"
BLOCKS=$(cat)
TOTAL=$(jq 'length' <<<"$BLOCKS")

for ((START = 0; START < TOTAL; START += 100)); do
  PAYLOAD=$(jq -c --argjson start "$START" '{children: .[$start:$start + 100]}' <<<"$BLOCKS")
  if ! RESPONSE=$(curl -sS --max-time 30 -w '\n%{http_code}' \
    -X PATCH "https://api.notion.com/v1/blocks/$PAGE_ID/children" \
    -H "Authorization: Bearer $NOTION_TOKEN" \
    -H "Notion-Version: 2022-06-28" \
    -H "Content-Type: application/json" \
    -d "$PAYLOAD"); then
    echo "::error::노션 본문 쓰기 요청 실패" >&2
    exit 1
  fi
  STATUS=$(tail -n 1 <<<"$RESPONSE")
  if [[ "$STATUS" != 2* ]]; then
    echo "::error::노션 본문 쓰기 실패($STATUS): $(sed '$d' <<<"$RESPONSE")" >&2
    exit 1
  fi
done
