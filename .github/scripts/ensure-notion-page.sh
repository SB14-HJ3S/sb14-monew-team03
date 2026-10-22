#!/bin/bash
# 재연동 전에 노션 페이지가 살아 있는지 확인한다
# - 휴지통에 있으면 꺼낸다 (주소 그대로)
# - 완전히 지워졌으면 같은 DB에 새 페이지를 만든다 (속성·본문은 다음 단계에서 채움)
# 입력(환경변수): NOTION_TOKEN, PAGE_ID, PAGE_URL, NOTION_DB_ID, TITLE_PROP(제목 속성 이름), TITLE
# 출력(표준출력, GITHUB_OUTPUT 형식): page_id=…, page_url=…, created=true|false(새로 만들었는지)
set -euo pipefail

notion() {
  local method="$1" path="$2" data="${3:-}"
  local args=(-sS --max-time 15 -w '\n%{http_code}' -X "$method" "https://api.notion.com/v1/$path"
    -H "Authorization: Bearer $NOTION_TOKEN" -H "Notion-Version: 2022-06-28")
  [[ -n "$data" ]] && args+=(-H "Content-Type: application/json" -d "$data")
  curl "${args[@]}"
}
status() { tail -n 1 <<<"$1"; }
body() { sed '$d' <<<"$1"; }

RESPONSE=$(notion GET "pages/$PAGE_ID")
STATUS=$(status "$RESPONSE")

if [[ "$STATUS" == 200 ]]; then
  if [[ "$(body "$RESPONSE" | jq -r '(.archived // false) or (.in_trash // false)')" != "true" ]]; then
    echo "page_id=$PAGE_ID"
    echo "page_url=$PAGE_URL"
    echo "created=false"
    exit 0
  fi
  RESTORED=$(notion PATCH "pages/$PAGE_ID" '{"archived": false}')
  if [[ "$(status "$RESTORED")" == 200 ]]; then
    echo "휴지통에 있던 노션 페이지를 꺼냈습니다." >&2
    echo "page_id=$PAGE_ID"
    echo "page_url=$PAGE_URL"
    echo "created=false"
    exit 0
  fi
  echo "::warning::휴지통의 노션 페이지를 꺼내지 못해 새로 만듭니다: $(body "$RESTORED")" >&2
elif [[ "$STATUS" == 404 ]]; then
  # 노션은 페이지가 없을 때와 연동 봇이 접근 권한을 잃었을 때 둘 다 404를 준다
  # DB에도 접근이 안 되면 권한 문제라서, 새 페이지를 만들지 않고 멈춘다 (재연동마다 중복 페이지가 쌓이지 않게)
  DB_STATUS=$(status "$(notion GET "databases/$NOTION_DB_ID")")
  if [[ "$DB_STATUS" != 200 ]]; then
    echo "::error::노션 페이지와 DB 둘 다 찾을 수 없습니다($DB_STATUS). 노션 DB에 연동 봇 접근 권한이 있는지 확인해 주세요." >&2
    exit 1
  fi
  echo "::warning::노션 페이지가 완전히 지워져서 새로 만듭니다." >&2
else
  echo "::error::노션 페이지 확인 실패($STATUS): $(body "$RESPONSE")" >&2
  exit 1
fi

PAYLOAD=$(jq -n --arg db "$NOTION_DB_ID" --arg prop "$TITLE_PROP" --arg title "$TITLE" \
  '{parent: {database_id: $db}, properties: {($prop): {title: [{text: {content: $title}}]}, "카테고리": {multi_select: [{name: "[미정]"}]}}}')
CREATED=$(notion POST pages "$PAYLOAD")
if [[ "$(status "$CREATED")" != 200 ]]; then
  echo "::error::노션 페이지 새로 만들기 실패: $(body "$CREATED")" >&2
  exit 1
fi
echo "page_id=$(body "$CREATED" | jq -r '.id' | tr -d '-')"
echo "page_url=$(body "$CREATED" | jq -r '.url')"
echo "created=true"
