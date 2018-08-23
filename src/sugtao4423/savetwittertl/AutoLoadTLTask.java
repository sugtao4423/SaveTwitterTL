package sugtao4423.savetwittertl;

import java.util.Collections;
import java.util.TimerTask;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;

public class AutoLoadTLTask extends TimerTask{

	public interface OnStatusListener{
		void onStatus(ResponseList<Status> statuses);
	}

	private Twitter twitter;
	private long listAsTL;
	private OnStatusListener listener;

	private long latestTweetId = -1L;

	public AutoLoadTLTask(Twitter twitter, long listAsTL, OnStatusListener listener){
		this.twitter = twitter;
		this.listAsTL = listAsTL;
		this.listener = listener;
	}

	@Override
	public void run(){
		try{
			Paging paging = latestTweetId < 0 ? new Paging(1, 200) : new Paging(1, 200).sinceId(latestTweetId - 1);

			ResponseList<Status> statuses;
			if(listAsTL < 0){
				statuses = twitter.getHomeTimeline(paging);
			}else{
				statuses = twitter.getUserListStatuses(listAsTL, paging);

				if(latestTweetId > 0){
					for(int i = 2; i < 10; i++){
						if(statuses.size() <= 0){
							break;
						}
						if(isIncludeLatestTweetId(statuses)){
							statuses = removeLatestTweet(statuses);
							break;
						}
						paging = new Paging(i, 200).sinceId(latestTweetId - 1);
						statuses.addAll(twitter.getUserListStatuses(listAsTL, paging));
					}
				}
			}

			if(statuses.size() > 0){
				Collections.sort(statuses);
				latestTweetId = statuses.get(statuses.size() - 1).getId();
				listener.onStatus(statuses);
			}
		}catch(TwitterException e){
		}
	}

	public boolean isIncludeLatestTweetId(ResponseList<Status> statuses){
		for(Status s : statuses){
			if(s.getId() == latestTweetId){
				return true;
			}
		}
		return false;
	}

	public ResponseList<Status> removeLatestTweet(ResponseList<Status> statuses){
		Collections.sort(statuses);
		Collections.reverse(statuses);
		for(int i = (statuses.size() - 1); i >= 0; i--){
			if(statuses.get(i).getId() <= latestTweetId){
				statuses.remove(i);
			}
		}
		return statuses;
	}

}
