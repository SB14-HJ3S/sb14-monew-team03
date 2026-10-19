#!/bin/bash
# 노션 연동 기록(최초 연동·마지막 반영·재연동 횟수)을 계산하고, GitHub 연동 댓글이나 노션 본문 한 줄로 만든다
# 처음 연동하면 새 댓글, 재연동하면 기존 댓글을 고친다. 노션 카드 본문 맨 위에도 같은 값을 한 줄로 넣는다
#   댓글:  🔗 노션 동기화됨: <노션 주소>
#          - 처음 연동: 2026-10-19 23:32
#          - 마지막 반영: 2026-10-20 10:05 (재연동 2회)      ← 재연동했을 때만
#          (빈 줄)
#          수정 후 다시 반영하려면 이 디스커션에 `resync:재연동` 라벨을 붙여주세요.
#   노션:  🔄 최초 연동: 2026-10-19 23:32 · 마지막 반영: 2026-10-20 10:05 (재연동 2회)
# 입력(환경변수)
#   MODE: comment(기본) / meta(계산 결과를 first=·now=·count= 줄로 출력, GITHUB_OUTPUT용) / line(노션 본문 한 줄)
#   OLD_BODY, OLD_CREATED_AT: 재연동일 때 기존 연동 댓글 본문·작성 시각(UTC ISO). 없으면 처음 연동
#   FIRST, NOW, COUNT: meta로 미리 계산한 값을 넘기면 그대로 쓴다(댓글과 노션 값을 맞추기 위함)
#   PAGE_URL, TARGET(디스커션/이슈/PR): comment에서만 사용
# 시각은 한국 시간. 기존 댓글에 "처음 연동" 줄이 없으면 댓글 작성 시각을 처음 연동 시각으로 쓴다
set -euo pipefail

MODE="${MODE:-comment}"
OLD_BODY="${OLD_BODY:-}"
NOW="${NOW:-$(TZ=Asia/Seoul date '+%Y-%m-%d %H:%M')}"

if [[ -z "${COUNT:-}" ]]; then
  if [[ -z "$OLD_BODY" ]]; then
    FIRST="${FIRST:-$NOW}"
    COUNT=0
  else
    if [[ -z "${FIRST:-}" ]]; then
      FIRST="$(printf '%s\n' "$OLD_BODY" | sed -nE 's/^- 처음 연동: (.+)$/\1/p' | head -1)"
    fi
    if [[ -z "$FIRST" && -n "${OLD_CREATED_AT:-}" ]]; then
      FIRST="$(TZ=Asia/Seoul date -d "$OLD_CREATED_AT" '+%Y-%m-%d %H:%M')"
    fi
    FIRST="${FIRST:-$NOW}"
    COUNT="$(printf '%s\n' "$OLD_BODY" | sed -nE 's/.*\(재연동 ([0-9]+)회\).*/\1/p' | head -1)"
    COUNT=$(( ${COUNT:-0} + 1 ))
  fi
fi
FIRST="${FIRST:-$NOW}"

case "$MODE" in
  meta)
    printf 'first=%s\nnow=%s\ncount=%s\n' "$FIRST" "$NOW" "$COUNT"
    ;;
  line)
    if [[ "$COUNT" -eq 0 ]]; then
      printf '🔄 최초 연동: %s' "$FIRST"
    else
      printf '🔄 최초 연동: %s · 마지막 반영: %s (재연동 %d회)' "$FIRST" "$NOW" "$COUNT"
    fi
    ;;
  *)
    if [[ "$COUNT" -eq 0 ]]; then
      printf '🔗 노션 동기화됨: %s\n- 처음 연동: %s\n\n수정 후 다시 반영하려면 이 %s에 `resync:재연동` 라벨을 붙여주세요.' "$PAGE_URL" "$FIRST" "$TARGET"
    else
      printf '🔗 노션 동기화됨: %s\n- 처음 연동: %s\n- 마지막 반영: %s (재연동 %d회)\n\n수정 후 다시 반영하려면 이 %s에 `resync:재연동` 라벨을 붙여주세요.' \
        "$PAGE_URL" "$FIRST" "$NOW" "$COUNT" "$TARGET"
    fi
    ;;
esac
