
const { Lexer } = require('marked');
const TurndownService = require('turndown');
const { gfm } = require('turndown-plugin-gfm');

const TEXT_LIMIT = 1900;
const RICH_TEXT_LIMIT = 100;
const MAX_DEPTH = 2;
const MAX_CHILDREN = 100;
const IMAGE_MARKER = /^\[\[NOTION_IMAGE:(\d+)\]\]$/;

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


const plain = (content, annotations = {}, link = null) => ({ content, annotations, link });


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

  if (out.length > RICH_TEXT_LIMIT) {
    const rest = out.splice(RICH_TEXT_LIMIT - 1).map((t) => t.text.content).join('');
    out.push({ type: 'text', text: { content: rest.slice(0, TEXT_LIMIT) } });
  }
  return out;
}

const same = (a, b) => a.link === b.link && JSON.stringify(a.annotations) === JSON.stringify(b.annotations);

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

function textBlocks(type, parts, extra = {}) {
  const rich = richText(trimParts(parts));
  if (!rich.length) return [];
  return [{ object: 'block', type, [type]: { rich_text: rich, ...extra } }];
}


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
      const rich = richText([plain(token.text.replace(/\n+$/, ''))]);
      return rich.length ? [{ object: 'block', type: 'code', code: { rich_text: rich, language: lang } }] : [];
    }
    case 'blockquote': {
      const parts = [];
      const rest = [];
      for (const child of token.tokens) {
        if (child.type !== 'paragraph' && child.type !== 'text') {
          rest.push(child);
          continue;
        }
        if (parts.length) parts.push(plain('\n'));
        parts.push(...inline(child.tokens || [{ type: 'text', text: child.text }]));
      }
      const children = blocks(rest, depth + 1);
      const [quote] = textBlocks('quote', parts);
      return quote ? nest(quote, children, depth) : children;
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

    const type = item.task ? 'to_do' : token.ordered ? 'numbered_list_item' : 'bulleted_list_item';

    let first = null;
    const rest = [];
    for (const child of item.tokens) {
      if (child.type === 'checkbox') continue;
      if (!first && (child.type === 'text' || child.type === 'paragraph')) first = child;
      else rest.push(child);
    }
    const parts = first ? inline(first.tokens || [{ type: 'text', text: first.text }]) : [];
    const extra = item.task ? { checked: Boolean(item.checked) } : {};

    const [itemBlock = { object: 'block', type, [type]: { rich_text: [], ...extra } }] = textBlocks(type, parts, extra);
    const children = blocks(rest, depth + 1);
    out.push(...nest(itemBlock, children, depth));
  }
  return out;
}



function nest(parent, children, depth) {
  if (!children.length) return [parent];
  if (depth < MAX_DEPTH) {
    parent[parent.type].children = children.slice(0, MAX_CHILDREN);
    return [parent, ...children.slice(MAX_CHILDREN)];
  }
  return [parent, ...children];
}

function table(token, depth) {
  const header = token.header.map((cell) => inline(cell.tokens));
  const rows = token.rows.map((row) => row.map((cell) => inline(cell.tokens)));
  const width = Math.max(header.length, ...rows.map((r) => r.length));

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

  const tables = [];
  for (let i = 0; i === 0 || i < rows.length; i += MAX_CHILDREN - 1) {
    tables.push({
      object: 'block',
      type: 'table',
      table: {
        table_width: width,
        has_column_header: true,
        has_row_header: false,
        children: [row(header), ...rows.slice(i, i + MAX_CHILDREN - 1).map(row)],
      },
    });
  }
  return tables;
}


function html(source, depth) {
  const trimmed = source.trim();
  if (/^<!--[\s\S]*-->$/.test(trimmed)) return []; // 주석은 GitHub에서도 안 보임
  const markdown = turndown.turndown(source).trim();
  if (!markdown) return [];
  const tokens = new Lexer(LEX_OPTIONS).lex(markdown);

  if (tokens.every((t) => t.type === 'html' || t.type === 'space')) {
    return textBlocks('paragraph', [plain(decode(markdown.replace(/<[^>]*>/g, '')))]);
  }
  return blocks(tokens, depth);
}

function convert(markdown) {
  return blocks(new Lexer(LEX_OPTIONS).lex(markdown));
}



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
