const fs = require('fs');
const path = require('path');

const MARKER = '<!-- develop-sync -->';
const LABEL = '🚨 develop 반영 필수';
const CONFLICT_LABEL = '⚠️ 충돌';
const CHECK_LINE = '여기를 체크하면 최신 develop을 이 PR에 반영해요';
const BOT_LOGIN = 'github-actions[bot]';
const PUSH_BOT_NAME = 'monew-team03[bot]';
const PUSH_BOT_EMAIL = '344808795+monew-team03[bot]@users.noreply.github.com';

const TEXT = {
  latest: '✅ 최신이에요',
  merged: '✅ 최신 develop을 반영했어요. 로컬에서 이어서 작업하기 전에 `git pull` 한 번 하세요',
  merging: '⏳ 최신 develop을 반영하는 중이에요…',
  behind: (n) => `develop보다 ${n}커밋 뒤예요. 반영 안 해도 머지 가능해요`,
  building: (files) => `⏳ develop에 새로 들어온 변경과 같은 파일(${fileList(files)})을 고쳐서, 합친 상태로 빌드해 보는 중이에요`,
  buildPass: (files) => `develop에 새로 들어온 변경과 같은 파일(${fileList(files)})을 고쳤지만, 합쳐서 빌드해 보니 문제없어요. 반영 안 해도 머지 가능해요`,
  buildFail: (tests) => [
    '🚨 이 PR은 반드시 최신 develop을 반영해야 해요. 합치면 빌드가 실패해요',
    tests.length > 0 ? `실패 테스트: ${tests.slice(0, 10).map((t) => `\`${t}\``).join(', ')}${tests.length > 10 ? ` 외 ${tests.length - 10}개` : ''}` : '실패 원인은 이 워크플로의 빌드 로그에서 확인해 주세요',
  ].join('\n'),
  conflict: (files, branch) => [
    '🚨 이 PR은 반드시 최신 develop을 반영해야 해요. develop과 충돌이 나서 체크박스로는 반영할 수 없어요',
    files.length > 0 ? `충돌 파일: ${fileList(files, 10)}` : '',
    '',
    '충돌이 풀릴 때까지 CI·자동 리뷰가 실행되지 않아요. GitHub 웹의 "Resolve conflicts" 버튼 대신 로컬에서 해결해 주세요',
    '',
    '로컬에서 이렇게 해결해 주세요',
    '```bash',
    `git checkout ${branch}`,
    'git pull',
    'git fetch origin develop',
    'git merge origin/develop',
    '# 충돌 파일을 고친 뒤',
    'git add .',
    'git commit',
    'git push',
    '```',
  ].filter((line, i) => line !== '' || i === 2 || i === 4).join('\n'),
  notAllowed: (user) => `@${user} 체크박스는 저장소 쓰기 권한이 있는 팀원만 쓸 수 있어요`,
  forkOrClosed: '열린 PR이 아니거나 다른 저장소에서 온 PR이라 체크박스로 반영할 수 없어요',
};

function fileList(files, max = 5) {
  const shown = files.slice(0, max).map((f) => `\`${path.basename(f)}\``).join(', ');
  return files.length > max ? `${shown} 외 ${files.length - max}개` : shown;
}

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

const HEADER = '### 🔄 develop 최신 반영';

function statusOf(body) {
  const start = body.indexOf(HEADER);
  const end = body.indexOf(`- [`, start);
  if (start < 0 || end < 0) return null;
  return body.slice(start + HEADER.length, end).trim();
}

function commentBody(status, previousBody) {
  const prev = previousBody ? Number((previousBody.match(/재수정 (\d+)회/) || [0, 0])[1]) : 0;
  const footer = previousBody ? `재수정 ${prev + 1}회, 수정된 시각 ${nowKst()}` : `작성 시각 ${nowKst()}`;
  return [MARKER, HEADER, '', status, '', `- [ ] ${CHECK_LINE}`, '', `<sub>${footer}</sub>`].join('\n');
}

function issueOf(ref) {
  const m = ref.match(/^[a-z]+\/(\d+)-/);
  return m ? m[1] : null;
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

async function findComment(github, repo, number) {
  const comments = await github.paginate(github.rest.issues.listComments, {
    ...repo,
    issue_number: number,
    per_page: 100,
  });
  return comments.find((c) => c.user && c.user.login === BOT_LOGIN && c.body && c.body.includes(MARKER));
}

async function upsert(github, repo, number, status) {
  const existing = await findComment(github, repo, number);
  if (existing && statusOf(existing.body) === status.trim()) return;
  const body = commentBody(status, existing && existing.body);
  if (existing) {
    await github.rest.issues.updateComment({ ...repo, comment_id: existing.id, body });
  } else {
    await github.rest.issues.createComment({ ...repo, issue_number: number, body });
  }
}

async function addLabel(github, repo, number, name) {
  await github.rest.issues.addLabels({ ...repo, issue_number: number, labels: [name] });
}

async function removeLabel(github, repo, number, name) {
  try {
    await github.rest.issues.removeLabel({ ...repo, issue_number: number, name });
  } catch (e) {
    if (e.status !== 404) throw e;
  }
}

function discordIdOf(login) {
  return process.env[`DISCORD_ID_${login.toUpperCase().replace(/[^A-Z0-9]/g, '_')}`] || '';
}

async function discord(path, body) {
  const res = await fetch(`https://discord.com/api/v10${path}`, {
    method: 'POST',
    headers: { Authorization: `Bot ${process.env.DISCORD_BOT_TOKEN}`, 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  if (!res.ok) throw new Error(`디스코드 응답 ${res.status}`);
  return res.json();
}

async function dmConflict(github, repo, number) {
  try {
    const { data: pr } = await github.rest.pulls.get({ ...repo, pull_number: number });
    const userId = discordIdOf(pr.user.login);
    if (!process.env.DISCORD_BOT_TOKEN || !userId) {
      console.log(`충돌 DM 건너뜀: ${pr.user.login}의 디스코드 ID나 봇 토큰이 없어요`);
      return;
    }
    const channel = await discord('/users/@me/channels', { recipient_id: userId });
    await discord(`/channels/${channel.id}/messages`, {
      content: `⚠️ PR이 develop과 충돌이 나요. 충돌이 풀릴 때까지 CI·자동 리뷰가 돌지 않아요. 로컬에서 \`git merge origin/develop\`으로 해결해 주세요\n**${pr.title}**\n${pr.html_url}`,
    });
  } catch (e) {
    console.log(`충돌 DM 실패: ${e.message}`);
  }
}

async function setLabel(github, repo, number, state) {
  if (state === 'conflict') {
    const { data: labels } = await github.rest.issues.listLabelsOnIssue({ ...repo, issue_number: number });
    const isNew = !labels.some((l) => l.name === CONFLICT_LABEL);
    await addLabel(github, repo, number, CONFLICT_LABEL);
    await removeLabel(github, repo, number, LABEL);
    if (isNew) await dmConflict(github, repo, number);
  } else if (state === 'must') {
    await addLabel(github, repo, number, LABEL);
    await removeLabel(github, repo, number, CONFLICT_LABEL);
  } else {
    await removeLabel(github, repo, number, LABEL);
    await removeLabel(github, repo, number, CONFLICT_LABEL);
  }
}

async function getPr(github, repo, number) {
  for (let i = 0; i < 6; i++) {
    const { data } = await github.rest.pulls.get({ ...repo, pull_number: number });
    if (data.mergeable !== null || data.state !== 'open') return data;
    await sleep(5000);
  }
  return (await github.rest.pulls.get({ ...repo, pull_number: number })).data;
}

async function git(exec, args, opts = {}) {
  return exec.getExecOutput('git', args, { silent: true, ignoreReturnCode: true, ...opts });
}

async function conflictFiles(exec, sha) {
  await git(exec, ['fetch', '-q', 'origin', '+refs/heads/develop:refs/remotes/origin/develop', sha]);
  await git(exec, ['checkout', '-q', '--detach', sha]);
  await git(exec, ['merge', '--no-commit', '--no-ff', 'origin/develop']);
  const { stdout } = await git(exec, ['diff', '--name-only', '--diff-filter=U']);
  await git(exec, ['merge', '--abort']);
  return stdout.split('\n').map((s) => s.trim()).filter(Boolean);
}

function isSameRepoDevelopPr(pr, repo) {
  return pr.base.ref === 'develop' && pr.head.repo && pr.head.repo.full_name === `${repo.owner}/${repo.repo}`;
}

async function evaluateOne({ github, repo, exec, pr }) {
  const fresh = await getPr(github, repo, pr.number);
  if (fresh.state !== 'open' || fresh.draft) return null;

  const { data: cmp } = await github.rest.repos.compareCommitsWithBasehead({
    ...repo,
    basehead: `${fresh.head.sha}...develop`,
  });
  const behind = cmp.ahead_by;
  const existing = await findComment(github, repo, pr.number);

  if (behind === 0) {
    const keepMerged = existing && existing.body.includes(TEXT.merged);
    await upsert(github, repo, pr.number, keepMerged ? TEXT.merged : TEXT.latest);
    await setLabel(github, repo, pr.number, 'none');
    return null;
  }

  if (fresh.mergeable === false) {
    const files = await conflictFiles(exec, fresh.head.sha);
    await upsert(github, repo, pr.number, TEXT.conflict(files, fresh.head.ref));
    await setLabel(github, repo, pr.number, 'conflict');
    return null;
  }

  const prFiles = new Set(
    (await github.paginate(github.rest.pulls.listFiles, { ...repo, pull_number: pr.number, per_page: 100 })).map(
      (f) => f.filename,
    ),
  );
  const overlap = (cmp.files || []).map((f) => f.filename).filter((f) => prFiles.has(f));

  if (overlap.length === 0) {
    await upsert(github, repo, pr.number, TEXT.behind(behind));
    await setLabel(github, repo, pr.number, 'none');
    return null;
  }

  await upsert(github, repo, pr.number, TEXT.building(overlap));
  return { number: pr.number, sha: fresh.head.sha, files: overlap.slice(0, 20) };
}

async function status({ github, context, core, exec }) {
  const repo = context.repo;
  let prs;
  if (context.eventName === 'pull_request') {
    prs = [context.payload.pull_request];
  } else {
    prs = await github.paginate(github.rest.pulls.list, { ...repo, state: 'open', base: 'develop', per_page: 100 });
  }
  prs = prs.filter((p) => !p.draft && isSameRepoDevelopPr(p, repo));

  await git(exec, ['config', 'user.name', PUSH_BOT_NAME]);
  await git(exec, ['config', 'user.email', PUSH_BOT_EMAIL]);

  const builds = [];
  const failed = [];
  for (const pr of prs) {
    try {
      const b = await evaluateOne({ github, repo, exec, pr });
      if (b) builds.push(b);
    } catch (e) {
      failed.push(pr.number);
      core.warning(`PR #${pr.number} 확인 실패: ${e.message}`);
    }
  }
  core.setOutput('builds', JSON.stringify(builds));
  if (failed.length > 0) {
    core.setFailed(`PR ${failed.map((n) => `#${n}`).join(', ')} 확인에 실패했어요`);
  }
}

function failedTests(dir) {
  if (!fs.existsSync(dir)) return [];
  const names = [];
  for (const file of fs.readdirSync(dir).filter((f) => f.endsWith('.xml'))) {
    const xml = fs.readFileSync(path.join(dir, file), 'utf8');
    const re = /<testcase\s+name="([^"]*)"\s+classname="([^"]*)"[^>]*?(?:\/>|>([\s\S]*?)<\/testcase>)/g;
    let m;
    while ((m = re.exec(xml)) !== null) {
      if (m[3] && /<(failure|error)\b/.test(m[3])) {
        names.push(`${m[2].split('.').pop()}.${m[1]}`);
      }
    }
  }
  return names;
}

async function buildResult({ github, context, core }) {
  const repo = context.repo;
  const number = Number(process.env.PR_NUMBER);
  const files = JSON.parse(process.env.FILES || '[]');
  const outcome = process.env.MERGE_OUTCOME === 'failure' ? 'conflict' : process.env.BUILD_OUTCOME;

  const { data: pr } = await github.rest.pulls.get({ ...repo, pull_number: number });
  if (pr.state !== 'open' || pr.head.sha !== process.env.HEAD_SHA) {
    core.info(`PR #${number}에 새 커밋이 올라와 이번 결과는 반영하지 않아요`);
    return;
  }

  if (outcome === 'conflict') {
    await upsert(github, repo, number, TEXT.conflict([], pr.head.ref));
    await setLabel(github, repo, number, 'conflict');
  } else if (outcome === 'success') {
    await upsert(github, repo, number, TEXT.buildPass(files));
    await setLabel(github, repo, number, 'none');
  } else {
    await upsert(github, repo, number, TEXT.buildFail(failedTests('build/test-results/test')));
    await setLabel(github, repo, number, 'must');
  }
}

async function syncCheck({ github, context, core }) {
  const repo = context.repo;
  const comment = context.payload.comment;
  const number = context.payload.issue.number;
  const sender = context.payload.sender.login;

  if (!comment.user || comment.user.login !== BOT_LOGIN || !comment.body.includes(MARKER)) return;
  const before = (context.payload.changes && context.payload.changes.body && context.payload.changes.body.from) || '';
  const checkedNow = comment.body.includes(`- [x] ${CHECK_LINE}`);
  const checkedBefore = before.includes(`- [x] ${CHECK_LINE}`);
  if (!checkedNow || checkedBefore) return;

  const { data: perm } = await github.rest.repos.getCollaboratorPermissionLevel({ ...repo, username: sender });
  if (!['admin', 'maintain', 'write'].includes(perm.permission) && !['admin', 'maintain', 'write'].includes(perm.role_name)) {
    await github.rest.issues.updateComment({ ...repo, comment_id: comment.id, body: comment.body.replace(`- [x] ${CHECK_LINE}`, `- [ ] ${CHECK_LINE}`) });
    await github.rest.issues.createComment({ ...repo, issue_number: number, body: TEXT.notAllowed(sender) });
    return;
  }

  const { data: pr } = await github.rest.pulls.get({ ...repo, pull_number: number });
  if (pr.state !== 'open' || !isSameRepoDevelopPr(pr, repo)) {
    await upsert(github, repo, number, TEXT.forkOrClosed);
    return;
  }

  await upsert(github, repo, number, TEXT.merging);
  core.setOutput('ok', 'true');
  core.setOutput('ref', pr.head.ref);
}

async function syncMerge({ github, context, exec }) {
  const repo = context.repo;
  const number = context.payload.issue.number;
  const ref = process.env.HEAD_REF;
  const issue = issueOf(ref);
  const title = `merge : develop 최신 내용 반영${issue ? ` [#${issue}]` : ''}`;

  await git(exec, ['config', 'user.name', PUSH_BOT_NAME]);
  await git(exec, ['config', 'user.email', PUSH_BOT_EMAIL]);
  await git(exec, ['fetch', '-q', 'origin', '+refs/heads/develop:refs/remotes/origin/develop']);

  const merge = await git(exec, ['merge', '--no-ff', '-m', title, 'origin/develop']);
  if (merge.exitCode === 0 && /Already up to date/i.test(merge.stdout)) {
    await upsert(github, repo, number, TEXT.latest);
    await setLabel(github, repo, number, 'none');
    return;
  }
  if (merge.exitCode !== 0) {
    const { stdout } = await git(exec, ['diff', '--name-only', '--diff-filter=U']);
    await git(exec, ['merge', '--abort']);
    const files = stdout.split('\n').map((s) => s.trim()).filter(Boolean);
    await upsert(github, repo, number, TEXT.conflict(files, ref));
    await setLabel(github, repo, number, 'conflict');
    return;
  }

  const push = await git(exec, ['push', 'origin', `HEAD:refs/heads/${ref}`]);
  if (push.exitCode !== 0) {
    await upsert(github, repo, number, `반영하지 못했어요. 그사이 브랜치에 새 커밋이 올라왔을 수 있어요. 체크박스를 다시 눌러 주세요\n\`\`\`\n${push.stderr.trim().slice(0, 500)}\n\`\`\``);
    return;
  }
  await upsert(github, repo, number, TEXT.merged);
  await setLabel(github, repo, number, 'none');
}

async function conflictNotice({ github, context, core }) {
  const repo = context.repo;
  const pr = await getPr(github, repo, context.payload.pull_request.number);
  if (pr.state !== 'open' || pr.draft || !isSameRepoDevelopPr(pr, repo)) return;
  if (pr.mergeable !== false) {
    core.info(`PR #${pr.number}은 충돌이 없어요 (mergeable: ${pr.mergeable})`);
    return;
  }
  await upsert(github, repo, pr.number, TEXT.conflict([], pr.head.ref));
  await setLabel(github, repo, pr.number, 'conflict');
}

module.exports = { status, buildResult, syncCheck, syncMerge, conflictNotice, _test: { commentBody, statusOf, issueOf, failedTests, TEXT, fileList, discordIdOf } };
