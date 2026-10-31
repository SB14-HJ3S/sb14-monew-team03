#!/bin/bash

set -euo pipefail

cat | tr -d '\r' | awk '
  /^```/ { in_code = !in_code }
  !in_code && /^### (📝|📑) / && last != "---" { print "---"; print "" }
  { print; if ($0 !~ /^[[:space:]]*$/) { last = $0; sub(/^[[:space:]]+/, "", last); sub(/[[:space:]]+$/, "", last) } }
'
