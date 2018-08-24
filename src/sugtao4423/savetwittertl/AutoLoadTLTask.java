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

	private long latestTweetId;

	public AutoLoadTLTask(Twitter twitter, long listAsTL, OnStatusListener listener){
		this.twitter = twitter;
		this.listAsTL = listAsTL;
		this.listener = listener;
		this.latestTweetId = -1L;
	}

	@Override
	public void run(){
		try{
			Paging paging = new Paging(1, 200);
			paging = latestTweetId > 0 ? paging.sinceId(latestTweetId - 1) : paging;

			ResponseList<Status> statuses;
			if(listAsTL < 0){
				statuses = twitter.getHomeTimeline(paging);
			}else{
				statuses = twitter.getUserListStatuses(listAsTL, paging);

				for(int i = 2; i <= 10; i++){
					if(statuses.size() <= 0){
						break;
					}
					if(isIncludeLatestTweetId(statuses)){
						statuses = removeLatestTweet(statuses);
						break;
					}
					paging = new Paging(i, 200);
					paging = latestTweetId > 0 ? paging.sinceId(latestTweetId - 1) : paging;
					statuses.addAll(twitter.getUserListStatuses(listAsTL, paging));
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
