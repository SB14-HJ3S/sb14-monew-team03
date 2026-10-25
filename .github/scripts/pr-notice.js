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

async function upsertNotice({ github, context }, { marker, legacyMarkers = [], problem, problemBody, okBody }) {
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

module.exports = { upsertNotice, archNotice, _test: { archViolations, unescapeXml } };
