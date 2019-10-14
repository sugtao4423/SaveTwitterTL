package sugtao4423.savetwittertl

import twitter4j.*
import java.util.*

class AutoLoadTLTask(private val twitter: Twitter, private val listAsTL: Long, private val listener: OnStatusListener) :
    TimerTask() {

    interface OnStatusListener {
        fun onStatus(statuses: ResponseList<Status>)
    }

    private var latestTweetId = -1L

    override fun run() {
        try {
            var paging = Paging(1, 200).also {
                if (latestTweetId > 0) {
                    it.sinceId(latestTweetId - 1)
                }
            }
            var statuses: ResponseList<Status>
            if (listAsTL < 0) {
                statuses = twitter.getHomeTimeline(paging)
            } else {
                statuses = twitter.getUserListStatuses(listAsTL, paging)

                for (i in 2..10) {
                    if (statuses.isEmpty()) {
                        break
                    }
                    if (isIncludeLatestTweetId(statuses)) {
                        statuses = removeLatestTweet(statuses)
                        break
                    }
                    paging = Paging(i, 200).also {
                        if (latestTweetId > 0) {
                            it.sinceId(latestTweetId - 1)
                        }
                    }
                    statuses.addAll(twitter.getUserListStatuses(listAsTL, paging))
                }
            }

            if (statuses.isNotEmpty()) {
                statuses.sort()
                latestTweetId = statuses.last().id
                listener.onStatus(statuses)
            }
        } catch (e: TwitterException) {
        }
    }

    private fun isIncludeLatestTweetId(statuses: ResponseList<Status>): Boolean {
        statuses.map {
            if (it.id == latestTweetId) {
                return true
            }
        }
        return false
    }

    private fun removeLatestTweet(statuses: ResponseList<Status>): ResponseList<Status> {
        statuses.sort()
        statuses.reverse()
        for (i in (statuses.size - 1) downTo 0) {
            if (statuses[i].id <= latestTweetId) {
                statuses.removeAt(i)
            }
        }
        return statuses
    }

}