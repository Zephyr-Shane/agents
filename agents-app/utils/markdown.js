/**
 * 简化的 Markdown → HTML 转换（面向 AI 对话常见的格式）
 * 输出携带内联样式，兼容微信小程序 rich-text
 */
export function mdToHtml(text) {
  if (!text) return ''

  const codeBlocks = []
  let html = text.replace(/```(\w*)\n?([\s\S]*?)```/g, (_, lang, code) => {
    const idx = codeBlocks.length
    codeBlocks.push(
      `<pre style="background:#f5f5f5;border-radius:12rpx;padding:20rpx;overflow-x:auto;margin:12rpx 0;font-size:24rpx;line-height:1.5;white-space:pre-wrap;word-break:break-all;font-family:monospace;">${escapeHtml(code.trim())}</pre>`
    )
    return `%%CODEBLOCK_${idx}%%`
  })

  html = escapeHtml(html)
  html = html.replace(/%%CODEBLOCK_(\d+)%%/g, (_, idx) => codeBlocks[+idx])

  html = html.replace(/`([^`]+)`/g,
    '<code style="background:#f0f0f0;border-radius:6rpx;padding:2rpx 8rpx;font-size:24rpx;font-family:monospace;">$1</code>')

  html = html.replace(/^#### (.+)$/gm, '<h4 style="margin:16rpx 0 8rpx;font-weight:600;font-size:28rpx;">$1</h4>')
  html = html.replace(/^### (.+)$/gm, '<h3 style="margin:16rpx 0 8rpx;font-weight:600;font-size:30rpx;">$1</h3>')
  html = html.replace(/^## (.+)$/gm, '<h2 style="margin:16rpx 0 8rpx;font-weight:600;font-size:32rpx;">$1</h2>')
  html = html.replace(/^# (.+)$/gm, '<h1 style="margin:16rpx 0 8rpx;font-weight:600;font-size:36rpx;">$1</h1>')

  html = html.replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>')
  html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  html = html.replace(/\*(.+?)\*/g, '<em>$1</em>')

  html = html.replace(/\[([^\]]+)\]\(([^)]+)\)/g,
    '<a href="$2" style="color:#667EEA;text-decoration:underline;">$1</a>')

  html = html.replace(/^- (.+)$/gm, '<li style="margin:4rpx 0;">$1</li>')
  html = html.replace(/(<li[^>]*>.*<\/li>\n?)+/g,
    '<ul style="padding-left:40rpx;margin:8rpx 0;">$&</ul>')
  html = html.replace(/^\d+\. (.+)$/gm, '<li style="margin:4rpx 0;">$1</li>')

  const lines = html.split('\n')
  const result = []
  let inParagraph = false
  for (const line of lines) {
    const t = line.trim()
    if (!t) {
      if (inParagraph) { result.push('</p>'); inParagraph = false }
      continue
    }
    if (/^<(h[1-4]|ul|ol|pre|p)/.test(t)) {
      if (inParagraph) { result.push('</p>'); inParagraph = false }
      result.push(line)
      continue
    }
    if (!inParagraph) { result.push('<p style="margin:8rpx 0;">'); inParagraph = true }
    result.push(line)
  }
  if (inParagraph) result.push('</p>')

  return result.join('\n')
}

function escapeHtml(str) {
  return str.replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}
