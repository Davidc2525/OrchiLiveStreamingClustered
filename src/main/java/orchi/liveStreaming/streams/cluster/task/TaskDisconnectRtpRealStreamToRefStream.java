package orchi.liveStreaming.streams.cluster.task;

import java.io.Serializable;
import orchi.liveStreaming.streams.ManagerStream;

public class TaskDisconnectRtpRealStreamToRefStream implements Runnable, Serializable{
	
	private String idStream;

	public
	TaskDisconnectRtpRealStreamToRefStream(String idhost,String idStream){//esto en un pc
		this.idStream = idStream;		
	}
	
	@Override
	public void run() {//esto se ejecuta en otro pc
		ManagerStream.getInstance().deleteStream(idStream);	
	}

}
