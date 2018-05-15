package orchi.liveStreaming;

import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
	private static UserManager instance;
	private ConcurrentHashMap<String,UserSession> usersBySessionID  = new ConcurrentHashMap<>();
	
	
	public static UserManager getInstance(){
		if(instance == null){
			instance = new UserManager();
		}
		return instance;
	}
	
	public UserSession getUserSession(String sessionid){
		return usersBySessionID.get(sessionid);
	}
	
	public void register(String id,UserSession user){
		usersBySessionID.put(id, user);
	}
	
	
	public void remove(String id){
		usersBySessionID.remove(id);
	}
}
