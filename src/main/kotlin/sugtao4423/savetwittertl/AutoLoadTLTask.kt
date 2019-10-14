package sugtao4423.savetwittertl

import twitter4j.*
import java.util.*

class AutoLoadTLTask(private val twitter: Twitter, private val listAsTL: Long, private val listener: OnStatusListener) :
    TimerTask() {

    interface OnStatusListener {
        fun onStatus(statuses: List<Status>)
    }

    private var latestTweetId = -1L

    private fun getPaging(page: Int): Paging {
        return Paging(page, 200).also {
            if (latestTweetId > 0) {
                it.sinceId(latestTweetId - 1)
            }
        }
    }

    override fun run() {
        try {
            val statuses = ArrayList<Status>()
            if (listAsTL < 0) {
                statuses.addAll(twitter.getHomeTimeline(getPaging(1)))
            } else {
                statuses.addAll(twitter.getUserListStatuses(listAsTL, getPaging(1)))

                for (i in 2..10) {
                    if (statuses.isEmpty()) {
                        break
                    }
                    if (isIncludeLatestTweetId(statuses)) {
                        removeLatestTweet(statuses)
                        break
                    }
                    statuses.addAll(twitter.getUserListStatuses(listAsTL, getPaging(i)))
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

    private fun isIncludeLatestTweetId(statuses: ArrayList<Status>): Boolean {
        statuses.map {
            if (it.id == latestTweetId) {
                return true
            }
        }
        return false
    }

    private fun removeLatestTweet(statuses: ArrayList<Status>) {
        statuses.sort()
        statuses.reverse()
        for (i in (statuses.size - 1) downTo 0) {
            if (statuses[i].id <= latestTweetId) {
                statuses.removeAt(i)
            }
        }
    }

}