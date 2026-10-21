// 본문에 붙인 이미지(스크린샷)를 노션 이미지 블록으로 바꾼다 (#119)
// - GitHub에 붙여 넣으면 생기는 <img src="..."> 와 마크다운 ![설명](주소) 둘 다 찾는다
// - GitHub 이미지 주소는 몇 분 뒤 만료되는 임시 주소로 넘어가서 노션에 주소만 걸면 나중에 깨진다
//   → 내려받아서 노션에 직접 업로드한다(노션 파일 업로드 API)
// - GitHub에 올린 이미지가 아니거나, 너무 크거나, 업로드에 실패하면 "이미지 원본 보기" 링크 문단으로 대신 넣는다
//
// 쓰는 곳
// - troubleshooting-notion.js: require 해서 extractImages / stripImages / imageBlocks 사용
// - build-notion-blocks.sh(디스커션 회의록·멘토링): `node notion-images.js` — 표준입력 본문을 받아
//   이미지 자리에 표시 줄을 넣은 본문과 이미지 블록 목록을 JSON으로 출력 {"body": "...", "images": [...]}

const NOTION_VERSION = '2022-06-28';
const MAX_BYTES = 5 * 1024 * 1024; // 노션 무료 플랜 파일 한 개 최대 크기
const MARKER = (i) => `[[NOTION_IMAGE:${i}]]`;

// <img ... src="주소" ... alt="설명" ...> 또는 ![설명](주소 "제목")
const IMAGE_PATTERN = /<img\b[^>]*>|!\[([^\]]*)\]\(\s*<?([^)\s>]+)>?(?:\s+"[^"]*")?\s*\)/gi;

function parseMatch(match) {
  if (match[0].startsWith('<')) {
    const attr = (name) => match[0].match(new RegExp(`\\b${name}\\s*=\\s*["']([^"']*)["']`, 'i'))?.[1] || '';
    return { url: attr('src'), alt: attr('alt') };
  }
  return { url: match[2], alt: match[1] };
}

// http(s) 주소만 이미지로 본다. GitHub 이미지가 아니거나 http면 업로드하지 않고 링크로 넣는다(imageBlocks)
const isWebUrl = (url) => /^https?:\/\//i.test(url);

// 코드 블록(``` ... ```) 안은 이미지로 보지 않는다 — 에러 로그에 섞인 HTML이 지워지거나 업로드되지 않게
// 코드 블록 밖 부분에만 fn을 적용하고, 코드 블록은 그대로 둔다
const CODE_FENCE = /```[\s\S]*?```/g;
function outsideCode(text, fn) {
  const source = text || '';
  let out = '';
  let last = 0;
  for (const fence of source.matchAll(CODE_FENCE)) {
    out += fn(source.slice(last, fence.index)) + fence[0];
    last = fence.index + fence[0].length;
  }
  return out + fn(source.slice(last));
}

// 본문 순서대로 이미지 목록 [{ url, alt }]
function extractImages(text) {
  const images = [];
  outsideCode(text, (part) => {
    for (const match of part.matchAll(IMAGE_PATTERN)) {
      const image = parseMatch(match);
      if (isWebUrl(image.url)) images.push(image);
    }
    return part;
  });
  return images;
}

// 글 부분에 <img ...> 같은 태그 글자가 그대로 보이지 않게 이미지 표시를 지운다
const stripImages = (text) => outsideCode(text, (part) => part.replace(IMAGE_PATTERN, '')).replace(/\n{3,}/g, '\n\n');

// 이미지 자리에 표시 줄을 넣는다(앞뒤 빈 줄로 따로 떼서 문단 하나가 되게) — 디스커션 변환용
// 번호는 extractImages가 돌려준 목록 순서와 같다
function markImages(text) {
  let i = 0;
  return outsideCode(text, (part) =>
    part.replace(IMAGE_PATTERN, (...args) => (isWebUrl(parseMatch(args).url) ? `\n\n${MARKER(i++)}\n\n` : '')),
  );
}

// GitHub에 올린 이미지만 내려받는다(아무 주소나 받아서 노션에 올리지 않게)
const isGithubImage = (url) => /^https:\/\/(github\.com\/user-attachments\/|([a-z0-9-]+\.)*githubusercontent\.com\/)/i.test(url);

const caption = (alt) => (alt && alt.toLowerCase() !== 'image' ? [{ type: 'text', text: { content: alt.slice(0, 1900) } }] : []);

function linkBlock({ url, alt }) {
  const label = alt && alt.toLowerCase() !== 'image' ? `🖼️ ${alt} (이미지 원본 보기)` : '🖼️ 이미지 원본 보기';
  return { object: 'block', type: 'paragraph', paragraph: { rich_text: [{ type: 'text', text: { content: label, link: { url } } }] } };
}

const EXTENSIONS = { 'image/png': 'png', 'image/jpeg': 'jpg', 'image/gif': 'gif', 'image/webp': 'webp', 'image/svg+xml': 'svg' };

async function uploadToNotion(token, { url }, index) {
  const res = await fetch(url, { redirect: 'follow', signal: AbortSignal.timeout(20000) });
  if (!res.ok) throw new Error(`이미지 내려받기 실패(${res.status})`);
  const type = (res.headers.get('content-type') || '').split(';')[0].trim();
  if (!EXTENSIONS[type]) throw new Error(`이미지가 아님(${type || '형식 모름'})`);
  // 크기를 알려 주면 내려받기 전에 먼저 거른다
  const length = Number(res.headers.get('content-length') || 0);
  if (length > MAX_BYTES) {
    await res.body?.cancel().catch(() => {});
    throw new Error(`파일이 너무 큼(${Math.round(length / 1024 / 1024)}MB)`);
  }
  const bytes = Buffer.from(await res.arrayBuffer());
  if (bytes.length > MAX_BYTES) throw new Error(`파일이 너무 큼(${Math.round(bytes.length / 1024 / 1024)}MB)`);

  const headers = { Authorization: `Bearer ${token}`, 'Notion-Version': NOTION_VERSION };
  const filename = `image-${index + 1}.${EXTENSIONS[type]}`;
  const created = await fetch('https://api.notion.com/v1/file_uploads', {
    method: 'POST',
    headers: { ...headers, 'Content-Type': 'application/json' },
    body: JSON.stringify({ filename, content_type: type }),
  });
  const upload = await created.json();
  if (!created.ok) throw new Error(`노션 업로드 준비 실패(${created.status}): ${upload.message || ''}`);

  const form = new FormData();
  form.append('file', new Blob([bytes], { type }), filename);
  const sent = await fetch(`https://api.notion.com/v1/file_uploads/${upload.id}/send`, { method: 'POST', headers, body: form });
  if (!sent.ok) throw new Error(`노션 업로드 실패(${sent.status}): ${(await sent.json().catch(() => ({}))).message || ''}`);
  return upload.id;
}

// 이미지 목록 → 노션 블록 목록(같은 순서). 하나가 실패해도 나머지는 계속 만든다
async function imageBlocks(images, token, warn = (m) => console.error(m)) {
  const blocks = [];
  for (const [i, image] of images.entries()) {
    if (!token || !isGithubImage(image.url)) {
      blocks.push(linkBlock(image));
      continue;
    }
    try {
      const id = await uploadToNotion(token, image, i);
      blocks.push({ object: 'block', type: 'image', image: { type: 'file_upload', file_upload: { id }, caption: caption(image.alt) } });
    } catch (e) {
      warn(`이미지를 노션에 올리지 못해 링크로 대신 넣습니다(${image.url}): ${e.message}`);
      blocks.push(linkBlock(image));
    }
  }
  return blocks;
}

module.exports = { extractImages, stripImages, markImages, imageBlocks, MARKER };

// 디스커션용: 표준입력 본문 → {"body": 표시 줄을 넣은 본문, "images": 블록 목록}
if (require.main === module) {
  let input = '';
  process.stdin.setEncoding('utf8');
  process.stdin.on('data', (chunk) => (input += chunk));
  process.stdin.on('end', async () => {
    // 실패하면 원인을 로그에 남기고 0이 아닌 코드로 끝낸다 → build-notion-blocks.sh가 이미지 없이 원래 본문으로 변환
    try {
      const images = await imageBlocks(extractImages(input), process.env.NOTION_TOKEN, (m) => console.error(`::warning::${m}`));
      process.stdout.write(JSON.stringify({ body: markImages(input), images }));
    } catch (e) {
      console.error(`::warning::이미지 변환 중 오류: ${e.stack || e.message}`);
      process.exitCode = 1;
    }
  });
}
