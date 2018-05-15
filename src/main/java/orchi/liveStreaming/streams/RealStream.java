package orchi.liveStreaming.streams;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.EventListener;
import org.kurento.client.Fraction;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.MediaElement;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RecorderEndpoint;
import org.kurento.client.RtpEndpoint;
import org.kurento.client.VideoCaps;
import org.kurento.client.VideoCodec;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonObject;
import orchi.liveStreaming.UserManager;
import orchi.liveStreaming.UserSession;
import orchi.liveStreaming.UserType;
import orchi.liveStreaming.connection.Connection;
import orchi.liveStreaming.connection.ConnectionManager;
import orchi.liveStreaming.exceptions.MaximunViewerswAllowException;
import orchi.liveStreaming.streams.cluster.MemberCluster;
import orchi.liveStreaming.streams.cluster.task.TaskDisconnectRtpRealStreamToRefStream;

public class RealStream implements Stream{
	private static final Logger log = LoggerFactory.getLogger(RealStream.class);
	
	private Connection connection;
	
	private MediaPipeline pipeline;

	private ConcurrentHashMap<String, UserSession> viewers = new ConcurrentHashMap<String, UserSession>();
	
	/**
	 * RTP por host (KMS) remoto
	 * idhost -> rtp
	 * */
	private ConcurrentHashMap<String, RtpEndpoint> rtpByHostId = new ConcurrentHashMap<String, RtpEndpoint>();

	
	private UserSession presenterUser;

	private String id;

	public String address;

	
	public RealStream(String id, UserSession presenterUser) {

		this.setId(id);
		this.setPresenterUser(presenterUser);

		setConnection(ConnectionManager.getInstance().getConnectionWithLeastClients());
		setPipeline(getConnection().getConnection().createMediaPipeline());
		
		
		
		presenterUser.setWebRtcEndpoint(new WebRtcEndpoint.Builder(pipeline).build());
		

		
		WebRtcEndpoint presenterWebRtc = presenterUser.getWebRtcEndpoint();
		presenterWebRtc.setVideoFormat(new VideoCaps(VideoCodec.H264, new Fraction(1, 100)));
		
		presenterWebRtc.setMaxVideoSendBandwidth(0);
		presenterWebRtc.setMaxVideoRecvBandwidth(0);
		presenterWebRtc.setMaxOutputBitrate(0);
		presenterWebRtc.setMinOutputBitrate(15000);;
	
		
		RecorderEndpoint recording = new RecorderEndpoint.Builder(pipeline,
				"file:///home/david/kurento-tutorial-java/OrchiStreams/recording/" + id + ".webm")
				.build();
		//recording.setVideoFormat(new VideoCaps(VideoCodec.RAW, new org.kurento.client.Fraction(30, 30)));
		recording.setMaxOutputBitrate(0); 
		recording.setMinOutputBitrate(15000);;
		recording.setVideoFormat(new VideoCaps(VideoCodec.H264, new Fraction(1, 100)));
		
		//new HttpGetEndpoint.Builder(pipeline).build();
		presenterWebRtc.connect(recording);
		
		log.info("Grabando strean {}", id);
		recording.record();

		final WebSocketSession session = presenterUser.getSession();
		presenterWebRtc.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

			@Override
			public void onEvent(IceCandidateFoundEvent event) {
				JsonObject response = new JsonObject();
				response.addProperty("id", "iceCandidate");
				response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
				try {
					synchronized (session) {
						session.sendMessage(new TextMessage(response.toString()));
					}
				} catch (IOException e) {
					// log.debug(e.getMessage());
				}
			}
		});

	}

	public void showtDown(){
		
		for(Entry<String, UserSession> viewer:getViewers().entrySet()){
			log.info("Enviado señal a observador de q se termino el stream");
			JsonObject response = new JsonObject();
			response.addProperty("id", "stopCommunication");
			try {
				viewer.getValue().sendMessage(response);
			} catch (IOException e) {
				e.printStackTrace();
			}	
		}
		getViewers().clear();
		Iterator<Entry<String, RtpEndpoint>> iter = rtpByHostId.entrySet().iterator();
		while(iter.hasNext()){
			
			Entry<String, RtpEndpoint> entry = iter.next();
			log.info("Enviado tarea de desconexion a {}",entry.getKey());
			
			MemberCluster.getInstance().executeToMember(
					new TaskDisconnectRtpRealStreamToRefStream(entry.getKey(),getId()),
					entry.getKey()
			);
			
			
			log.info("Desconectando RTP de WebRtc");
			if(getPresenterUser()!=null)
				getPresenterUser().getWebRtcEndpoint().disconnect(entry.getValue());
			
			log.info("liberando RTP de host {}",entry.getKey());
			entry.getValue().release();
			
		}
		rtpByHostId.clear();
		if(getPresenterUser()!=null)
			getPresenterUser().getWebRtcEndpoint().release();
		
		//ManagerStream.getInstance().deleteStream(getId());;
		UserManager.getInstance().remove(getPresenterUser().getSession().getId());
		pipeline.release();		
	}	
	
	public void addView(UserSession view) throws MaximunViewerswAllowException {		
		if(viewers.size()>=10){
			throw new MaximunViewerswAllowException("Este stream supero la capasidad permitida de espectadores: "+viewers.size());
		}
		log.info("Nuevo observador {} en el stream {} ", view.getSession().getId(), getId());
		view.setStream(this);
		viewers.put(view.getSession().getId(), view);
	}

	public void removeView(UserSession view) {
		log.info("Observador {} dejo el stream {} ", view.getSession().getId(), getId());
		viewers.remove(view.getSession().getId(), view);
	}

	public ConcurrentHashMap<String, UserSession> getViewers() {
		return viewers;
	}

	/**
	 * @return the presenterUser
	 */
	public UserSession getPresenterUser() {
		return presenterUser;
	}

	/**
	 * @param presenterUser
	 *            the presenterUser to set
	 */
	public void setPresenterUser(UserSession presenterUser) {

		this.presenterUser = presenterUser;
		address = String.format("wss://%s:%s/streaming",
				presenterUser.getSession().getLocalAddress().getAddress().getHostAddress(),
				presenterUser.getSession().getLocalAddress().getPort());
		this.presenterUser.setStream(this);
		this.presenterUser.setIs(UserType.presenter);
	}

	/**
	 * @return the pipeline
	 */
	public MediaPipeline getPipeline() {
		return pipeline;
	}

	/**
	 * @param pipeline
	 *            the pipeline to set
	 */
	public void setPipeline(MediaPipeline pipeline) {
		this.pipeline = pipeline;		
	}

	/**
	 * @return the connection
	 */
	public Connection getConnection() {
		return connection;
	}

	/**
	 * @param connection the connection to set
	 */
	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	@Override
	public void connectMediaElement(MediaElement sink) {
		/*((WebRtcEndpoint) sink).setMaxVideoSendBandwidth(0);
		((WebRtcEndpoint) sink).setMaxVideoRecvBandwidth(0);
		((WebRtcEndpoint) sink).setMaxOutputBitrate(0);
		((WebRtcEndpoint) sink).setMinOutputBitrate(15000);;
		((WebRtcEndpoint) sink).setVideoFormat(new VideoCaps(VideoCodec.H264, new Fraction(1, 100)));*/
		getPresenterUser().getWebRtcEndpoint().connect(sink);		
	}
	
	public void disconnectRtpByHostId(String hostId){
		RtpEndpoint rtp = rtpByHostId.remove(hostId);
		rtp.release();
	}
	
	/**
	 * obtiene el rtp para ese host remoto, si no lo hay, lo crea, lo guarda y lo envia
	 * */
	public RtpEndpoint getRtpByHostid(String hostId){
		log.info("getRtpByHostid: stream {}, host {}",getId(),hostId);
		RtpEndpoint rtp = null;
		rtp = rtpByHostId.get(hostId);
		if(rtp == null){
			rtp = this.createRtpEndPintByHostId(hostId);
		}
		return rtp ;
	}
	
	
	public RtpEndpoint createRtpEndPintByHostId(String hostId) {
		log.info("createRtpEndPintByHostId: {}",hostId);
		RtpEndpoint rtp = new RtpEndpoint.Builder(pipeline).build();
		rtpByHostId.put(hostId, rtp);
		connectMediaElement(rtp);
		return rtp ;
	}

	@Override
	public RtpEndpoint createAndGetRtpEndPoint() {
		
		/*RtpEndpoint rtp = new RtpEndpoint.Builder(pipeline).build();
		connectMediaElement(rtp);
		return rtp ;*/
		return null;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(String id) {
		this.id = id;
	};
}
