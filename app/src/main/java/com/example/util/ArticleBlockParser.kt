package com.example.util

import java.util.regex.Pattern

sealed class ArticleBlock {
    data class TextBlock(val html: String) : ArticleBlock()
    data class ImageBlock(val imageUrl: String, val caption: String? = null) : ArticleBlock()
    data class VideoBlock(val videoId: String) : ArticleBlock()
}

object ArticleBlockParser {

    /**
     * Parses Blogger HTML content into a chronological list of Text, Image, and Video blocks.
     * Preserves natural article layout (Text -> Photo -> Text -> Photo) instead of separating them.
     */
    fun parse(rawHtml: String, plainExcerpt: String = ""): List<ArticleBlock> {
        if (rawHtml.isBlank()) {
            return if (plainExcerpt.isNotBlank()) {
                listOf(ArticleBlock.TextBlock(plainExcerpt))
            } else {
                emptyList()
            }
        }

        val blocks = mutableListOf<ArticleBlock>()

        // Regex to match Blogger image containers, standalone img tags, or YouTube iframes
        // 1. Blogger table caption container: <table[^>]*class="[^"]*tr-caption-container[^"]*"[^>]*>.*?</table>
        // 2. Blogger anchor wrapping img: <a[^>]*href="[^"]*"[^>]*>\s*<img[^>]*src="([^"]+)"[^>]*>\s*</a>
        // 3. Standalone img: <img[^>]*src="([^"]+)"[^>]*>
        // 4. YouTube iframe: <iframe[^>]*src="[^"]*(?:youtube\.com/embed/|youtu\.be/)([^"?&]+)[^"]*"[^>]*>.*?</iframe>
        val pattern = Pattern.compile(
            "(?is)(<table[^>]*class=\"[^\"]*tr-caption-container[^\"]*\"[^>]*>.*?</table>)|" +
            "(<a[^>]*href=\"[^\"]*\"[^>]*>\\s*<img[^>]*src=\"([^\"]+)\"[^>]*>.*?</a>)|" +
            "(<img[^>]*src=\"([^\"]+)\"[^>]*>)|" +
            "(<iframe[^>]*src=\"[^\"]*(?:youtube\\.com/embed/|youtu\\.be/)([^\"?&]+)[^\"]*\"[^>]*>.*?</iframe>)"
        )

        val matcher = pattern.matcher(rawHtml)
        var lastEnd = 0

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            // Extract text before this block
            if (start > lastEnd) {
                val textSegment = rawHtml.substring(lastEnd, start)
                val cleanTextSegment = cleanHtmlSegment(textSegment)
                if (cleanTextSegment.isNotBlank()) {
                    blocks.add(ArticleBlock.TextBlock(cleanTextSegment))
                }
            }

            val fullMatch = matcher.group()

            when {
                // Blogger Caption Table
                fullMatch.contains("tr-caption-container", ignoreCase = true) -> {
                    val imgMatcher = Pattern.compile("(?i)<img[^>]*src=\"([^\"]+)\"").matcher(fullMatch)
                    val imgUrl = if (imgMatcher.find()) BloggerImageUtils.sanitizeUrl(imgMatcher.group(1)) else ""
                    val captionMatcher = Pattern.compile("(?is)<td[^>]*class=\"[^\"]*tr-caption[^\"]*\"[^>]*>(.*?)</td>").matcher(fullMatch)
                    val caption = if (captionMatcher.find()) {
                        captionMatcher.group(1).replace("<[^>]*>".toRegex(), "").trim()
                    } else null

                    if (imgUrl.isNotBlank()) {
                        blocks.add(ArticleBlock.ImageBlock(imgUrl, caption.takeIf { !it.isNullOrBlank() }))
                    }
                }

                // YouTube Iframe
                fullMatch.contains("youtube.com/embed/", ignoreCase = true) || fullMatch.contains("youtu.be/", ignoreCase = true) -> {
                    val vidMatcher = Pattern.compile("(?i)(?:youtube\\.com/embed/|youtu\\.be/)([a-zA-Z0-9_-]{11})").matcher(fullMatch)
                    if (vidMatcher.find()) {
                        val vidId = vidMatcher.group(1)
                        if (!vidId.isNullOrBlank()) {
                            blocks.add(ArticleBlock.VideoBlock(vidId))
                        }
                    }
                }

                // Image inside <a> or standalone <img>
                else -> {
                    val imgMatcher = Pattern.compile("(?i)<img[^>]*src=\"([^\"]+)\"").matcher(fullMatch)
                    if (imgMatcher.find()) {
                        val rawUrl = imgMatcher.group(1)
                        val sanitized = BloggerImageUtils.sanitizeUrl(rawUrl)
                        if (sanitized.isNotBlank()) {
                            blocks.add(ArticleBlock.ImageBlock(sanitized, null))
                        }
                    }
                }
            }

            lastEnd = end
        }

        // Remaining text after last match
        if (lastEnd < rawHtml.length) {
            val textSegment = rawHtml.substring(lastEnd)
            val cleanTextSegment = cleanHtmlSegment(textSegment)
            if (cleanTextSegment.isNotBlank()) {
                blocks.add(ArticleBlock.TextBlock(cleanTextSegment))
            }
        }

        // Fallback if no blocks were produced
        if (blocks.isEmpty()) {
            val fallback = if (plainExcerpt.isNotBlank()) plainExcerpt else rawHtml.replace("<[^>]*>".toRegex(), "").trim()
            if (fallback.isNotBlank()) {
                blocks.add(ArticleBlock.TextBlock(fallback))
            }
        }

        return blocks
    }

    private fun cleanHtmlSegment(html: String): String {
        // Strip empty paragraph/div wrappers that only contained whitespace or line breaks
        val textOnly = html
            .replace("&nbsp;", " ")
            .replace("<[^>]*>".toRegex(), "")
            .trim()
        return if (textOnly.isBlank()) "" else html.trim()
    }
}
