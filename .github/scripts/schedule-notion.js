
const DATA_SOURCE_ID = 'da79e310-9743-8272-8e01-87d6c9b6a45e';
const ISSUE_PROPERTY = '깃이슈';
const STATUS_PROPERTY = '진행상태';
const START_PROPERTY = '시작일';
const END_PROPERTY = '종료일';
const BRANCH_PATTERN = /^[a-z]+\/(\d+)-/;
const BASE_BRANCH = 'develop';

function kstDate(iso) {
  return new Date(new Date(iso).getTime() + 9 * 3600 * 1000).toISOString().slice(0, 10);
}

function notionClient(token) {
  return async (method, path, body) => {
    const res = await fetch(`https://api.notion.com/v1${path}`, {
      method,
      headers: { Authorization: `Bearer ${token}`, 'Notion-Version': '2025-09-03', 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined,
    });
    const json = await res.json();
    if (!res.ok) throw new Error(`노션 ${method} ${path} 실패(${res.status}): ${JSON.stringify(json)}`);
    return json;
  };
}

async function findRow(notion, issueUrl) {
  const res = await notion('POST', `/data_sources/${DATA_SOURCE_ID}/query`, {
    filter: { property: ISSUE_PROPERTY, url: { equals: issueUrl } },
    page_size: 10,
  });
  return res.results;
}

function statusOf(page) {
  return page.properties[STATUS_PROPERTY]?.status?.name || '';
}

async function start({ github, context, core }) {
  const notion = notionClient(process.env.NOTION_TOKEN);
  const { owner, repo } = context.repo;
  const branch = context.ref.replace('refs/heads/', '');
  const match = branch.match(BRANCH_PATTERN);
  if (!match) {
    core.info(`브랜치 이름(${branch})에 이슈 번호가 없어 건너뜁니다.`);
    return;
  }
  const issueUrl = `https://github.com/${owner}/${repo}/issues/${match[1]}`;
  const rows = (await findRow(notion, issueUrl)).filter((page) => statusOf(page) === '시작 전');
  if (rows.length === 0) {
    core.info(`${issueUrl}: "시작 전"인 노션 행이 없어 건너뜁니다.`);
    return;
  }

  const base = BASE_BRANCH;
  const compare = await github.rest.repos.compareCommitsWithBasehead({ owner, repo, basehead: `${base}...${branch}`, per_page: 100 });
  const dates = compare.data.commits.map((c) => c.commit.author?.date || c.commit.committer?.date).filter(Boolean);
  if (dates.length === 0) {
    core.info(`${branch}: ${base} 대비 새 커밋이 없어 건너뜁니다.`);
    return;
  }
  const startDate = kstDate(dates.sort()[0]);

  for (const page of rows) {
    await notion('PATCH', `/pages/${page.id}`, {
      properties: {
        [START_PROPERTY]: { date: { start: startDate } },
        [STATUS_PROPERTY]: { status: { name: '진행 중' } },
      },
    });
  }
  core.info(`${issueUrl}: 시작일 ${startDate}, 진행 중으로 바꿨습니다.`);
}

async function finishIssue(notion, core, issueUrl, closedAt) {
  const rows = (await findRow(notion, issueUrl)).filter((page) => statusOf(page) !== '완료');
  if (rows.length === 0) {
    core.info(`${issueUrl}: 바꿀 노션 행이 없거나 이미 완료입니다.`);
    return;
  }
  const endDate = kstDate(closedAt);
  for (const page of rows) {
    await notion('PATCH', `/pages/${page.id}`, {
      properties: {
        [END_PROPERTY]: { date: { start: endDate } },
        [STATUS_PROPERTY]: { status: { name: '완료' } },
      },
    });
  }
  core.info(`${issueUrl}: 종료일 ${endDate}, 완료로 바꿨습니다.`);
}

async function finish({ github, context, core }) {
  const notion = notionClient(process.env.NOTION_TOKEN);
  const { owner, repo } = context.repo;

  if (context.eventName === 'issues') {
    const issue = context.payload.issue;
    await finishIssue(notion, core, `https://github.com/${owner}/${repo}/issues/${issue.number}`, issue.closed_at);
    return;
  }

  const pr = context.payload.pull_request;
  const q = await github.graphql(
    `query($o: String!, $r: String!, $n: Int!) { repository(owner: $o, name: $r) { pullRequest(number: $n) {
      closingIssuesReferences(first: 20) { nodes { number repository { nameWithOwner } } }
    } } }`,
    { o: owner, r: repo, n: pr.number },
  );
  const issues = q.repository.pullRequest.closingIssuesReferences.nodes.filter(
    (node) => node.repository.nameWithOwner === `${owner}/${repo}`,
  );
  if (issues.length === 0) {
    core.info(`PR #${pr.number}에 연결된 이슈가 없어 건너뜁니다.`);
    return;
  }
  for (const issue of issues) {
    await finishIssue(notion, core, `https://github.com/${owner}/${repo}/issues/${issue.number}`, pr.merged_at);
  }
}

module.exports = { start, finish };
