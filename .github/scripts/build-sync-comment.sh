#!/bin/bash

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
