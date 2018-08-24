package sugtao4423.savetwittertl;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Timer;

import sugtao4423.savetwittertl.AutoLoadTLTask.OnStatusListener;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.User;
import twitter4j.auth.AccessToken;
import twitter4j.conf.Configuration;
import twitter4j.conf.ConfigurationBuilder;

public class SaveTwitterTL{

	private static Connection conn;
	private static Statement stmt;

	public static void main(String[] args) throws ClassNotFoundException, IOException, SQLException{
		prepareSQL();
		Twitter twitter = prepareTwitter();

		OnStatusListener listener = new OnStatusListener(){
			@Override
			public void onStatus(ResponseList<Status> statuses){
				for(Status s : statuses){
					if(!s.isRetweet()){
						onTweet(s);
					}
				}
			}
		};
		AutoLoadTLTask task = new AutoLoadTLTask(twitter, Config.LIST_AS_TL, listener);
		Timer timer = new Timer(false);
		timer.schedule(task, 0, Config.TL_LOAD_INTERVAL_SECONDS * 1000);

		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run(){
				try{
					timer.cancel();
					timer.purge();
					stmt.close();
					conn.close();
				}catch(SQLException e){
				}
			}
		});
	}

	public static void prepareSQL() throws ClassNotFoundException, IOException, SQLException {
		Class.forName("org.sqlite.JDBC");
		String location = Config.SAVE_PATH + "/TwitterTL.sqlite3";
		File db = new File(location);
		if(!db.exists()){
			db.createNewFile();
		}
		Properties prop = new Properties();
		prop.put("foreign_keys", "on");
		conn = DriverManager.getConnection("jdbc:sqlite:" + location, prop);
		stmt = conn.createStatement();
		stmt.execute("CREATE TABLE IF NOT EXISTS userlist(id INTEGER UNIQUE, screen_name TEXT)");
		stmt.execute("CREATE TABLE IF NOT EXISTS vialist(id INTEGER PRIMARY KEY AUTOINCREMENT, via TEXT UNIQUE)");
		stmt.execute("CREATE TABLE IF NOT EXISTS tweets(content TEXT, user_id INTEGER NOT NULL, date TEXT, via_id INTEGER NOT NULL, medias TEXT, tweetId INTEGER UNIQUE, FOREIGN KEY (user_id) REFERENCES userlist(id), FOREIGN KEY (via_id) REFERENCES vialist(id))");
	}

	public static Twitter prepareTwitter(){
		Configuration conf = new ConfigurationBuilder()
				.setOAuthConsumerKey(Config.CONSUMER_KEY)
				.setOAuthConsumerSecret(Config.CONSUMER_SECRET)
				.setTweetModeExtended(true).build();
		AccessToken token = new AccessToken(Config.ACCESS_TOKEN, Config.ACCESS_TOKEN_SECRET);
		return new TwitterFactory(conf).getInstance(token);
	}

	public static void onTweet(Status status){
		boolean notSave = false;
		for(String s : Config.NOT_SAVE_USER){
			if(s.equals(status.getUser().getScreenName())){
				notSave = true;
			}
		}
		if(notSave){
			return;
		}

		TwitterMediaUtil mediaUtil = new TwitterMediaUtil(status);
		String content = mediaUtil.getContent();
		ArrayList<String> mediaUrls = mediaUtil.getUrls();
		String medias = implode(mediaUrls, ",");
		String date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z").format(status.getCreatedAt());
		String via = status.getSource().replaceAll("<.+?>", "");

		insertTweet(status.getUser(), content, date, via, medias, status.getId());
	}

	public static void insertTweet(User user, String content, String date, String via, String medias, long tweetId){
		content = content.replaceAll("'", "''");
		String screenName = user.getScreenName().replaceAll("'", "''");
		long userId = user.getId();
		via = via.replaceAll("'", "''");
		medias = medias.replaceAll("'", "''");
		String sql = "INSERT INTO tweets VALUES(" +
					"'" + content + "'," +
					userId + "," +
					"'" + date + "'," +
					"(SELECT id FROM vialist WHERE via = '" + via + "')," +
					"'" + medias + "'," +
					String.valueOf(tweetId) +
					")";
		for(int i = 0; i < 5; i++){
			try{
				stmt.execute(sql);
				break;
			}catch(SQLException e){
				if(e.getErrorCode() == 19){
					String addUser = "INSERT INTO userlist VALUES(" + userId + ", '" + screenName + "')";
					String addVia = "INSERT INTO vialist(via) VALUES('" + via + "')";
					try{
						stmt.execute(addUser);
						stmt.execute(addVia);
					}catch(SQLException e1){
					}
				}else{
					System.err.println("ERROR");
					System.err.println("code: " + e.getErrorCode());
					System.err.println("message: " + e.getMessage());
					System.err.println();
					break;
				}
			}
		}
	}

	public static String implode(ArrayList<String> list, String glue){
		if(list.size() < 1){
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for(String s : list){
			sb.append(glue).append(s);
		}
		return sb.substring(glue.length());
	}

}
