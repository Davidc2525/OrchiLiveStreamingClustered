package orchi.liveStreaming.streams;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.client.ConnectionStateChangedEvent;
import org.kurento.client.ErrorEvent;
import org.kurento.client.EventListener;
import org.kurento.client.KurentoObject;
import org.kurento.client.MediaElement;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RtpEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import orchi.liveStreaming.UserSession;
import orchi.liveStreaming.connection.Connection;
import orchi.liveStreaming.connection.ConnectionManager;
import orchi.liveStreaming.exceptions.MaximunViewerswAllowException;


/**
 * Stream de referencia, es usado para conectar un stream real a otra instancia 
 * q necesita flujo de ese stream real
 * */
public class RefStream implements Stream{
	private static final Logger log = LoggerFactory.getLogger(RefStream.class);
	
	private String id;
	private ConcurrentHashMap<String, UserSession> viewers = new ConcurrentHashMap<String, UserSession>();

	private Connection connection;
	private MediaPipeline pipeline;
	private RtpEndpoint rtp;

	public RefStream(String id, UserSession presenterUser){
		this.id = id;
		//this.setPresenterUser(presenterUser);

		setConnection(ConnectionManager.getInstance().getConnectionWithLeastClients());
		setPipeline(getConnection().getConnection().createMediaPipeline());
		
	}
	
	private void setPipeline(MediaPipeline pipeline) {
		this.pipeline = pipeline;			
	}

	private Connection getConnection() {
		return connection;
	}

	private void setConnection(Connection connection) {
		this.connection = connection;
		
	}

	@Override
	public void connectMediaElement(MediaElement sink) {
		getRtp().connect(sink);	
	}

	@Override
	public RtpEndpoint createAndGetRtpEndPoint() {
		setRtp(new RtpEndpoint.Builder(pipeline).build());
		getRtp().addErrorListener(new EventListener<ErrorEvent>() {
			
			@Override
			public void onEvent(ErrorEvent event) {
				log.warn("error en RTP {}",event.getDescription());
				
			}
		});
		getRtp().addConnectionStateChangedListener(new EventListener<ConnectionStateChangedEvent>() {
			
			@Override
			public void onEvent(ConnectionStateChangedEvent event) {
				log.warn("nuevo estado del RTP {}",event.getNewState());
			}
		});
		return getRtp();
	}

	/**
	 * @return the rtp
	 */
	public RtpEndpoint getRtp() {
		return rtp;
	}

	/**
	 * @param rtp the rtp to set
	 */
	public void setRtp(RtpEndpoint rtp) {
		this.rtp = rtp;
	}

	@Override
	public void addView(UserSession view) throws MaximunViewerswAllowException {
		if(viewers.size()>=10){
			throw new MaximunViewerswAllowException("Este stream supero la capasidad permitida de espectadores: "+viewers.size());
		}
		log.info("Nuevo observador {} en el stream {} ", view.getSession().getId(), id);
		view.setStream(this);
		viewers.put(view.getSession().getId(), view);		
	}

	@Override
	public UserSession getPresenterUser() {
		return null;
	}

	@Override
	public MediaPipeline getPipeline() {
		
		return pipeline;
	}

	@Override
	public void removeView(UserSession view) {
		log.info("Observador {} dejo el stream {} ", view.getSession().getId(), id);
		viewers.remove(view.getSession().getId(), view);		
	}

	@Override
	public ConcurrentHashMap<String, UserSession> getViewers() {
		// TODO Auto-generated method stub
		return viewers;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return id;
	}

	@Override
	public void showtDown() {
		
		log.info("RefStream shutdown {}",getId());
		Iterator<Entry<String, UserSession>> iter = viewers.entrySet().iterator();
		while(iter.hasNext()){
			Entry<String, UserSession> entry = iter.next();
			
			log.info("Enviado señal a observador de q se termino el stream");
			JsonObject response = new JsonObject();
			response.addProperty("id", "stopCommunication");
			response.addProperty("stopType", "normal");
			try {
				entry.getValue().sendMessage(response);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
		viewers.clear();
		pipeline.release();
		getRtp().release();
		
	}

}
