"""비공개 저장소에서도 읽을 수 있도록 실제 실행 결과를 SVG로 저장한다."""

import base64
import json
import os
import time
from decimal import Decimal, InvalidOperation, ROUND_DOWN
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen
from xml.sax.saxutils import escape

SUCCESS = "#5E9EAD"
FAILURE = "#E85D9E"
COVERAGE_LOW = FAILURE
UNKNOWN = "#A0A0A0"
LABEL = "#475569"


def badge(label, value, color):
    """지정한 문구와 색상으로 접근성 설명이 포함된 SVG를 만든다."""
    # 기존 배지 비율과 로고를 유지하고 모서리를 살짝 둥글게 처리한다.
    left = 40 if label == "CI" else 72
    label_width = 13 if label == "CI" else 45
    text_width = {"passing": 41, "failing": 34, "cancelled": 55,
                  "skipped": 44, "unknown": 51}.get(value)
    if text_width is None:
        text_width = sum(7 if c.isdigit() else 3 if c == "." else 12 for c in value)
    right = text_width + 12
    width = left + right
    icon_name = "github" if label == "CI" else "codecov"
    icon = (Path(__file__).parent / "badge-icons" / f"{icon_name}.svg").read_bytes()
    icon_data = base64.b64encode(icon).decode("ascii")
    title = escape(f"{label}: {value}")
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="20" role="img" aria-label="{title}">
  <title>{title}</title>
  <defs><clipPath id="badge-round"><rect width="{width}" height="20" rx="7"/></clipPath></defs>
  <g clip-path="url(#badge-round)">
    <rect width="{left}" height="20" fill="{LABEL}"/>
    <rect x="{left}" width="{right}" height="20" fill="{color}"/>
  </g>
  <image x="5" y="3" width="14" height="14" href="data:image/svg+xml;base64,{icon_data}"/>
  <g fill="#fff" text-anchor="middle" font-family="Verdana,Geneva,DejaVu Sans,sans-serif" text-rendering="geometricPrecision" font-size="110">
    <text x="{(left + 19) * 5}" y="140" textLength="{label_width * 10}" transform="scale(.1)">{escape(label)}</text>
    <text x="{(left + right / 2) * 10}" y="140" textLength="{text_width * 10}" transform="scale(.1)">{escape(value)}</text>
  </g>
</svg>
'''


def coverage_value(data):
    """처리 완료된 Codecov 응답에서 0~100 범위의 유효한 커버리지를 읽는다."""
    # 업로드 직후의 처리 중 결과나 다른 커밋의 값은 표시하지 않는다.
    if data.get("state") != "complete":
        return None
    raw = (data.get("totals") or {}).get("coverage")
    if raw is None:
        return None
    try:
        value = Decimal(str(raw))
    except InvalidOperation:
        return None
    return value if value.is_finite() and 0 <= value <= 100 else None


def fetch_coverage(repository, sha, token):
    """해당 커밋의 처리 완료 수치를 재시도하며 조회하고 실패하면 None을 반환한다."""
    if not token:
        print("::warning::CODECOV_API_TOKEN이 없어 커버리지 배지를 unknown으로 표시합니다.")
        return None
    owner, repo = repository.split("/", 1)
    url = f"https://api.codecov.io/api/v2/github/{owner}/repos/{repo}/commits/{sha}/"
    request = Request(url, headers={"Authorization": f"Bearer {token}", "Accept": "application/json"})
    # Codecov는 비동기로 처리하므로 해당 커밋의 완료 결과를 요청 시간 초과를 포함해 최대 약 5분 기다린다.
    for attempt in range(12):
        try:
            with urlopen(request, timeout=15) as response:
                value = coverage_value(json.load(response))
            if value is not None:
                return value
        except HTTPError as error:
            if error.code not in (404, 429, 500, 502, 503, 504):
                print(f"::warning::Codecov 조회 실패(HTTP {error.code}). 토큰 권한을 확인해 주세요.")
                return None
        except (URLError, TimeoutError, ValueError):
            pass
        if attempt < 11:
            time.sleep(10)
    print("::warning::해당 커밋의 Codecov 결과를 확인하지 못해 unknown으로 표시합니다.")
    return None


def write_badges(directory, result, coverage):
    """CI 결과와 커버리지의 상태별 색상을 적용해 두 배지 파일을 저장한다."""
    directory.mkdir(parents=True, exist_ok=True)
    state, color = {
        "success": ("passing", SUCCESS),
        "failure": ("failing", FAILURE),
        "cancelled": ("cancelled", UNKNOWN),
        "skipped": ("skipped", UNKNOWN),
    }.get(result, ("unknown", UNKNOWN))
    (directory / "ci.svg").write_text(badge("CI", state, color))
    value = "unknown" if coverage is None else f"{coverage.quantize(Decimal('0.01'), rounding=ROUND_DOWN)}%"
    color = UNKNOWN if coverage is None else SUCCESS if coverage >= 80 else COVERAGE_LOW
    (directory / "codecov.svg").write_text(badge("codecov", value, color))


if __name__ == "__main__":
    result = os.environ.get("CI_RESULT", "unknown")
    coverage = fetch_coverage(
        os.environ["GITHUB_REPOSITORY"],
        os.environ["COVERAGE_SHA"],
        os.environ.get("CODECOV_API_TOKEN"),
    ) if result == "success" else None
    # CI에서는 배지 전용 브랜치를 체크아웃한 폴더를 넘겨받는다
    write_badges(Path(os.environ.get("BADGE_DIR", ".github/badges")), result, coverage)
