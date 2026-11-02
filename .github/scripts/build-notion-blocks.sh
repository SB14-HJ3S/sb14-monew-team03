#!/bin/bash
set -euo pipefail

DEFAULT_FIELDS='["🗂️ 노션 카테고리","📌 회의 종류","📆 회의 날짜(요일) · 회의 시간","📆 날짜(요일)·시간","🕘 오전/오후","⏰ 회의 시간","😄 참여자 목록","⚠️ 특이사항","📝 김민재","📝 이승현","📝 박수민","📝 정수환","📝 이수찬","📑 기타","📝 댓글 양식 (복사해서 쓰세요)"]'
FIELDS="${NOTION_FIELDS:-$DEFAULT_FIELDS}"
DEFAULT_SKIP='["🗂️ 노션 카테고리","📌 회의 종류","📆 회의 날짜(요일) · 회의 시간","📆 날짜(요일)·시간","🕘 오전/오후","⏰ 회의 시간","😄 참여자 목록","📝 댓글 양식 (복사해서 쓰세요)"]'
SKIP="${NOTION_SKIP:-$DEFAULT_SKIP}"

BODY_INPUT=$(cat)
if PREPARED=$(printf '%s' "$BODY_INPUT" | node "$(dirname "$0")/notion-images.js"); then
  BODY_INPUT=$(jq -r '.body' <<<"$PREPARED")
  IMAGES=$(jq -c '.images' <<<"$PREPARED")
else
  echo "::warning::이미지 변환에 실패해 이미지 없이 노션에 넣습니다." >&2
  IMAGES='[]'
fi

DRAFT=$(printf '%s' "$BODY_INPUT" | jq -Rs \
  --argjson images "$IMAGES" \
  --argjson fields "$FIELDS" \
  --argjson skip "$SKIP" \
  --arg author "$AUTHOR" \
  --arg url "$DISCUSSION_URL" \
  --arg extra "${EXTRA_NOTE:-}" \
  --arg date_label "${DATE_LABEL:-}" \
  '
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
  + ([(if $date_label == "" then empty else "📆 " + $date_label end)]
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
