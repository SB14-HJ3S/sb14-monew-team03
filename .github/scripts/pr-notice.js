const fs = require('fs');
const path = require('path');

const MAX_BODY = 65000;

function nowKst() {
  const p = Object.fromEntries(
    new Intl.DateTimeFormat('ko-KR', {
      timeZone: 'Asia/Seoul',
      month: 'numeric',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hourCycle: 'h23',
    }).formatToParts(new Date()).map((x) => [x.type, x.value]),
  );
  return `${p.month}월 ${p.day}일 ${p.hour}시 ${p.minute}분 ${p.second}초`;
}

async function upsertNotice({ github, context }, { marker, legacyMarkers = [], problem, problemBody, okBody, skipSame = false }) {
  const issue_number = context.payload.pull_request.number;
  const comments = await github.paginate(github.rest.issues.listComments, {
    ...context.repo,
    issue_number,
    per_page: 100,
  });
  const markers = [marker, ...legacyMarkers];
  const existing = comments.find((c) => c.body && markers.some((m) => c.body.includes(m)));

  if (!existing && !problem) return;
  if (existing && !problem && existing.body.includes(okBody)) return;

  const prev = existing ? Number((existing.body.match(/재수정 (\d+)회/) || [0, 0])[1]) : 0;
  const footer = existing ? `재수정 ${prev + 1}회, 수정된 시각 ${nowKst()}` : `작성 시각 ${nowKst()}`;
  const tail = ['', `<sub>${footer}</sub>`].join('\n');
  let main = [marker, problem ? problemBody.trim() : okBody].join('\n');
  const cut = '\n\n…(길어서 생략했어요. Checks 탭에서 전부 볼 수 있어요)';
  if (main.length + tail.length > MAX_BODY) {
    main = main.slice(0, MAX_BODY - tail.length - cut.length) + cut;
  }
  if (existing && skipSame && existing.body.startsWith(`${main}\n`)) return;
  const body = `${main}\n${tail}`;

  if (existing) {
    await github.rest.issues.updateComment({ ...context.repo, comment_id: existing.id, body });
  } else {
    await github.rest.issues.createComment({ ...context.repo, issue_number, body });
  }
}

function unescapeXml(s) {
  return s
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#10;/g, '\n')
    .replace(/&#13;/g, '')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, '&');
}

function archViolations(dir) {
  if (!fs.existsSync(dir)) return null;
  const files = fs.readdirSync(dir).filter((f) => f.endsWith('.xml'));
  if (files.length === 0) return null;
  const rules = [];
  for (const file of files) {
    const xml = fs.readFileSync(path.join(dir, file), 'utf8');
    const re = /<testcase\s+name="([^"]*)"[^>]*?(?:\/>|>([\s\S]*?)<\/testcase>)/g;
    let m;
    while ((m = re.exec(xml)) !== null) {
      const failure = m[2] && m[2].match(/<failure[^>]*message="([^"]*)"/);
      if (!failure) continue;
      const message = unescapeXml(failure[1]);
      const because = (message.match(/because (.*?)' was violated/) || [])[1] || '';
      const details = message
        .split('\n')
        .slice(1)
        .map((l) => l.trim())
        .filter(Boolean);
      rules.push({ name: m[1], because, details });
    }
  }
  return rules;
}

async function archNotice({ github, context, core }) {
  const rules = archViolations('build/test-results/archTest');
  if (rules === null) {
    core.warning('패키지 규칙 검사 결과가 없어요. archTest가 돌지 않았을 수 있어요');
    return;
  }
  for (const r of rules) {
    for (const d of r.details.slice(0, 20)) {
      core.warning(d, { title: `패키지 규칙 위반: ${r.because || r.name}` });
    }
  }
  const total = rules.reduce((n, r) => n + r.details.length, 0);
  const problemBody = [
    `### ⚠️ 패키지 규칙 위반 ${total}건 (규칙 ${rules.length}개)`,
    '',
    '빌드·머지는 막지 않아요. 아래 의존을 정리해 주세요',
    '',
    ...rules.flatMap((r) => [
      `**${r.because || r.name}**`,
      ...r.details.slice(0, 10).map((d) => `- ${d}`),
      ...(r.details.length > 10 ? [`- 외 ${r.details.length - 10}건 (Checks 탭 경고에서 전부 볼 수 있어요)`] : []),
      '',
    ]),
    '<sub>규칙 전체: `src/test/java/com/sprint/monew/architecture/ArchitectureTest.java`</sub>',
  ].join('\n');

  await upsertNotice(
    { github, context },
    {
      marker: '<!-- arch-rule-notice -->',
      problem: rules.length > 0,
      problemBody,
      okBody: '### ✅ 패키지 규칙 위반 없어요',
    },
  );
}

const COVERAGE_TARGET = 80;

function changedLines(diff) {
  const files = new Map();
  let current = null;
  for (const line of diff.split('\n')) {
    if (line.startsWith('+++ ')) {
      current = line.startsWith('+++ b/') ? line.slice(6) : null;
      if (current) files.set(current, new Set());
      continue;
    }
    const hunk = current && line.match(/^@@ -\d+(?:,\d+)? \+(\d+)(?:,(\d+))? @@/);
    if (hunk) {
      const start = Number(hunk[1]);
      const count = hunk[2] === undefined ? 1 : Number(hunk[2]);
      for (let n = start; n < start + count; n++) files.get(current).add(n);
    }
  }
  return files;
}

function coverageReport(xml) {
  const percent = (missed, covered) => (missed + covered === 0 ? null : (covered * 100) / (missed + covered));
  const lineCounter = (s) => {
    const m = s.match(/<counter type="LINE" missed="(\d+)" covered="(\d+)"\/>/);
    return m ? percent(Number(m[1]), Number(m[2])) : null;
  };
  const files = new Map();
  const pkgRe = /<package name="([^"]*)">([\s\S]*?)<\/package>/g;
  let p;
  while ((p = pkgRe.exec(xml)) !== null) {
    const srcRe = /<sourcefile name="([^"]*)">([\s\S]*?)<\/sourcefile>/g;
    let s;
    while ((s = srcRe.exec(p[2])) !== null) {
      const uncovered = new Set();
      const lineRe = /<line nr="(\d+)" mi="(\d+)" ci="(\d+)"/g;
      let l;
      while ((l = lineRe.exec(s[2])) !== null) {
        if (Number(l[2]) > 0 && Number(l[3]) === 0) uncovered.add(Number(l[1]));
      }
      files.set(`src/main/java/${p[1]}/${s[1]}`, { percent: lineCounter(s[2]), uncovered });
    }
  }
  const tail = xml.slice(xml.lastIndexOf('</package>'));
  return { total: lineCounter(tail), files };
}

function ranges(numbers) {
  const sorted = [...numbers].sort((a, b) => a - b);
  const out = [];
  for (let i = 0; i < sorted.length; i++) {
    let j = i;
    while (j + 1 < sorted.length && sorted[j + 1] === sorted[j] + 1) j++;
    out.push(i === j ? `${sorted[i]}` : `${sorted[i]}~${sorted[j]}`);
    i = j;
  }
  return out;
}

async function coverageNotice({ github, context, core }) {
  const reportPath = 'build/reports/jacoco/test/jacocoTestReport.xml';
  if (!fs.existsSync(reportPath)) {
    core.info('커버리지 결과 파일이 없어 안내를 건너뛰어요');
    return;
  }
  const { execSync } = require('child_process');
  const git = (args) =>
    execSync(`git -c core.quotePath=false ${args}`, { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 });
  const range = `"origin/${process.env.BASE_REF}...HEAD"`;
  const changed = changedLines(git(`diff -U0 --no-color ${range} -- src/main/java`));
  const testChanged = git(`diff --name-only ${range} -- src/test/java`).trim() !== '';
  const report = coverageReport(fs.readFileSync(reportPath, 'utf8'));

  const rows = [];
  let allTested = true;
  for (const [file, lines] of changed) {
    const info = report.files.get(file);
    if (!info || lines.size === 0) continue;
    const missed = ranges([...lines].filter((n) => info.uncovered.has(n)));
    if (missed.length > 0) allTested = false;
    const shown = missed.length > 15 ? [...missed.slice(0, 15), `외 ${missed.length - 15}곳`] : missed;
    const pct = info.percent === null ? '-' : `${info.percent.toFixed(1)}%`;
    rows.push(`| \`${path.basename(file)}\` | ${pct} | ${shown.length ? shown.join(', ') : '✅ 없음'} |`);
  }
  if (rows.length === 0 && !testChanged) {
    core.info('본 코드와 테스트 코드가 바뀌지 않아 안내를 건너뛰어요');
    return;
  }

  const total = report.total === null ? '-' : `${report.total.toFixed(1)}%`;
  const mark = report.total !== null && report.total >= COVERAGE_TARGET ? '✅' : '📊';
  const heading = `### ${mark} 테스트 커버리지 ${total} (목표 ${COVERAGE_TARGET}%)`;
  const body =
    rows.length === 0
      ? [heading, '', '이번 PR에서 바뀐 본 코드가 없어 전체 커버리지만 보여 줘요'].join('\n')
      : [
          heading,
          '',
          ...(allTested ? ['✅ 이번에 바꾼 줄은 모두 테스트됐어요', ''] : []),
          '이번 PR에서 바뀐 파일',
          '',
          '| 파일 | 커버리지 | 테스트 안 된 줄 (이번에 바꾼 줄 중) |',
          '|---|---|---|',
          ...rows,
        ].join('\n');

  await upsertNotice(
    { github, context },
    { marker: '<!-- coverage-notice -->', problem: true, problemBody: body, okBody: '', skipSame: true },
  );
}

module.exports = {
  upsertNotice,
  archNotice,
  coverageNotice,
  _test: { archViolations, unescapeXml, changedLines, coverageReport, ranges },
};
