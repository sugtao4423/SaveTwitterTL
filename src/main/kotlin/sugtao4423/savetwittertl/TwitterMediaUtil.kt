package sugtao4423.savetwittertl

import twitter4j.MediaEntity
import twitter4j.Status
import java.util.*

class TwitterMediaUtil(status: Status) {

    val mediaUrls: ArrayList<String> = ArrayList()
    var content: String = status.text

    init {
        status.urlEntities?.map {
            content = content.replace(it.url, it.expandedURL)
        }

        status.mediaEntities?.map {
            if (it.isVideoOrGif()) {
                val videoUrl = getVideoURLsSortByBitrate(status.mediaEntities)
                if (videoUrl != null) {
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

    private fun getVideoURLsSortByBitrate(mediaEntities: Array<MediaEntity>): String? {
        val videos = ArrayList<VideoURLs>()
        mediaEntities.map {
            if (it.isVideoOrGif()) {
                it.videoVariants.map { variant ->
                    if (variant.isMP4()) {
                        videos.add(VideoURLs(variant.bitrate, variant.url))
                    }
                }
                if (videos.isEmpty()) {
                    it.videoVariants.map { variant ->
                        if (variant.isMP4() || variant.isWebm()) {
                            videos.add(VideoURLs(variant.bitrate, variant.url))
                        }
                    }
                }
                videos.sort()
            }
        }
        return if (videos.isEmpty()) {
            null
        } else {
            videos.last().url
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

    private data class VideoURLs(
        val bitrate: Int,
        val url: String
    ) : Comparable<VideoURLs> {
        override fun compareTo(other: VideoURLs): Int {
            return this.bitrate - other.bitrate
        }
    }

}