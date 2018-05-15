package orchi.liveStreaming.streams;

import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.KurentoObject;
import org.kurento.client.MediaElement;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RtpEndpoint;

import com.google.gson.JsonObject;

import orchi.liveStreaming.UserSession;
import orchi.liveStreaming.exceptions.MaximunViewerswAllowException;


public interface Stream {
	
	
	public void connectMediaElement(MediaElement sink);
	
	public void showtDown();
	
	/**Crear un rtp para conectar con el webRtc del host remoto, por protocolo RTP
	 * se crea un RTP por cada host q necesite ese streaming
	 * 
	 * SOLO PARA RefSrteams*/
	public RtpEndpoint createAndGetRtpEndPoint();

	public void addView(UserSession viewer)throws MaximunViewerswAllowException ;

	public UserSession getPresenterUser();

	public MediaPipeline getPipeline();

	public void removeView(UserSession user);

	public ConcurrentHashMap<String, UserSession>  getViewers();

	public String getId();
}
