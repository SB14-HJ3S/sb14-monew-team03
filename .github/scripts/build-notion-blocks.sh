#!/bin/bash
# 디스커션 본문(표준입력)을 노션 블록 배열(JSON)로 바꾼다
# - 맨 위: 작성자·디스커션 원문 링크 콜아웃 (EXTRA_NOTE가 있으면 그 아래 한 줄 추가 — 연동 기록 "🔄 최초 연동 … (재연동 N회)")
# - 폼 항목("### 📝 김민재" 등) → 노션 제목3 블록, 내용 → 문단("- " 줄은 글머리 기호)
# - 비워 둔 항목("_No response_")과 노션 속성으로 이미 들어가는 항목(회의 종류·날짜·회의 시간·참여자)은 뺀다
# - 날짜(요일)·시간과 회의 시간은 콜아웃 아래 "📆 261020(화) · ⏰ 오전 11시" 한 줄로 같이 보여 준다
# 입력(환경변수): AUTHOR, DISCUSSION_URL, DATE_LABEL, MEETING_TIME, EXTRA_NOTE(선택), NOTION_FIELDS·NOTION_SKIP(선택)
set -euo pipefail

# 기본값은 회의록 항목. 멘토링처럼 다른 폼은 NOTION_FIELDS·NOTION_SKIP(JSON 배열)으로 바꿔서 쓴다
# extract-meeting-fields.sh의 FIELD_HEADINGS와 같은 목록. 이 제목에서만 항목을 나눈다
DEFAULT_FIELDS='["📌 회의 종류","📆 날짜(요일)·시간","🕘 오전/오후","⏰ 회의 시간","😄 참여자 목록","⚠️ 특이사항","📝 김민재","📝 이승현","📝 박수민","📝 정수환","📝 이수찬","📑 기타"]'
FIELDS="${NOTION_FIELDS:-$DEFAULT_FIELDS}"
DEFAULT_SKIP='["📌 회의 종류","📆 날짜(요일)·시간","🕘 오전/오후","⏰ 회의 시간","😄 참여자 목록"]'
SKIP="${NOTION_SKIP:-$DEFAULT_SKIP}"

jq -Rs \
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
     | (.lines | join("\n") | trim) as $content
     | select($content != "" and $content != "_No response_")
     | [{object: "block", type: "heading_3", heading_3: {rich_text: [{text: {content: .title}}]}}]
       + [$content | split("\n\n")[] | trim | select(. != "")
          | if (split("\n") | all(test("^\\s*[-*] "))) then
              (split("\n")[] | sub("^\\s*[-*] "; "") | text_blocks("bulleted_list_item")[])
            else text_blocks("paragraph")[] end]
    ] | flatten
'
