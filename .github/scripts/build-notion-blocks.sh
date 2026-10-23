#!/bin/bash
# 디스커션 본문(표준입력)을 노션 블록 배열(JSON)로 바꾼다
# - 맨 위: 작성자·디스커션 원문 링크 콜아웃 (EXTRA_NOTE가 있으면 그 아래 한 줄 추가 — 연동 기록 "🔄 최초 연동 … (재연동 N회)")
# - 폼 항목("### 📝 김민재" 등) → 노션 제목3 블록, 내용 → notion-markdown.js로 제목·목록·표·서식 그대로, 붙인 이미지 → 이미지 블록
# - 멤버별·기타 칸(📝·📑) 위에는 노션 구분선을 넣는다(GitHub 본문의 칸 사이 "---"와 같은 모양, #133). 내용 중간의 "---" 문단도 구분선으로
# - 비워 둔 항목("_No response_")과 노션 속성으로 이미 들어가는 항목(회의 종류·날짜·회의 시간·참여자)은 뺀다
# - 날짜(요일)·시간과 회의 시간은 콜아웃 아래 "📆 261020(화) · ⏰ 오전 11시" 한 줄로 같이 보여 준다
# 입력(환경변수): AUTHOR, DISCUSSION_URL, DATE_LABEL, MEETING_TIME, EXTRA_NOTE(선택), NOTION_FIELDS·NOTION_SKIP(선택),
#   NOTION_TOKEN(선택 — 없으면 이미지를 업로드하지 않고 "이미지 원본 보기" 링크로 넣음)
set -euo pipefail

# 기본값은 회의록 항목. 멘토링처럼 다른 폼은 NOTION_FIELDS·NOTION_SKIP(JSON 배열)으로 바꿔서 쓴다
# extract-meeting-fields.sh의 FIELD_HEADINGS와 같은 목록. 이 제목에서만 항목을 나눈다
DEFAULT_FIELDS='["📌 회의 종류","📆 날짜(요일)·시간","🕘 오전/오후","⏰ 회의 시간","😄 참여자 목록","⚠️ 특이사항","📝 김민재","📝 이승현","📝 박수민","📝 정수환","📝 이수찬","📑 기타"]'
FIELDS="${NOTION_FIELDS:-$DEFAULT_FIELDS}"
DEFAULT_SKIP='["📌 회의 종류","📆 날짜(요일)·시간","🕘 오전/오후","⏰ 회의 시간","😄 참여자 목록"]'
SKIP="${NOTION_SKIP:-$DEFAULT_SKIP}"

# 본문의 이미지(스크린샷)는 노션에 업로드해서 이미지 블록으로 넣는다 (#119, notion-images.js)
# 이미지 자리에는 표시 줄([[NOTION_IMAGE:번호]])이 들어가고, 아래 jq에서 그 문단을 이미지 블록으로 바꾼다
# 스크립트가 실패하면 이미지 없이 원래 본문 그대로 변환한다
BODY_INPUT=$(cat)
if PREPARED=$(printf '%s' "$BODY_INPUT" | node "$(dirname "$0")/notion-images.js"); then
  BODY_INPUT=$(jq -r '.body' <<<"$PREPARED")
  IMAGES=$(jq -c '.images' <<<"$PREPARED")
else
  echo "::warning::이미지 변환에 실패해 이미지 없이 노션에 넣습니다." >&2
  IMAGES='[]'
fi

# 칸 내용은 notion-markdown.js가 노션 블록(제목·표·목록·서식 등)으로 바꾼다
# 그래서 jq는 칸 내용을 {"__markdown": 내용, "__fallback": 예전 방식 블록}으로 남겨 두고,
# 변환 스크립트를 못 쓰면(의존성 설치 실패 등) 예전 방식(문단·글머리 목록)으로 넣는다
DRAFT=$(printf '%s' "$BODY_INPUT" | jq -Rs \
  --argjson images "$IMAGES" \
  --argjson fields "$FIELDS" \
  --argjson skip "$SKIP" \
  --arg author "$AUTHOR" \
  --arg url "$DISCUSSION_URL" \
  --arg extra "${EXTRA_NOTE:-}" \
  --arg date_label "${DATE_LABEL:-}" \
  --arg meeting_time "${MEETING_TIME:-}" '
  def chunk(n): if length == 0 then [] else [.[0:n]] + (.[n:] | chunk(n)) end;
  def text_blocks($type): chunk(1900) | map({object: "block", type: $type, ($type): {rich_text: [{text: {content: .}}]}});
  def trim: sub("^\\s+"; "") | sub("\\s+$"; "");
  def divider: {object: "block", type: "divider", divider: {}};
  # 칸 끝에 붙은 구분선(다음 칸 제목 위에 넣은 "---")은 내용에서 뺀다. 구분선은 아래에서 칸 제목 위에 다시 넣음
  def strip_hr: until(test("(^|\\n)[ \\t]*-{3,}[ \\t]*$") | not; sub("(^|\\n)[ \\t]*-{3,}[ \\t]*$"; "") | trim);

  # 본문을 "폼 항목" 단위로 나눈다. 항목 목록에 없는 "###" 제목은 내용으로 취급
  (gsub("\r"; "") | split("\n")
   | reduce .[] as $line ({cur: null, out: []};
       (if ($line | test("^#{1,6}\\s+")) then ($line | sub("^#{1,6}\\s+"; "") | trim) else null end) as $title
       | if $title != null and ($fields | index([$title])) != null then
           (if .cur != null then .out += [.cur] else . end) | .cur = {title: $title, lines: []}
         elif .cur != null then .cur.lines += [$line]
         else . end)
   | (if .cur != null then .out + [.cur] else .out end)) as $sections

  | [{object: "block", type: "callout", callout: {
       icon: {emoji: "💬"}, color: "gray_background",
       rich_text: [{text: {content: ("작성자: " + $author + " · ")}}, {text: {content: "디스커션 원문", link: {url: $url}}}]}}]
  + ([(if $date_label == "" then empty else "📆 " + $date_label end), (if $meeting_time == "" then empty else "⏰ " + $meeting_time end)]
     | if length == 0 then [] else [{object: "block", type: "paragraph", paragraph: {rich_text: [{text: {content: join(" · ")}, annotations: {bold: true}}]}}] end)
  + (if $extra == "" then [] else [{object: "block", type: "paragraph", paragraph: {rich_text: [{text: {content: $extra}}]}}] end)
  + [$sections[]
     | .title as $t | select(($skip | index([$t])) == null)
     | (.lines | join("\n") | trim | strip_hr) as $content
     | select($content != "" and $content != "_No response_")
     | (if ($t | test("^(📝|📑) ")) then [divider] else [] end)
       + [{object: "block", type: "heading_3", heading_3: {rich_text: [{text: {content: .title}}]}}]
       + [{__markdown: $content, __fallback: [$content | split("\n\n")[] | trim | select(. != "")
          | if test("^\\[\\[NOTION_IMAGE:[0-9]+\\]\\]$") then
              (capture("(?<n>[0-9]+)").n | tonumber) as $n | ($images[$n] // empty)
            elif test("^-{3,}$") then divider
            elif (split("\n") | all(test("^\\s*[-*] "))) then
              (split("\n")[] | sub("^\\s*[-*] "; "") | text_blocks("bulleted_list_item")[])
            else text_blocks("paragraph")[] end]}]
    ] | flatten
')

if ! NOTION_IMAGES="$IMAGES" node "$(dirname "$0")/notion-markdown.js" <<<"$DRAFT"; then
  echo "::warning::서식 변환에 실패해 문단·글머리 목록으로만 노션에 넣습니다." >&2
  jq -c '[.[] | if type == "object" and has("__markdown") then .__fallback[] else . end]' <<<"$DRAFT"
fi
