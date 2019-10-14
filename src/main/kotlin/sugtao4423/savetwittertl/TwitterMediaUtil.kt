package sugtao4423.savetwittertl

import twitter4j.MediaEntity
import twitter4j.Status

class TwitterMediaUtil(status: Status) {

    val mediaUrls: ArrayList<String> = ArrayList()
    var content: String = status.text

    init {
        status.urlEntities?.map {
            content = content.replace(it.url, it.expandedURL)
        }

        status.mediaEntities?.map {
            if (it.isVideoOrGif()) {
                getVideoUrlSortByBitrate(status.mediaEntities)?.let { videoUrl ->
                    mediaUrls.add(videoUrl)
                }
            } else {
                mediaUrls.add(it.mediaURLHttps)
            }
            content = content.replace(it.url, "")
        }

        status.quotedStatusPermalink?.let {
            content = content.replace(it.url, it.expandedURL)
        }
    }

    private fun getVideoUrlSortByBitrate(mediaEntities: Array<MediaEntity>): String? {
        val mp4 = ArrayList<VideoUrl>()
        val webm = ArrayList<VideoUrl>()
        mediaEntities.map {
            if (it.isVideoOrGif()) {
                it.videoVariants.map { variant ->
                    val videoUrl = VideoUrl(variant.bitrate, variant.url)
                    when {
                        variant.isMP4() -> mp4.add(videoUrl)
                        variant.isWebm() -> webm.add(videoUrl)
                        else -> false
                    }
                }
                mp4.sort()
                webm.sort()
            }
        }

        return when {
            (mp4.isEmpty() && webm.isEmpty()) -> null
            mp4.isNotEmpty() -> mp4.last().url
            webm.isNotEmpty() -> webm.last().url
            else -> null
        }
    }

    private fun MediaEntity.isVideoOrGif(): Boolean {
        return (this.type == "video" || this.type == "animated_gif")
    }

    private fun MediaEntity.Variant.isMP4(): Boolean {
        return (this.contentType == "video/mp4")
    }

    private fun MediaEntity.Variant.isWebm(): Boolean {
        return (this.contentType == "video/webm")
    }

    private data class VideoUrl(
        val bitrate: Int,
        val url: String
    ) : Comparable<VideoUrl> {
        override fun compareTo(other: VideoUrl): Int {
            return this.bitrate - other.bitrate
        }
    }

}