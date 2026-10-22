// 디스커션 본문(마크다운)을 노션 블록으로 바꾼다 — GitHub 화면과 같은 모양이 되게
// - 제목(#·##·###), 목록(-·1.·들여쓴 목록), 표, 굵게·기울임·취소선·`코드`·링크, 코드 블록, 인용, 구분선(---)
// - HTML로 붙여 넣은 부분(<h1>, <ul>, <table> 등)은 먼저 마크다운으로 바꾼 뒤 같은 방식으로 변환
// - 이미지 자리 표시 줄([[NOTION_IMAGE:번호]])은 notion-images.js가 만든 이미지 블록으로 바꾼다
//
// 쓰는 곳: build-notion-blocks.sh
//   표준입력: 노션 블록 배열(JSON). 그중 {"__markdown": "내용", "__fallback": [블록…]} 항목만 변환해서 펼친다
//   환경변수 NOTION_IMAGES: 이미지 블록 배열(JSON)
//   표준출력: 노션 블록 배열(JSON)
// 한 칸 변환이 실패하면 그 칸만 __fallback(예전 방식: 문단·글머리 목록)으로 넣는다

const { Lexer } = require('marked');
const TurndownService = require('turndown');
const { gfm } = require('turndown-plugin-gfm');

const TEXT_LIMIT = 1900; // 노션 글 조각 하나 최대 2000자
const RICH_TEXT_LIMIT = 100; // 블록 하나에 넣을 수 있는 글 조각 수
const MAX_DEPTH = 2; // 노션 API는 요청 한 번에 블록을 2단까지만 겹쳐 넣을 수 있다
const IMAGE_MARKER = /^\[\[NOTION_IMAGE:(\d+)\]\]$/;

// 노션 코드 블록이 받는 언어 이름. 목록에 없으면 plain text
const LANGUAGES = {
  java: 'java', kotlin: 'kotlin', groovy: 'groovy', gradle: 'groovy', js: 'javascript', javascript: 'javascript',
  ts: 'typescript', typescript: 'typescript', json: 'json', bash: 'bash', sh: 'shell', shell: 'shell', zsh: 'shell',
  sql: 'sql', yaml: 'yaml', yml: 'yaml', xml: 'xml', html: 'html', css: 'css', python: 'python', py: 'python',
  markdown: 'markdown', md: 'markdown', diff: 'diff', dockerfile: 'docker', docker: 'docker', http: 'plain text',
};

const turndown = new TurndownService({ headingStyle: 'atx', bulletListMarker: '-', codeBlockStyle: 'fenced', hr: '---' });
turndown.use(gfm);
turndown.remove(['head', 'style', 'script']);

const LEX_OPTIONS = { gfm: true, breaks: true }; // GitHub 디스커션은 줄바꿈 한 번도 그대로 줄바꿈

// ---------- 글(rich text) ----------

const plain = (content, annotations = {}, link = null) => ({ content, annotations, link });

// 인라인 토큰 → [{content, annotations, link}]
function inline(tokens, annotations = {}, link = null) {
  const out = [];
  for (const token of tokens || []) {
    switch (token.type) {
      case 'strong':
        out.push(...inline(token.tokens, { ...annotations, bold: true }, link));
        break;
      case 'em':
        out.push(...inline(token.tokens, { ...annotations, italic: true }, link));
        break;
      case 'del':
        out.push(...inline(token.tokens, { ...annotations, strikethrough: true }, link));
        break;
      case 'codespan':
        out.push(plain(decode(token.text), { ...annotations, code: true }, link));
        break;
      case 'link': {
        const url = /^(https?:|mailto:)/i.test(token.href) ? token.href : null;
        out.push(...inline(token.tokens, annotations, url || link));
        break;
      }
      case 'br':
        out.push(plain('\n', annotations, link));
        break;
      case 'image':
        // 이미지는 notion-images.js가 따로 처리. 여기까지 온 건(주소가 이상한 것) 설명 글자만
        if (token.text) out.push(plain(token.text, annotations, link));
        break;
      case 'html':
        out.push(...inlineHtml(token.text, annotations, link));
        break;
      case 'text':
      case 'escape':
        if (token.tokens && token.tokens.length) out.push(...inline(token.tokens, annotations, link));
        else out.push(plain(decode(token.text), annotations, link));
        break;
      default:
        if (token.tokens) out.push(...inline(token.tokens, annotations, link));
        else if (token.text) out.push(plain(decode(token.text), annotations, link));
    }
  }
  return out;
}

// 글 중간의 HTML 태그(<br>, <b> 등): 줄바꿈만 살리고 태그 글자는 지운다
function inlineHtml(html, annotations, link) {
  if (/^<br\s*\/?>$/i.test(html.trim())) return [plain('\n', annotations, link)];
  const text = decode(html.replace(/<[^>]*>/g, ''));
  return text ? [plain(text, annotations, link)] : [];
}

function decode(text) {
  return String(text)
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&');
}

// [{content, annotations, link}] → 노션 rich_text (긴 글은 1900자씩 나누고, 같은 서식끼리는 합침)
function richText(parts) {
  const merged = [];
  for (const part of parts) {
    if (!part.content) continue;
    const last = merged[merged.length - 1];
    if (last && same(last, part)) last.content += part.content;
    else merged.push({ ...part });
  }
  const out = [];
  for (const part of merged) {
    for (let i = 0; i < part.content.length; i += TEXT_LIMIT) {
      const text = { content: part.content.slice(i, i + TEXT_LIMIT) };
      if (part.link) text.link = { url: part.link };
      const item = { type: 'text', text };
      const annotations = Object.fromEntries(Object.entries(part.annotations).filter(([, v]) => v));
      if (Object.keys(annotations).length) item.annotations = annotations;
      out.push(item);
    }
  }
  // 조각이 너무 많으면 뒤쪽은 서식 없이 하나로 합친다
  if (out.length > RICH_TEXT_LIMIT) {
    const rest = out.splice(RICH_TEXT_LIMIT - 1).map((t) => t.text.content).join('');
    out.push({ type: 'text', text: { content: rest.slice(0, TEXT_LIMIT) } });
  }
  return out;
}

const same = (a, b) => a.link === b.link && JSON.stringify(a.annotations) === JSON.stringify(b.annotations);

// 앞뒤 빈 줄 정리
function trimParts(parts) {
  const out = parts.map((p) => ({ ...p }));
  while (out.length && !out[0].content.replace(/^\s+/, '')) out.shift();
  while (out.length && !out[out.length - 1].content.replace(/\s+$/, '')) out.pop();
  if (out.length) {
    out[0].content = out[0].content.replace(/^\s+/, '');
    out[out.length - 1].content = out[out.length - 1].content.replace(/\s+$/, '');
  }
  return out;
}

// 글 블록 하나 (글이 비면 블록을 만들지 않음)
function textBlocks(type, parts, extra = {}) {
  const rich = richText(trimParts(parts));
  if (!rich.length) return [];
  return [{ object: 'block', type, [type]: { rich_text: rich, ...extra } }];
}

// ---------- 블록 ----------

let images = [];

function blocks(tokens, depth = 1) {
  const out = [];
  for (const token of tokens) out.push(...block(token, depth));
  return out;
}

function block(token, depth) {
  switch (token.type) {
    case 'space':
    case 'def':
      return [];
    case 'heading': {
      const level = Math.min(token.depth, 3);
      return textBlocks(`heading_${level}`, inline(token.tokens));
    }
    case 'paragraph': {
      const marker = token.text.trim().match(IMAGE_MARKER);
      if (marker) return images[Number(marker[1])] ? [images[Number(marker[1])]] : [];
      return textBlocks('paragraph', inline(token.tokens));
    }
    case 'text':
      return textBlocks('paragraph', inline(token.tokens || [{ type: 'text', text: token.text }]));
    case 'hr':
      return [{ object: 'block', type: 'divider', divider: {} }];
    case 'code': {
      const lang = LANGUAGES[(token.lang || '').trim().toLowerCase()] || 'plain text';
      return textBlocks('code', [plain(token.text)], { language: lang });
    }
    case 'blockquote': {
      // 인용 안 문단들은 줄바꿈으로 이어 한 블록에
      const parts = [];
      for (const child of token.tokens) {
        if (parts.length) parts.push(plain('\n'));
        parts.push(...inline(child.tokens || [{ type: 'text', text: child.text || child.raw }]));
      }
      return textBlocks('quote', parts);
    }
    case 'list':
      return list(token, depth);
    case 'table':
      return table(token, depth);
    case 'html':
      return html(token.text, depth);
    default:
      return token.raw && token.raw.trim() ? textBlocks('paragraph', [plain(token.raw)]) : [];
  }
}

function list(token, depth) {
  const out = [];
  for (const item of token.items) {
    // 체크 목록(- [x])은 노션 할 일 블록으로
    const type = item.task ? 'to_do' : token.ordered ? 'numbered_list_item' : 'bulleted_list_item';
    // 목록 항목의 첫 글 = 항목 글, 나머지(하위 목록·추가 문단) = 하위 블록
    let first = null;
    const rest = [];
    for (const child of item.tokens) {
      if (child.type === 'checkbox') continue;
      if (!first && (child.type === 'text' || child.type === 'paragraph')) first = child;
      else rest.push(child);
    }
    const parts = first ? inline(first.tokens || [{ type: 'text', text: first.text }]) : [];
    const extra = item.task ? { checked: Boolean(item.checked) } : {};
    const [itemBlock] = textBlocks(type, parts.length ? parts : [plain(' ')], extra);
    if (!itemBlock) continue;
    const children = blocks(rest, depth + 1);
    out.push(...nest(itemBlock, children, depth));
  }
  return out;
}

// 노션은 2단까지만 겹쳐 넣을 수 있어서, 그보다 깊은 하위 블록은 같은 높이로 펼친다
function nest(parent, children, depth) {
  if (!children.length) return [parent];
  if (depth < MAX_DEPTH) {
    parent[parent.type].children = children;
    return [parent];
  }
  return [parent, ...children];
}

function table(token, depth) {
  const header = token.header.map((cell) => inline(cell.tokens));
  const rows = token.rows.map((row) => row.map((cell) => inline(cell.tokens)));
  const width = Math.max(header.length, ...rows.map((r) => r.length));
  // 목록 안의 표는 노션에 겹쳐 넣을 수 없어서 줄 글로 넣는다
  if (depth >= MAX_DEPTH) {
    return [header, ...rows].flatMap((cells) =>
      textBlocks('paragraph', cells.flatMap((c, i) => (i ? [plain(' | '), ...c] : c))),
    );
  }
  const row = (cells) => ({
    object: 'block',
    type: 'table_row',
    table_row: { cells: Array.from({ length: width }, (_, i) => richText(trimParts(cells[i] || []))) },
  });
  return [
    {
      object: 'block',
      type: 'table',
      table: { table_width: width, has_column_header: true, has_row_header: false, children: [row(header), ...rows.map(row)] },
    },
  ];
}

// HTML로 붙여 넣은 부분 → 마크다운으로 바꿔서 다시 변환
function html(source, depth) {
  const trimmed = source.trim();
  if (/^<!--[\s\S]*-->$/.test(trimmed)) return []; // 주석은 GitHub에서도 안 보임
  const markdown = turndown.turndown(source).trim();
  if (!markdown) return [];
  const tokens = new Lexer(LEX_OPTIONS).lex(markdown);
  // 바꾼 결과에 또 HTML만 남으면(노션에 없는 태그) 태그를 지운 글로
  if (tokens.every((t) => t.type === 'html' || t.type === 'space')) {
    return textBlocks('paragraph', [plain(decode(markdown.replace(/<[^>]*>/g, '')))]);
  }
  return blocks(tokens, depth);
}

function convert(markdown) {
  return blocks(new Lexer(LEX_OPTIONS).lex(markdown));
}

// ---------- 실행 ----------

function main() {
  images = JSON.parse(process.env.NOTION_IMAGES || '[]');
  const input = JSON.parse(require('fs').readFileSync(0, 'utf8'));
  const output = [];
  for (const item of input) {
    if (item && typeof item.__markdown === 'string') {
      try {
        output.push(...convert(item.__markdown));
      } catch (error) {
        process.stderr.write(`::warning::본문 일부를 서식 없이 넣습니다: ${error.message}\n`);
        output.push(...(item.__fallback || []));
      }
    } else {
      output.push(item);
    }
  }
  process.stdout.write(JSON.stringify(output));
}

if (require.main === module) main();

module.exports = { convert };
