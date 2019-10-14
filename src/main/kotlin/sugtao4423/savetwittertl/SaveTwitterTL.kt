package sugtao4423.savetwittertl

import twitter4j.*
import twitter4j.auth.AccessToken
import twitter4j.conf.ConfigurationBuilder
import java.io.File
import java.io.IOException
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement
import java.text.SimpleDateFormat
import java.util.*

fun main(@Suppress("UnusedMainParameter") args: Array<String>) {
    SaveTwitterTL().main()
}

class SaveTwitterTL {

    private lateinit var conn: Connection
    private lateinit var stmt: Statement

    @Throws(ClassNotFoundException::class, IOException::class, SQLException::class)
    fun main() {
        prepareSQL()
        val twitter = prepareTwitter()

        val onStatusListener = object : AutoLoadTLTask.OnStatusListener {
            override fun onStatus(statuses: ResponseList<Status>) {
                statuses.map {
                    if (!it.isRetweet) {
                        onTweet(it)
                    }
                }
            }
        }
        val task = AutoLoadTLTask(twitter, Config.LIST_AS_TL, onStatusListener)
        val timer = Timer(false)
        timer.schedule(task, 0, Config.TL_LOAD_INTERVAL_SECONDS * 1000L)

        Runtime.getRuntime().addShutdownHook(Thread {
            try {
                timer.cancel()
                timer.purge()
                stmt.close()
                conn.close()
            } catch (e: SQLException) {
            }
        })
    }

    @Throws(ClassNotFoundException::class, IOException::class, SQLException::class)
    private fun prepareSQL() {
        Class.forName("org.sqlite.JDBC")
        val location = Config.SAVE_PATH + "/TwitterTL.sqlite3"
        val db = File(location)
        if (!db.exists()) {
            db.createNewFile()
        }
        val prop = Properties()
        prop["foreign_keys"] = "on"
        conn = DriverManager.getConnection("jdbc:sqlite:$location", prop)
        stmt = conn.createStatement()
        stmt.execute("CREATE TABLE IF NOT EXISTS userlist(id INTEGER UNIQUE, screen_name TEXT)")
        stmt.execute("CREATE TABLE IF NOT EXISTS vialist(id INTEGER PRIMARY KEY AUTOINCREMENT, via TEXT UNIQUE)")
        stmt.execute("CREATE TABLE IF NOT EXISTS tweets(content TEXT, user_id INTEGER NOT NULL, date TEXT, via_id INTEGER NOT NULL, medias TEXT, tweetId INTEGER UNIQUE, FOREIGN KEY (user_id) REFERENCES userlist(id), FOREIGN KEY (via_id) REFERENCES vialist(id))")
    }

    private fun prepareTwitter(): Twitter {
        val conf = ConfigurationBuilder().run {
            setOAuthConsumerKey(Config.CONSUMER_KEY)
            setOAuthConsumerSecret(Config.CONSUMER_SECRET)
            setTweetModeExtended(true)
            build()
        }
        val accessToken = AccessToken(Config.ACCESS_TOKEN, Config.ACCESS_TOKEN_SECRET)
        return TwitterFactory(conf).getInstance(accessToken)
    }

    private fun onTweet(status: Status) {
        var notSave = false
        Config.NOT_SAVE_USER.map {
            if (it == status.user.screenName) {
                notSave = true
            }
        }
        if (notSave) {
            return
        }

        val mediaUtil = TwitterMediaUtil(status)
        val content = mediaUtil.content
        val mediaUrls = mediaUtil.mediaUrls
        val medias = implode(mediaUrls, ",")
        val date = SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z").format(status.createdAt)
        val via = status.source.replace(Regex("<.+?>"), "")

        insertTweet(status.user, content, date, via, medias, status.id)
    }

    private fun insertTweet(user: User, content: String, date: String, via: String, medias: String, tweetId: Long) {
        val sql = "INSERT INTO tweets VALUES(" +
                "'${content.replace("'", "''")}'," +
                "${user.id}," +
                "'$date'," +
                "(SELECT id FROM vialist WHERE via = '${via.replace("'", "''")}')," +
                "'${medias.replace("'", "''")}'," +
                "$tweetId" +
                ")"
        for (i in 0..4) {
            try {
                stmt.execute(sql)
                break
            } catch (e: SQLException) {
                if (e.errorCode == 19) {
                    val addUser = "INSERT INTO userlist VALUES(${user.id}, '${user.screenName.replace("'", "''")}')"
                    val addVia = "INSERT INTO vialist(via) VALUES('${via.replace("'", "''")}')"
                    try {
                        stmt.execute(addUser)
                    } catch (e1: SQLException) {
                    }
                    try {
                        stmt.execute(addVia)
                    } catch (e1: SQLException) {
                    }
                } else {
                    System.err.println("ERROR")
                    System.err.println("code: ${e.errorCode}")
                    System.err.println("message: ${e.message}")
                    System.err.println()
                    break
                }
            }
        }
    }

    private fun implode(list: ArrayList<String>, glue: String): String {
        return if (list.isEmpty()) {
            ""
        } else {
            val sb = StringBuilder()
            list.map {
                sb.append(glue).append(it)
            }
            sb.substring(glue.length)
        }
    }

}