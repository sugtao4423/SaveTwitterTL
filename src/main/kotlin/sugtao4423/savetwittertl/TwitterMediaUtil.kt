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
            if (isVideoOrGif(it)) {
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
            if (isVideoOrGif(it)) {
                it.videoVariants.map { variant ->
                    if (variant.contentType == "video/mp4") {
                        videos.add(VideoURLs(variant.bitrate, variant.url))
                    }
                }
                if (videos.isEmpty()) {
                    it.videoVariants.map { variant ->
                        if (variant.contentType == "video/mp4" || variant.contentType == "video/webm") {
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

    private fun isVideoOrGif(mediaEntity: MediaEntity): Boolean {
        return (mediaEntity.type == "video" || mediaEntity.type == "animated_gif")
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