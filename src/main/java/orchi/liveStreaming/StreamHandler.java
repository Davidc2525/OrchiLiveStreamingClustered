
package orchi.liveStreaming;

import java.io.IOException;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.EventListener;
import org.kurento.client.HttpPostEndpoint;
import org.kurento.client.IceCandidate;
import org.kurento.client.IceCandidateFoundEvent;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RtpEndpoint;
import org.kurento.client.VideoCaps;
import org.kurento.client.VideoCodec;
import org.kurento.client.WebRtcEndpoint;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import orchi.liveStreaming.connection.Connection;
import orchi.liveStreaming.connection.ConnectionManager;
import orchi.liveStreaming.exceptions.MaximunViewerswAllowException;
import orchi.liveStreaming.streams.ManagerStream;
import orchi.liveStreaming.streams.RealStream;
import orchi.liveStreaming.streams.Stream;

public class StreamHandler extends TextWebSocketHandler {
	
	static {
		ManagerStream.getInstance();
	}
	
	private static final Logger log = LoggerFactory.getLogger(StreamHandler.class);
	private static final Gson gson = new GsonBuilder().create();

	private final ConcurrentHashMap<String, UserSession> viewers = new ConcurrentHashMap<>();
	private static ConcurrentHashMap<String, UserSession> presenters = new ConcurrentHashMap<>();
	// private static ConcurrentHashMap<String, String> userssByIdSession = new
	// ConcurrentHashMap<>();

	// private static ConcurrentHashMap<String, MediaPipeline> pipelines = new
	// ConcurrentHashMap<>();
	//@Autowired
	//public static KurentoClient kurento = KurentoClient.create("ws://orchi:8888/kurento");
	
	static {
		ConnectionManager cn = ConnectionManager.getInstance();
		String kmsUrl = System.getProperty("kms");
		log.info("kms {}",kmsUrl);
		if(kmsUrl != null){
			cn.addConnection(KurentoClient.create(kmsUrl));
		}else{
			cn.addConnection(KurentoClient.create());
		}
		//cn.addConnection(KurentoClient.create("ws://orchi:7777/kurento"));
		//cn.connectiToNKurentoClients(10);
	}
	//public KurentoClient getKurentoClient(){return kurento;};

	// private KurentoClient kurento2 =
	// KurentoClient.create("ws://orchi2:8888/kurento");

	// private MediaPipeline pipeline;
	// private UserSession presenterUserSession;
	
	@Override
	public void afterConnectionEstablished(WebSocketSession session) throws Exception {
		
		log.info("addrees local {}:{}",session.getLocalAddress().getAddress().getHostAddress(),session.getLocalAddress().getPort());
	}
	
	
	@Override
	public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
		UserSession user = UserManager.getInstance().getUserSession(session.getId());

		JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

		if (user != null) {
		      log.info("Incoming message from user '{}': {}", user, jsonMessage);
		    } else {
		      log.info("Incoming message from new user: {}", jsonMessage);
		    }
		switch (jsonMessage.get("id").getAsString()) {
		case "presenter":
			try {
				presenter(session, jsonMessage);
			} catch (Throwable t) {
				handleErrorResponse(t, session, "presenterResponse");
			}
			break;
		case "viewer":
			try {
				viewer2(session, jsonMessage);
			} catch (Throwable t) {
				t.printStackTrace();
				handleErrorResponse(t, session, "viewerResponse");
			}
			break;
		case "onIceCandidate": {
			JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();

			if (user != null) {
				IceCandidate cand = new IceCandidate(candidate.get("candidate").getAsString(),
						candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
				user.addCandidate(cand);
			}
			break;
		}
		case "stop":
			stop(session);
			break;
		default:
			break;
		}
	}

	private void handleErrorResponse(Throwable throwable, WebSocketSession session, String responseId)
			throws IOException {
		stop(session);
		log.error(throwable.getMessage(), throwable);
		JsonObject response = new JsonObject();
		response.addProperty("id", responseId);
		response.addProperty("response", "rejected");
		response.addProperty("message", throwable.getMessage());
		session.sendMessage(new TextMessage(response.toString()));
	}

	private synchronized void presenter(final WebSocketSession session, JsonObject jsonMessage) throws IOException {
		Stream stream = ManagerStream.getInstance().getStream(jsonMessage.get("idStream").getAsString(),true);
		if (stream == null) {
			UserSession user = UserManager.getInstance().getUserSession(session.getId());
			if (user == null) {
				user = new UserSession(session);
				
				UserManager.getInstance().register(session.getId(), user);
				log.info("session {}",session.getId());
				log.info("register a user {}",UserManager.getInstance().getUserSession(session.getId()));
			}
			stream = ManagerStream.getInstance().createStream(user, jsonMessage);

		} else {
			JsonObject response = new JsonObject();
			response.addProperty("id", "presenterResponse");
			response.addProperty("response", "rejected");
			response.addProperty("message", "ya existe stream con ese id");
			session.sendMessage(new TextMessage(response.toString()));
		}
	}

	private synchronized void viewer2(final WebSocketSession session, JsonObject jsonMessage) throws IOException {
		Stream stream = ManagerStream.getInstance().getStream(jsonMessage.get("idStream").getAsString(),true);
		if (stream != null) {
			UserSession viewer = UserManager.getInstance().getUserSession(session.getId());
			if (viewer == null) {
				viewer = new UserSession(session);
				UserManager.getInstance().register(session.getId(), viewer);
			}
			try {
				stream.addView(viewer);
			} catch (MaximunViewerswAllowException e1) {
				
				JsonObject response = new JsonObject();
				response.addProperty("response", "rejected");
				response.addProperty("message",e1.getMessage());
				
				try {
					synchronized (session) {
						session.sendMessage(new TextMessage(response.toString()));
					}
				} catch (IOException e) {
					log.debug(e.getMessage());
				}
				stop(stream.getPresenterUser().getSession());
				e1.printStackTrace();
			}
			
			//creo una conexion para el viewer
			//Connection con = ConnectionManager.getInstance().getConnectionWithLeastClients();
			
			//1 create media pipeline viewer
			MediaPipeline newpipe = null;
			log.info("get Stream");
			
			if(stream instanceof RealStream){
				//local
				log.info("Stream local");
				newpipe = stream.getPipeline();
			}else{
				//remoto
				log.info("Stream remoto, connect with RTP");
				newpipe = stream.getPipeline();;//con.getConnection().createMediaPipeline();
			}

			WebRtcEndpoint viewerWebRtc = new WebRtcEndpoint.Builder(newpipe).build();
			
			/*//2 crate rpc enpoint viewer
			log.info("2 crate rpc enpoint viewer");
			RtpEndpoint rtp = new RtpEndpoint.Builder(newpipe).build();
			//rtp.setVideoFormat(new VideoCaps(VideoCodec.RAW, new org.kurento.client.Fraction(1, 30)));
			
			//3 create a rtp endpoint viewer
			log.info("3 create a rtp endpoint stream");
			RtpEndpoint rtpStream = new RtpEndpoint.Builder(viewer.getStream().getPipeline()).build();
			//rtpStream.setVideoFormat(new VideoCaps(VideoCodec.RAW, new org.kurento.client.Fraction(1, 30)));
			
			//4 viewer rtp endpint generate offer
			log.info("4 viewer rtp endpint generate offer");
			String offer = rtp.generateOffer();
			//System.err.println(offer);
			
			//5 stream rtp process offer and process answer then
			log.info("5 stream rtp process offer and process answer then");
			String rtpStreamresponse = rtpStream.processOffer(offer);
			//System.out.println(rtpViewerresponse);
			rtp.processAnswer(rtpStreamresponse);			
			
			//canalizo de rtp a webrtc viewer
			rtp.connect(viewerWebRtc);
			rtp.connect(new HttpPostEndpoint.Builder(newpipe).build());
			
			//6 stream webrtc connecto to stream rtp
			log.info("6 stream webrtc connecto to stream rtp");
			stream.getPresenterUser().getWebRtcEndpoint().connect(rtpStream);	
			log.info("stream kms {} -> viewer kms {}",stream.getConnection().getConnection().getSessionId(),con.getConnection().getSessionId());
			
			
			*/
			viewerWebRtc.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

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
						log.debug(e.getMessage());
					}
				}
			});

			viewer.setWebRtcEndpoint(viewerWebRtc);
			//viewer.setRtpEndPoint(rtp);
			stream.connectMediaElement(viewerWebRtc);
			String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();
			String sdpAnswer = viewerWebRtc.processOffer(sdpOffer);

			JsonObject response = new JsonObject();
			response.addProperty("id", "viewerResponse");
			response.addProperty("response", "accepted");
			response.addProperty("sdpAnswer", sdpAnswer);

			synchronized (session) {
				viewer.sendMessage(response);
			}
			viewerWebRtc.gatherCandidates();
		} else {
			JsonObject response = new JsonObject();
			response.addProperty("id", "presenterResponse");
			response.addProperty("response", "rejected");
			response.addProperty("message", "no existe stream");
			session.sendMessage(new TextMessage(response.toString()));
		}
	}
	
	private synchronized void stop(WebSocketSession session) throws IOException {
		log.info("Stoping session {}",session.getId());
		String sessionId = session.getId();
		UserSession user = UserManager.getInstance().getUserSession(sessionId);
		
		if(user == null){ 
			return;
		}
		//log.info("user {}",user.getStream().getPresenterUser().getSession().getId());
		if(user.getIs()==UserType.presenter){
			if(user.getStream().getPresenterUser() != null 
				&& user.getStream().getPresenterUser().getSession().getId().equals(session.getId())){
				//presentador
				
				log.info("ShowtDown Stream {}",user.getStream().getId());			
				ManagerStream.getInstance().deleteStream(user.getStream().getId());
				//user.getStream().showtDown();
				//ManagerStream.getInstance().deleteStream(user.getStream().getId());;
				//user.getStream().getPipeline().release();
				//UserManager.getInstance().remove(sessionId);
			}
		}else{
			//observador
			UserManager.getInstance().remove(sessionId);
			user.getStream().removeView(user);
			user.getWebRtcEndpoint().release();
		}
		
		/*if(user.getStream().getPresenterUser() != null 
				&& user.getStream().getPresenterUser().getSession().getId().equals(session.getId())){
			//presentador
			
			log.info("ShowtDown Stream {}",user.getStream().getId());			
			user.getStream().showtDown();
			//ManagerStream.getInstance().deleteStream(user.getStream().getId());;
			//user.getStream().getPipeline().release();
			//UserManager.getInstance().remove(sessionId);
		}else{
			//observador
			UserManager.getInstance().remove(sessionId);
			user.getStream().removeView(user);
			user.getWebRtcEndpoint().release();
			
		}*/
		
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
		stop(session);
	}

}
