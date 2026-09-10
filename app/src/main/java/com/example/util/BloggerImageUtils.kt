package com.example.util

object BloggerImageUtils {

    /**
     * Ensures all Blogger image URLs use https:// scheme and handles relative // protocol prefixes.
     */
    fun sanitizeUrl(rawUrl: String?): String {
        if (rawUrl.isNullOrBlank()) return ""
        var url = rawUrl.trim()
        if (url.startsWith("//")) {
            url = "https:$url"
        } else if (url.startsWith("http://")) {
            url = "https://" + url.substring(7)
        }
        return url
    }

    /**
     * Formats a Blogger / Google User Content image URL for the requested quality mode.
     * When [dataSaverEnabled] is true, returns a low-quality/compressed variant (e.g. /s400/ or =s400).
     * When [dataSaverEnabled] is false, returns standard high-quality resolution (e.g. /s1600/ or =s1600).
     */
    fun getOptimizedUrl(rawUrl: String?, dataSaverEnabled: Boolean): String {
        val cleanUrl = sanitizeUrl(rawUrl)
        if (cleanUrl.isBlank()) return ""

        val targetPath = if (dataSaverEnabled) "/s400/" else "/s1600/"
        val targetParam = if (dataSaverEnabled) "=s400" else "=s1600"

        var result = cleanUrl
            // Replace legacy Blogger size segments in path
            .replace("/s72-c/", targetPath)
            .replace("/s72-w/", targetPath)
            .replace("/w72-h72-p-k-no-nu/", targetPath)
            .replace("/s1600/", targetPath)
            .replace("/s1600-rw/", targetPath)
            .replace(Regex("/s[0-9]+(-[cwhp][0-9]*)*(-rw)?/"), targetPath)
            .replace(Regex("/w[0-9]+-h[0-9]+(-[cwhp][0-9]*)*(-rw)?/"), targetPath)

        // Handle googleusercontent/blogspot parameter format (e.g., =s1600 or =w640-h400)
        if (result.contains("googleusercontent.com") || result.contains("blogspot.com")) {
            result = result
                .replace(Regex("=s[0-9]+(-[cwhp][0-9]*)*"), targetParam)
                .replace(Regex("=w[0-9]+-h[0-9]+(-[cwhp][0-9]*)*"), targetParam)
        }

        return result
    }
}
