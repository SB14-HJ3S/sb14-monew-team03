
const NOTION_VERSION = '2022-06-28';
const MAX_BYTES = 5 * 1024 * 1024;
const MARKER = (i) => `[[NOTION_IMAGE:${i}]]`;

const IMAGE_PATTERN = /<img\b[^>]*>|!\[([^\]]*)\]\(\s*<?([^)\s>]+)>?(?:\s+"[^"]*")?\s*\)/gi;

function parseMatch(match) {
  if (match[0].startsWith('<')) {
    const attr = (name) => match[0].match(new RegExp(`\\b${name}\\s*=\\s*["']([^"']*)["']`, 'i'))?.[1] || '';
    return { url: attr('src'), alt: attr('alt') };
  }
  return { url: match[2], alt: match[1] };
}

const isWebUrl = (url) => /^https?:\/\//i.test(url);

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

const stripImages = (text) => outsideCode(text, (part) => part.replace(IMAGE_PATTERN, '')).replace(/\n{3,}/g, '\n\n');

function markImages(text) {
  let i = 0;
  return outsideCode(text, (part) =>
    part.replace(IMAGE_PATTERN, (...args) => (isWebUrl(parseMatch(args).url) ? `\n\n${MARKER(i++)}\n\n` : '')),
  );
}

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

if (require.main === module) {
  let input = '';
  process.stdin.setEncoding('utf8');
  process.stdin.on('data', (chunk) => (input += chunk));
  process.stdin.on('end', async () => {
    try {
      const images = await imageBlocks(extractImages(input), process.env.NOTION_TOKEN, (m) => console.error(`::warning::${m}`));
      process.stdout.write(JSON.stringify({ body: markImages(input), images }));
    } catch (e) {
      console.error(`::warning::이미지 변환 중 오류: ${e.stack || e.message}`);
      process.exitCode = 1;
    }
  });
}
