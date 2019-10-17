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
        val videos = ArrayList<VideoUrl>()
        mediaEntities.map {
            if (it.isVideoOrGif()) {
                it.videoVariants.map { variant ->
                    videos.add(VideoUrl(variant.bitrate, variant.url))
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

    private data class VideoUrl(
        val bitrate: Int,
        val url: String
    ) : Comparable<VideoUrl> {
        override fun compareTo(other: VideoUrl): Int {
            return this.bitrate - other.bitrate
        }
    }

}