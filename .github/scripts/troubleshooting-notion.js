// fix:수정 이슈·PR이 해결되면(이슈 닫힘 / PR 머지) 노션 "트래블 슈팅" DB에 카드를 만든다
// - 이슈와 그 이슈를 고친 PR은 카드 하나로 묶는다. 카드 키는 이슈 URL(이슈 없이 머지된 PR이면 PR URL)
// - 노션 "GitHub 링크"(URL) 속성으로 이미 있는 카드인지 확인한다
//   - 해결 이벤트: 없으면 새로 만들고, 있으면 날짜·작성자만 갱신
//   - resync:재연동 라벨: GitHub 내용 기준으로 카드 본문을 통째로 다시 쓴다
// - 양식의 글 부분(환경·관련 기술·증상·재현 방법·원인 분석·해결 방법·배운 점·전체 요약)은 GitHub 연동이 이슈·PR·코드 변경을 읽고 쓴다(ts-written.json)
//   발생일·해결 소요 시간·에러 로그 원문·참고 링크 같은 사실 정보는 코드가 채운다. 작성에 실패하면 이슈·PR 원문으로 채운다
// 실행 순서: prepare(데이터 모으기, 작성용 자료 만들기) → 글 작성 → write(노션에 쓰기)

const fs = require('fs');
const { extractImages, stripImages, imageBlocks } = require('./notion-images.js');

const NOTION_DB_ID = 'fd09e310-9743-8397-95d6-0177da00be8c';
const LINK_PROPERTY = 'GitHub 링크';
const TEXT_LIMIT = 1900;
const CARDS_FILE = 'ts-cards.json';
const CONTEXT_FILE = 'ts-context.md';
const WRITTEN_FILE = 'ts-written.json';
const DIFF_LIMIT = 12000;

// 이슈 템플릿(🛠️-fix.md)에 미리 적힌 안내 문구. 작성자가 안 지우고 둔 경우 내용에서 뺀다
const PLACEHOLDERS = [
  '어떤 문제가 발생했는지 한 문장으로 작성해주세요.',
  '정상적으로 동작한다면 어떤 결과가 나와야 하는지 작성해주세요.',
  '실제로 어떤 결과가 나왔는지 작성해주세요.',
  '에러 로그를 붙여넣어 주세요.',
];

function kstDate(iso) {
  return new Date(new Date(iso).getTime() + 9 * 3600 * 1000).toISOString().slice(0, 10);
}

function duration(fromIso, toIso) {
  const minutes = Math.max(0, Math.round((new Date(toIso) - new Date(fromIso)) / 60000));
  const days = Math.floor(minutes / 1440);
  const hours = Math.floor((minutes % 1440) / 60);
  if (days > 0) return `약 ${days}일${hours > 0 ? ` ${hours}시간` : ''}`;
  if (hours > 0) return `약 ${hours}시간`;
  return `약 ${minutes}분`;
}

// 마크다운 본문을 "## 제목" 단위로 나눈다. 제목의 이모지·괄호 설명은 빼고 비교한다 (예: "📝 작업내용 (설명)" → "작업내용")
function sections(body) {
  const result = {};
  let current = null;
  const clean = (body || '').replace(/\r/g, '').replace(/<!--[\s\S]*?-->/g, '');
  for (const line of clean.split('\n')) {
    const heading = line.match(/^#{1,6}\s+(.*)$/);
    if (heading) {
      current = heading[1].replace(/\(.*?\)/g, '').replace(/[^\p{L}\p{N}\s/·]/gu, '').trim();
      result[current] = [];
    } else if (current) {
      result[current].push(line);
    }
  }
  for (const key of Object.keys(result)) {
    let text = result[key].join('\n');
    for (const placeholder of PLACEHOLDERS) text = text.split(placeholder).join('');
    text = text.replace(/^\s*-\s*$/gm, '').replace(/^\s*\d+\.\s*$/gm, '').trim();
    result[key] = text === '_No response_' ? '' : text;
  }
  return result;
}

// 제목 앞의 "[Fix] :", "🛠️ [Fix] :", "[Chore] :" 같은 타입 표시는 노션 카드 이름·요약에서 뺀다
const cleanTitle = (title) => title.replace(/^[^\[\p{L}\p{N}]*\[[^\]]+\]\s*:\s*/u, '');

// 한 줄 요약에 넣을 때는 글머리 기호("- ")를 떼고 줄을 이어 붙인다
const inline = (text) => (text || '').replace(/^\s*[-*]\s+/gm, '').replace(/\n+/g, ' ').trim();

const richText = (content, extra = {}) =>
  [{ type: 'text', text: { content: String(content).slice(0, TEXT_LIMIT), ...(extra.link ? { link: { url: extra.link } } : {}) }, ...(extra.bold ? { annotations: { bold: true } } : {}) }];

// 글 속 마크다운 표시를 노션 서식으로 바꾼다: `코드` → 코드 서식, **굵게** → 굵은 글씨 (그 외 글자는 그대로)
// 링크가 있는 글(참고 자료)은 통째로 링크 하나로 둔다
const INLINE_MD = /(`[^`\n]+`|\*\*[^*\n]+\*\*)/g;
function richMarkdown(content, extra = {}) {
  if (extra.link) return richText(content, extra);
  const text = String(content).slice(0, TEXT_LIMIT);
  const parts = [];
  let last = 0;
  let match;
  const push = (t, annotations) => {
    if (!t) return;
    const merged = { ...(extra.bold ? { bold: true } : {}), ...annotations };
    parts.push({ type: 'text', text: { content: t }, ...(Object.keys(merged).length ? { annotations: merged } : {}) });
  };
  while ((match = INLINE_MD.exec(text))) {
    push(text.slice(last, match.index));
    const token = match[0];
    if (token.startsWith('`')) push(token.slice(1, -1), { code: true });
    else push(token.slice(2, -2), { bold: true });
    last = INLINE_MD.lastIndex;
  }
  push(text.slice(last));
  // 노션은 블록 하나에 rich_text 100개까지만 받는다. 넘치면 서식 없이 원문으로 넣는다
  if (parts.length === 0) return richText(content, extra);
  return parts.length <= 100 ? parts : richText(content, extra);
}

const block = (type, content, extra) => ({ object: 'block', type, [type]: { rich_text: richMarkdown(content, extra) } });
const heading = (level, content) => block(`heading_${level}`, content);
const divider = () => ({ object: 'block', type: 'divider', divider: {} });
const code = (content) => ({ object: 'block', type: 'code', code: { language: 'plain text', rich_text: richText(content) } });

function chunks(text) {
  const out = [];
  for (let i = 0; i < text.length; i += TEXT_LIMIT) out.push(text.slice(i, i + TEXT_LIMIT));
  return out.length ? out : [''];
}

// 마크다운 → 노션 블록 (문단, 글머리 기호, 번호 목록, 코드 블록 정도만)
// 이미지 태그는 글자로 보이지 않게 지우고, 이미지는 buildBlocks가 "스크린샷" 칸에 따로 넣는다
function markdownBlocks(rawText) {
  const text = stripImages(rawText).trim();
  if (!text) return [block('paragraph', '-')];
  const blocks = [];
  const fence = /```[^\n]*\n([\s\S]*?)```/g;
  let last = 0;
  let match;
  const plain = (part) => {
    for (const para of part.split(/\n\s*\n/).map((p) => p.trim()).filter(Boolean)) {
      const lines = para.split('\n');
      if (lines.every((l) => /^\s*[-*]\s+/.test(l))) {
        lines.forEach((l) => blocks.push(block('bulleted_list_item', l.replace(/^\s*[-*]\s+/, ''))));
      } else if (lines.every((l) => /^\s*\d+\.\s+/.test(l))) {
        lines.forEach((l) => blocks.push(block('numbered_list_item', l.replace(/^\s*\d+\.\s+/, ''))));
      } else {
        chunks(para).forEach((c) => blocks.push(block('paragraph', c)));
      }
    }
  };
  while ((match = fence.exec(text))) {
    plain(text.slice(last, match.index));
    const body = match[1].trim();
    if (body) chunks(body).forEach((c) => blocks.push(code(c)));
    last = fence.lastIndex;
  }
  plain(text.slice(last));
  return blocks.length ? blocks : [block('paragraph', '-')];
}

function overviewTable(rows) {
  // 값이 배열이면 이미 만든 rich_text(예: 사람 멘션)라 그대로 쓴다
  const cell = (t, bold) => (Array.isArray(t) ? t : richMarkdown(t, { bold }));
  return {
    object: 'block',
    type: 'table',
    table: {
      table_width: 2,
      has_column_header: true,
      has_row_header: false,
      children: [['항목', '내용'], ...rows].map(([k, v], i) => ({
        object: 'block',
        type: 'table_row',
        table_row: { cells: [cell(k, i > 0), cell(v, false)] },
      })),
    },
  };
}

// 해결한 사람 = 머지된 PR을 올린 사람(여러 명이면 쉼표로). 노션 대응표에 있으면 사람 멘션(@이름), 없으면 GitHub 아이디
function solvers(prs, userMap) {
  const logins = [...new Set(prs.map((pr) => pr.author?.login).filter(Boolean))];
  if (logins.length === 0) return '-';
  return logins.flatMap((login, i) => [
    ...(i > 0 ? [{ type: 'text', text: { content: ', ' } }] : []),
    userMap[login] ? { type: 'mention', mention: { user: { id: userMap[login] } } } : { type: 'text', text: { content: `@${login}` } },
  ]);
}

// written: 연동이 쓴 내용(없으면 null → 이슈·PR 원문으로 채움)
//   { environment, tech, symptom, reproduce, attempts: [..], root_cause, solution, lessons: [..], summary }
// images: 이슈·PR 본문에 붙인 이미지를 노션 블록으로 바꾼 것 { issue: [..], prs: [..] } (#119)
// userMap: GitHub 아이디 → 노션 사용자 ID. "해결한 사람"을 노션 사람 멘션으로 보여 줄 때 쓴다
function buildBlocks({ issue, prs, closedAt }, written, historyLine = '', images = {}, userMap = {}) {
  const screenshots = (title, list) => (list?.length ? [heading(3, title), ...list] : []);
  const base = issue || prs[0];
  const s = issue ? sections(issue.body) : {};
  const summary = inline(s['버그 요약']) || cleanTitle(base.title);
  const rawSymptom = [
    s['버그 요약'],
    s['기대 결과'] && `기대 결과: ${inline(s['기대 결과'])}`,
    s['실제 결과'] && `실제 결과:\n${s['실제 결과']}`,
  ].filter(Boolean).join('\n\n');
  // 작성에 실패한 칸은 "-"로 둔다(실패 내용은 Actions 로그 경고로 남음). 이슈에 resync:재연동 라벨을 붙이면 다시 시도
  const missing = '-';
  const list = (items, type) => (Array.isArray(items) && items.length ? items.map((t) => block(type, t)) : [block('paragraph', missing)]);

  const rawSolution = prs.length
    ? prs.flatMap((pr) => {
        const p = sections(pr.body);
        return [
          heading(3, `PR #${pr.number} ${pr.title}`),
          ...markdownBlocks([p['작업내용'], p['주요 변경 내용']].filter(Boolean).join('\n\n')),
        ];
      })
    : [block('paragraph', '(연결된 PR 없음)')];

  return [
    heading(1, '🔧 트러블슈팅 기록'),
    ...(historyLine ? [block('paragraph', historyLine)] : []),
    heading(2, '개요'),
    overviewTable([
      ['발생일', kstDate(base.createdAt)],
      ['환경', written?.environment || '-'],
      ['관련 기술', written?.tech || '-'],
      ['해결한 사람', solvers(prs, userMap)],
      ['해결 소요 시간', duration(base.createdAt, closedAt)],
    ]),
    divider(),
    heading(2, '문제 상황'),
    heading(3, '증상'),
    ...markdownBlocks(written?.symptom || rawSymptom),
    ...screenshots('스크린샷', images.issue),
    heading(3, '재현 방법'),
    ...markdownBlocks(written?.reproduce || s['재현 방법']),
    heading(3, '에러 메시지 / 로그'),
    // 에러 로그는 검색할 때 원문이 필요해서 다시 쓰지 않고 이슈에 적힌 그대로 넣는다
    ...(s['에러 로그'] ? markdownBlocks(s['에러 로그'].includes('```') ? s['에러 로그'] : '```\n' + s['에러 로그'] + '\n```') : [code('-')]),
    divider(),
    heading(2, '원인 분석'),
    heading(3, '시도한 것들'),
    ...list(written?.attempts, 'numbered_list_item'),
    heading(3, '근본 원인'),
    ...markdownBlocks(written?.root_cause || missing),
    divider(),
    heading(2, '해결 방법'),
    ...(written?.solution ? markdownBlocks(written.solution) : rawSolution),
    ...screenshots('스크린샷 (PR)', images.prs),
    divider(),
    heading(2, '배운 점 및 개선 사항'),
    ...list(written?.lessons, 'bulleted_list_item'),
    divider(),
    heading(2, '전체 요약'),
    ...(written?.summary ? markdownBlocks(written.summary) : []),
    block('bulleted_list_item', `문제: ${summary}`),
    ...prs.map((pr) => block('bulleted_list_item', `해결: PR #${pr.number} ${pr.title}`)),
    block('bulleted_list_item', `해결 소요 시간: ${duration(base.createdAt, closedAt)}`),
    divider(),
    heading(2, '참고 자료'),
    ...(issue ? [block('bulleted_list_item', `이슈 #${issue.number} ${issue.title}`, { link: issue.url })] : []),
    ...prs.map((pr) => block('bulleted_list_item', `PR #${pr.number} ${pr.title}`, { link: pr.url })),
  ];
}

function notionClient(token) {
  return async (method, path, body) => {
    const res = await fetch(`https://api.notion.com/v1${path}`, {
      method,
      headers: { Authorization: `Bearer ${token}`, 'Notion-Version': '2022-06-28', 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined,
    });
    const json = await res.json();
    if (!res.ok) throw new Error(`노션 ${method} ${path} 실패(${res.status}): ${JSON.stringify(json)}`);
    return json;
  };
}

const PR_FIELDS = 'number title body url createdAt mergedAt merged author { login }';

// 이슈 하나와 그 이슈를 고친(머지된) PR들을 카드 하나로 읽어 온다
async function loadIssueCard(github, owner, repo, number) {
  const q = await github.graphql(
    `query($o: String!, $r: String!, $n: Int!) { repository(owner: $o, name: $r) { issue(number: $n) {
      number title body url createdAt closedAt author { login }
      closedByPullRequestsReferences(first: 10, includeClosedPrs: true) { nodes { ${PR_FIELDS} } }
    } } }`,
    { o: owner, r: repo, n: number },
  );
  const issue = q.repository.issue;
  return { issue, prs: issue.closedByPullRequestsReferences.nodes.filter((pr) => pr.merged) };
}

// PR 하나를 카드로 바꾼다: 연결된 이슈(close #번호)가 있으면 이슈마다 카드, 없으면 PR 단독 카드
async function loadPrCards(github, owner, repo, number) {
  const q = await github.graphql(
    `query($o: String!, $r: String!, $n: Int!) { repository(owner: $o, name: $r) { pullRequest(number: $n) {
      ${PR_FIELDS} closingIssuesReferences(first: 10) { nodes { number } }
    } } }`,
    { o: owner, r: repo, n: number },
  );
  const pr = q.repository.pullRequest;
  const issueNumbers = pr.closingIssuesReferences.nodes.map((n) => n.number);
  if (issueNumbers.length === 0) return [{ issue: null, prs: [pr] }];
  const cards = [];
  for (const n of issueNumbers) {
    const card = await loadIssueCard(github, owner, repo, n);
    if (pr.merged && !card.prs.some((p) => p.number === pr.number)) card.prs.push(pr);
    cards.push(card);
  }
  return cards;
}

const cardKey = ({ issue, prs }) => (issue || prs[0]).url;
const cardClosedAt = ({ issue, prs }) =>
  [issue?.closedAt, ...prs.map((p) => p.mergedAt)].filter(Boolean).sort().pop() || new Date().toISOString();

function contextHeader() {
  return [
    '# 트러블슈팅 기록 작성 자료',
    '',
    '아래 이슈·PR·커밋·코드 변경을 읽고, 카드마다 트러블슈팅 기록을 한국어로 써 주세요. 읽는 사람은 같은 팀 개발자입니다.',
    `결과는 이 저장소 루트의 \`${WRITTEN_FILE}\` 파일에 JSON으로만 써 주세요. 다른 파일은 만들거나 고치지 마세요.`,
    '',
    '형식(카드 키는 아래 "카드 키" 값 그대로, 값 안에서는 마크다운 목록·코드 블록을 써도 됩니다):',
    '```json',
    '{ "<카드 키>": {',
    '  "environment": "OS·런타임·프레임워크 등 한 줄 (예: GitHub Actions ubuntu-latest, Spring Boot 3.5)",',
    '  "tech": "관련 기술 한 줄 (예: GitHub GraphQL API, Notion API)",',
    '  "symptom": "어떤 상황에서 무엇이 잘못 보였는지",',
    '  "reproduce": "재현 순서(번호 목록)",',
    '  "attempts": ["시도한 것과 결과 1", "..."],',
    '  "root_cause": "근본 원인 2~4문장",',
    '  "solution": "최종 해결 방법을 단계별로, 필요하면 수정 전/후 코드",',
    '  "lessons": ["배운 점·개선 사항 1", "..."],',
    '  "summary": "문제와 해결을 2~3문장으로"',
    '} }',
    '```',
    '- 자료에 근거가 없는 내용은 지어내지 말고, 추측이면 "(추정)"이라고 붙여 주세요. 모르면 빈 문자열로 두세요.',
    '- 에러 로그는 원문을 따로 넣으니 다시 옮겨 쓰지 않아도 됩니다.',
    '- attempts는 PR 커밋·본문에 드러난 시도 순서대로, 근거가 없으면 최종 해결 방법 한 줄만 써 주세요.',
    '',
  ];
}

// 글 작성에 쓸 카드 자료: 이슈·PR 본문, 커밋 제목, 코드 변경(카드당 최대 DIFF_LIMIT자)
async function appendCardContext(contextMd, { issue, prs }, github, owner, repo) {
  contextMd.push(`## 카드 키: ${cardKey({ issue, prs })}`, '');
  if (issue) contextMd.push(`### 이슈 #${issue.number} ${issue.title}`, '', issue.body || '(본문 없음)', '');
  let diffBudget = DIFF_LIMIT;
  for (const pr of prs) {
    contextMd.push(`### PR #${pr.number} ${pr.title}`, '', pr.body || '(본문 없음)', '');
    const commits = await github.paginate(github.rest.pulls.listCommits, { owner, repo, pull_number: pr.number, per_page: 100 });
    contextMd.push('커밋:', ...commits.map((c) => `- ${c.commit.message.split('\n')[0]}`), '');
    const files = await github.paginate(github.rest.pulls.listFiles, { owner, repo, pull_number: pr.number, per_page: 100 });
    for (const f of files) {
      const patch = (f.patch || '').slice(0, Math.max(0, diffBudget));
      diffBudget -= patch.length;
      contextMd.push(`변경 파일: ${f.filename} (+${f.additions} -${f.deletions})`, ...(patch ? ['```diff', patch, '```'] : []), '');
    }
  }
}

// 1단계: 이벤트에서 카드를 모으고, 노션에 이미 있는지 보고, 글 작성에 쓸 자료(ts-context.md)를 만든다
async function prepare({ github, context, core }) {
  const notion = notionClient(process.env.NOTION_TOKEN);
  const { owner, repo } = context.repo;
  const resync = context.payload.action === 'labeled';

  const loadIssue = (number) => loadIssueCard(github, owner, repo, number);

  const cards = [];
  if (context.eventName === 'issues') {
    cards.push(await loadIssue(context.payload.issue.number));
  } else {
    cards.push(...(await loadPrCards(github, owner, repo, context.payload.pull_request.number)));
  }

  const contextMd = contextHeader();

  const out = [];
  for (const card of cards) {
    const key = cardKey(card);
    // resync 라벨은 열린 이슈·PR에도 붙일 수 있다. 아직 해결 안 된 건은 해결 기록이 아니라서 건너뛴다
    if (resync && !card.issue?.closedAt && !card.prs.some((p) => p.merged)) {
      core.warning(`아직 해결되지 않아(이슈 열림, 머지된 PR 없음) 카드를 만들지 않습니다: ${key}`);
      continue;
    }
    const closedAt = cardClosedAt(card);
    const found = await notion('POST', `/databases/${NOTION_DB_ID}/query`, {
      filter: { property: LINK_PROPERTY, url: { equals: key } },
      page_size: 1,
    });
    const pageId = found.results[0]?.id || null;
    const action = pageId ? (resync ? 'rewrite' : 'update') : 'create';
    out.push({ ...card, key, closedAt, pageId, action });
    if (action === 'update') continue;

    await appendCardContext(contextMd, card, github, owner, repo);
  }

  fs.writeFileSync(CARDS_FILE, JSON.stringify(out));
  fs.writeFileSync(CONTEXT_FILE, contextMd.join('\n'));
  core.setOutput('need_write', String(out.some((c) => c.action !== 'update')));
  core.info(`카드 ${out.length}개: ${out.map((c) => `${c.action} ${c.key}`).join(', ')}`);
}

// 점검(수동 실행): 닫힌 fix 이슈·머지된 fix PR을 전부 카드로 묶어 노션과 비교한다
// - 빠진 것: 이슈·PR을 한 장으로 묶은 뒤에도 노션에 카드가 아예 없는 것
// - 중복: 같은 "GitHub 링크"를 가진 노션 카드가 두 장 이상인 것 (보고만 하고 지우지 않음)
// mode가 'create'면 빠진 것을 최대 AUDIT_CREATE_LIMIT건까지 만들 준비를 한다(이후 글 작성 → write 단계)
const AUDIT_CREATE_LIMIT = 10;

async function audit({ github, context, core }) {
  const notion = notionClient(process.env.NOTION_TOKEN);
  const { owner, repo } = context.repo;
  const mode = process.env.AUDIT_MODE === 'create' ? 'create' : 'report';

  // GitHub: 닫힌 fix:수정 이슈·PR (issues API는 PR도 같이 돌려준다)
  const closed = await github.paginate(github.rest.issues.listForRepo, { owner, repo, state: 'closed', labels: 'fix:수정', per_page: 100 });
  const cards = new Map();
  for (const item of closed) {
    let found = [];
    if (item.pull_request) {
      if (!item.pull_request.merged_at) continue; // 머지 안 하고 닫은 PR은 해결된 게 아님
      found = await loadPrCards(github, owner, repo, item.number);
    } else {
      if (item.state_reason === 'not_planned') continue;
      found = [await loadIssueCard(github, owner, repo, item.number)];
    }
    for (const card of found) if (!cards.has(cardKey(card))) cards.set(cardKey(card), card);
  }

  // 노션: 트래블 슈팅 DB 전체의 GitHub 링크
  const links = new Map();
  let cursor;
  do {
    const res = await notion('POST', `/databases/${NOTION_DB_ID}/query`, { page_size: 100, ...(cursor ? { start_cursor: cursor } : {}) });
    for (const page of res.results) {
      const url = page.properties?.[LINK_PROPERTY]?.url;
      if (url) links.set(url, [...(links.get(url) || []), page.url]);
    }
    cursor = res.has_more ? res.next_cursor : null;
  } while (cursor);

  const missing = [...cards.values()].filter((card) => !links.has(cardKey(card)));
  const duplicates = [...links.entries()].filter(([, pages]) => pages.length > 1);
  const label = (card) => (card.issue ? `이슈 #${card.issue.number}` : `PR #${card.prs[0].number}`) + ` ${cleanTitle((card.issue || card.prs[0]).title)}`;

  await core.summary
    .addHeading('트러블슈팅 점검 결과')
    .addRaw(`GitHub 카드 ${cards.size}개 · 노션 카드 ${[...links.values()].flat().length}개 · 빠진 것 ${missing.length}개 · 중복 ${duplicates.length}개`, true)
    .addHeading('빠진 것 (노션에 카드가 없음)', 3)
    .addList(missing.length ? missing.map((c) => `${label(c)} — ${cardKey(c)}`) : ['없음'])
    .addHeading('중복 (같은 GitHub 링크가 두 장 이상, 지우지 않음)', 3)
    .addList(duplicates.length ? duplicates.map(([url, pages]) => `${url} → ${pages.join(', ')}`) : ['없음'])
    .write();

  const targets = mode === 'create' ? missing.slice(0, AUDIT_CREATE_LIMIT) : [];
  if (mode === 'create' && missing.length > AUDIT_CREATE_LIMIT) {
    core.warning(`빠진 것이 ${missing.length}개라 이번엔 ${AUDIT_CREATE_LIMIT}개만 만듭니다. 한 번 더 실행하면 나머지를 이어서 만듭니다.`);
  }
  const contextMd = contextHeader();
  const out = [];
  for (const card of targets) {
    out.push({ ...card, key: cardKey(card), closedAt: cardClosedAt(card), pageId: null, action: 'create' });
    await appendCardContext(contextMd, card, github, owner, repo);
  }
  fs.writeFileSync(CARDS_FILE, JSON.stringify(out));
  fs.writeFileSync(CONTEXT_FILE, contextMd.join('\n'));
  core.setOutput('need_write', String(out.length > 0));
  core.info(`빠진 것 ${missing.length}개, 중복 ${duplicates.length}개, 이번에 만들 것 ${out.length}개`);
}

// 연동 기록(최초 연동·마지막 반영·재연동 횟수): 이슈·PR 연동 댓글과 노션 카드 본문 맨 위 한 줄에 같은 값을 쓴다
// (디스커션 연동과 같은 형식, build-sync-comment.sh 참고. 시각은 한국 시간)
// 처음 연동하면 새 댓글, 재연동하면 그 이슈·PR의 연동 댓글을 찾아 고친다
function kstStamp(iso) {
  const t = iso ? new Date(iso).getTime() : Date.now();
  return new Date(t + 9 * 3600 * 1000).toISOString().replace('T', ' ').slice(0, 16);
}
function syncMeta(old) {
  const now = kstStamp();
  if (!old) return { first: now, now, count: 0 };
  const first = old.body.match(/^- 처음 연동: (.+)$/m)?.[1] || kstStamp(old.created_at);
  const count = Number(old.body.match(/\(재연동 (\d+)회\)/)?.[1] || 0) + 1;
  return { first, now, count };
}
const syncLine = ({ first, now, count }) =>
  count === 0 ? `🔄 최초 연동: ${first}` : `🔄 최초 연동: ${first} · 마지막 반영: ${now} (재연동 ${count}회)`;
function syncCommentBody(pageUrl, isPr, { first, now, count }) {
  const lines = [`🔗 노션 동기화됨: ${pageUrl}`, `- 처음 연동: ${first}`];
  if (count > 0) lines.push(`- 마지막 반영: ${now} (재연동 ${count}회)`);
  lines.push('', `수정 후 다시 반영하려면 이 ${isPr ? 'PR' : '이슈'}에 \`resync:재연동\` 라벨을 붙여주세요.`);
  return lines.join('\n');
}
async function findSyncComment(github, context, core, number) {
  try {
    const comments = await github.paginate(github.rest.issues.listComments, { ...context.repo, issue_number: number, per_page: 100 });
    return comments.filter((c) => c.user?.login === 'github-actions[bot]' && c.body?.includes('노션 동기화됨')).pop() || null;
  } catch (e) {
    core.warning(`#${number} 연동 댓글을 찾지 못했습니다: ${e.message}`);
    return null;
  }
}
async function commentSynced(github, context, core, number, isPr, pageUrl, meta, old = null) {
  try {
    const body = syncCommentBody(pageUrl, isPr, meta);
    if (old) await github.rest.issues.updateComment({ ...context.repo, comment_id: old.id, body });
    else await github.rest.issues.createComment({ ...context.repo, issue_number: number, body });
  } catch (e) {
    core.warning(`#${number} 연동 댓글을 못 남겼습니다: ${e.message}`);
  }
}

// 2단계: 작성 결과(ts-written.json)를 읽어 노션에 쓰고, resync 라벨을 뗀다 (작성에 실패했으면 이슈·PR 원문으로 채운다)
async function write({ github, context, core }) {
  const notion = notionClient(process.env.NOTION_TOKEN);
  const userMap = JSON.parse(process.env.NOTION_USER_MAP || '{}');
  const cards = JSON.parse(fs.readFileSync(CARDS_FILE, 'utf8'));
  let written = {};
  try {
    written = JSON.parse(fs.readFileSync(WRITTEN_FILE, 'utf8'));
  } catch (e) {
    core.warning(`작성 결과(${WRITTEN_FILE})를 읽지 못해 이슈·PR 원문으로 채웁니다: ${e.message}`);
  }

  let rewriteSkipped = false;
  for (const card of cards) {
    const base = card.issue || card.prs[0];
    const authorId = userMap[base.author?.login];
    const properties = {
      이름: { title: richText(cleanTitle(base.title)) },
      날짜: { date: { start: kstDate(card.closedAt) } },
      [LINK_PROPERTY]: { url: card.key },
      ...(authorId ? { 작성자: { people: [{ id: authorId }] } } : {}),
    };

    if (card.action === 'update') {
      await notion('PATCH', `/pages/${card.pageId}`, { properties });
      core.info(`이미 있는 카드라 날짜·작성자만 갱신: ${card.key}`);
      continue;
    }

    // rewrite: 글 작성에 실패했으면 잘 써진 기존 본문을 원문 대체본으로 덮지 않고 건너뛴다(라벨을 남겨 다시 시도할 수 있게)
    if (card.action === 'rewrite' && !written[card.key]) {
      core.warning(`글 작성에 실패해 기존 카드 본문을 그대로 둡니다. resync:재연동 라벨을 뗐다가 다시 붙여 재시도하세요: ${card.key}`);
      rewriteSkipped = true;
      continue;
    }

    // 재연동이면 라벨을 붙인 이슈·PR의 연동 댓글에서 최초 연동 시각·횟수를 이어받는다
    const target = context.payload.issue || context.payload.pull_request;
    const oldComment = card.action === 'rewrite' && target ? await findSyncComment(github, context, core, target.number) : null;
    const meta = syncMeta(card.action === 'rewrite' ? oldComment || { body: '', created_at: null } : null);
    // 본문에 붙인 이미지를 노션에 올려 이미지 블록으로 만든다. 실패한 이미지는 "이미지 원본 보기" 링크로 들어간다
    const warn = (m) => core.warning(m);
    const images = {
      issue: await imageBlocks(extractImages(card.issue?.body), process.env.NOTION_TOKEN, warn),
      prs: await imageBlocks(card.prs.flatMap((pr) => extractImages(pr.body)), process.env.NOTION_TOKEN, warn),
    };
    const blocks = buildBlocks(card, written[card.key] || null, syncLine(meta), images, userMap);
    if (card.action === 'create') {
      const page = await notion('POST', '/pages', { parent: { database_id: NOTION_DB_ID }, properties, children: blocks.slice(0, 100) });
      for (let i = 100; i < blocks.length; i += 100) {
        await notion('PATCH', `/blocks/${page.id}/children`, { children: blocks.slice(i, i + 100) });
      }
      core.info(`노션 트러블슈팅 카드 생성: ${page.url}`);
      // 점검 워크플로(수동 실행)로 옛 카드를 한꺼번에 채울 때는 이슈·PR마다 알림이 가지 않게 댓글을 남기지 않는다
      if (context.eventName !== 'workflow_dispatch') {
        if (card.issue) await commentSynced(github, context, core, card.issue.number, false, page.url, meta);
        for (const pr of card.prs) await commentSynced(github, context, core, pr.number, true, page.url, meta);
      }
      continue;
    }

    // 새 내용을 먼저 덧붙이고, 성공하면 예전 블록을 지운다(중간에 실패해도 내용이 비지 않게)
    await notion('PATCH', `/pages/${card.pageId}`, { properties });
    const old = [];
    let cursor;
    do {
      const res = await notion('GET', `/blocks/${card.pageId}/children?page_size=100${cursor ? `&start_cursor=${cursor}` : ''}`);
      old.push(...res.results.map((b) => b.id));
      cursor = res.has_more ? res.next_cursor : null;
    } while (cursor);
    for (let i = 0; i < blocks.length; i += 100) {
      await notion('PATCH', `/blocks/${card.pageId}/children`, { children: blocks.slice(i, i + 100) });
    }
    for (const id of old) await notion('DELETE', `/blocks/${id}`);
    core.info(`resync로 카드 본문을 다시 씀: ${card.key}`);
    // 재연동 라벨을 붙인 이슈·PR의 연동 댓글에 반영 시각을 고쳐 넣는다(없으면 새로 단다). 댓글이 실패해도 이미 끝난 노션 반영·라벨 제거에는 영향을 주지 않는다
    try {
      const pageUrl = (await notion('GET', `/pages/${card.pageId}`)).url;
      await commentSynced(github, context, core, target.number, Boolean(context.payload.pull_request), pageUrl, meta, oldComment);
    } catch (e) {
      core.warning(`재연동 댓글을 남기지 못했습니다: ${e.message}`);
    }
  }

  if (context.payload.action === 'labeled' && !rewriteSkipped) {
    const number = (context.payload.issue || context.payload.pull_request).number;
    await github.rest.issues.removeLabel({ ...context.repo, issue_number: number, name: 'resync:재연동' }).catch(() => {});
  }
}

module.exports = { prepare, audit, write, buildBlocks, sections };
