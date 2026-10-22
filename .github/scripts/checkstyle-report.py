# Checkstyle 리포트(main.xml·test.xml)를 읽어 PR 알림 댓글 본문과 위반 개수를 만든다 (#113, #135)
# - pr_count: 이 PR에서 추가·수정한 줄에 걸린 위반 (Checks 탭에 reviewdog가 표시하는 기준과 같음)
# - total: 프로젝트 전체 위반 (PR과 무관하게 이미 쌓여 있던 것 포함)
# - 댓글: 이 PR 위반은 표로, 기존 위반은 접기/펼치기 표로. 위치를 누르면 해당 줄로 이동
# 입력: pr.diff(git diff -U0 결과), 환경변수 GITHUB_WORKSPACE·GITHUB_REPOSITORY·GITHUB_SHA
# 출력: 표준출력 pr_count=…/total=… (GITHUB_OUTPUT용), 파일 checkstyle-comment.md
import os
import re
import xml.etree.ElementTree as ET

REPORTS = ("build/reports/checkstyle/main.xml", "build/reports/checkstyle/test.xml")
MAX_ROWS = 100  # 댓글 길이 제한(65,536자) 안에 들어가게 표마다 최대 줄 수

# 자주 나오는 규칙의 한글 설명. 없는 규칙은 영어 규칙 이름 그대로 보여 준다
RULES = {
    "AvoidStarImport": "`*` import 금지 → 쓰는 클래스만 하나씩 import",
    "MagicNumber": "숫자를 바로 쓰지 말고 상수(static final)로",
    "AbbreviationAsWordInName": "이름에 대문자가 연속으로 너무 많음",
    "LineLength": "한 줄이 100자를 넘음 → 줄 나누기",
    "UnusedImports": "안 쓰는 import 삭제",
    "CustomImportOrder": "import 순서가 다름",
    "EmptyCatchBlock": "빈 catch 블록 → 처리나 로그 추가",
    "NeedBraces": "if·for 등에 중괄호 {} 필요",
    "MissingSwitchDefault": "switch에 default 필요",
    "FallThrough": "case 끝에 break 빠짐",
    "MultipleVariableDeclarations": "변수는 한 줄에 하나씩 선언",
    "VariableDeclarationUsageDistance": "변수를 쓰는 곳 가까이에서 선언",
    "OverloadMethodsDeclarationOrder": "이름이 같은 메서드는 붙여서 두기",
    "OneTopLevelClass": "파일 하나에 최상위 클래스 하나만",
    "EmptyLineSeparator": "선언 사이에 빈 줄 필요",
    "MemberName": "필드 이름 규칙(camelCase) 위반",
    "MethodName": "메서드 이름 규칙(camelCase) 위반",
    "ParameterName": "파라미터 이름 규칙 위반",
    "LocalVariableName": "지역 변수 이름 규칙 위반",
    "TypeName": "클래스 이름 규칙(PascalCase) 위반",
}

# diff에서 파일별로 추가·수정된 줄 번호 모으기
added, current = {}, None
for line in open("pr.diff", encoding="utf-8", errors="ignore"):
    if line.startswith("+++ "):
        path = line[4:].strip()
        current = path[2:] if path.startswith("b/") else None
    elif line.startswith("@@") and current:
        m = re.search(r"\+(\d+)(?:,(\d+))?", line)
        start, length = int(m.group(1)), int(m.group(2) or 1)
        added.setdefault(current, set()).update(range(start, start + length))

workspace = os.environ["GITHUB_WORKSPACE"].rstrip("/") + "/"
mine, old = [], []
for report in REPORTS:
    if not os.path.exists(report):
        continue
    for file in ET.parse(report).getroot().iter("file"):
        path = file.get("name", "").replace(workspace, "", 1)
        for error in file.iter("error"):
            item = {
                "path": path,
                "line": int(error.get("line", 0)),
                "rule": error.get("source", "").split(".")[-1].removesuffix("Check"),
                "message": error.get("message", ""),
            }
            (mine if item["line"] in added.get(path, ()) else old).append(item)

base = f"https://github.com/{os.environ['GITHUB_REPOSITORY']}/blob/{os.environ['GITHUB_SHA']}"


def cell(text):
    return text.replace("|", "\\|").replace("\n", " ")


def table(items):
    rows = ["| 위치 | 규칙 | 내용 |", "|---|---|---|"]
    for v in sorted(items, key=lambda v: (v["path"], v["line"]))[:MAX_ROWS]:
        name = v["path"].rsplit("/", 1)[-1]
        where = f"[{name}:{v['line']}]({base}/{v['path']}#L{v['line']})"
        rows.append(f"| {where} | {cell(RULES.get(v['rule'], v['rule']))} | {cell(v['message'])} |")
    if len(items) > MAX_ROWS:
        rows.append(f"\n외 {len(items) - MAX_ROWS}건은 CI 로그(빌드 및 테스트 단계)에서 `checkstyle`로 검색하세요.")
    return "\n".join(rows)


total = len(mine) + len(old)
parts = [f"⚠️ **이 PR에서 Checkstyle 위반 {len(mine)}건** 있어요 (전체 미해결 {total}건)", "", table(mine)]
if old:
    parts += [
        "",
        f"<details><summary>📋 기존에 쌓여 있던 위반 {len(old)}건 (이 PR과 무관 · 펼쳐 보기)</summary>",
        "",
        table(old),
        "",
        "</details>",
    ]
parts += ["", "<sub>위치를 누르면 해당 줄로 이동해요. 이 PR 위반은 Checks 탭 `checkstyle-main`·`checkstyle-test`에서도 볼 수 있어요.</sub>"]
with open("checkstyle-comment.md", "w", encoding="utf-8") as f:
    f.write("\n".join(parts) + "\n")

print(f"pr_count={len(mine)}")
print(f"total={total}")
