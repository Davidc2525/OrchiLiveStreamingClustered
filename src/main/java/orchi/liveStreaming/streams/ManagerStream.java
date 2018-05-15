package orchi.liveStreaming.streams;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.kurento.client.RtpEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberSelector;

import orchi.liveStreaming.UserSession;
import orchi.liveStreaming.streams.cluster.MemberCluster;
import orchi.liveStreaming.streams.cluster.task.TaskConnectRtpRefToRtpReal;

public class ManagerStream {
	


	public static final Logger log = LoggerFactory.getLogger(ManagerStream.class);
	//private Random ran = new Random();
	private static ManagerStream instance;
	/**Streams locales*/
	private ConcurrentHashMap<String, Stream> streams = new ConcurrentHashMap<String, Stream>();
	
	/**streams en cluster, idStream->idMember*/
	private IMap<String, String> streamsClusterMap;
	
	
	public ManagerStream(){
		MemberCluster.getInstance();
		
		streamsClusterMap = MemberCluster.getInstance().getHazel().getMap("streamsClusterMap");
		
	}
	
	
	public static ManagerStream getInstance(){
		if(instance == null){
			instance = new ManagerStream();
		}
		return instance;
	}
	
	
	
	public void deleteStream(String id){
		log.info("eliminando stream para session {}",id);
		streams.get(id).showtDown();;
		streams.remove(id);
		streamsClusterMap.remove(id);
	}
	   
	public RealStream createStream(UserSession user,JsonObject jsonMessage){
		log.info("creando nuevo stream para session {}",user.getSession().getId());
		String id = UUID.randomUUID().toString();
		RealStream stream = new RealStream(id,user);
		
		String sdpOffer = jsonMessage.getAsJsonPrimitive("sdpOffer").getAsString();
		
		WebRtcEndpoint presenterWebRtc = user.getWebRtcEndpoint();
		String sdpAnswer = presenterWebRtc.processOffer(sdpOffer);

		JsonObject response = new JsonObject();
		response.addProperty("id", "presenterResponse");
		response.addProperty("response", "accepted");
		response.addProperty("idStream", id);
		response.addProperty("locationStream", stream.address);
		response.addProperty("idConnection", stream.getConnection().getId());
		response.addProperty("sdpAnswer", sdpAnswer);

		synchronized (user.getSession()) {
			try {
				user.sendMessage(response);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		presenterWebRtc.gatherCandidates();
		streams.put(id, stream);
		streamsClusterMap.put(id,MemberCluster.getInstance().getIdMember());
		return stream;
	}
	
	public Stream getStream(String id,Boolean searchincluster){
		Stream stream = null;
		stream = streams.get(id);
		if(searchincluster){
			if(stream == null){
				if(streamsClusterMap.containsKey(id)){
					stream = connectRefStreamToRealStream(id);
				}else{
					return null;
				}
			}
		}
		
		return stream ;
	}
	
	/**
	 * 
	 * para cuando a un host llega un espectador (viewer)
	 * y existe el stream pero ese stream no se esta emitiendo desde este host
	 * se conecta al host q tiene ese stream por medio de RTP 
	 * para poder emitir asia ese viewer desde el host remoto q tiene el stream asia el viewer
	 * por medio de este host, con un stream de referencia
	 * <pre>
	 *             Host A, no
	 *             tiene el stream
	 *             solisitado
	 *             Pero Host B, si
	 *             Host A: {@link RefStream}
	 *             Host A: {@link RealStream}
	 *             ____________            ____________
	 *             |          |            |          |
	 *             |       rtp| <--------- |rtp       |
	 *             |          |      |     |          |
	 *viewer <---- |  host A  |      |     |  host B  |  <------- presenter
	 *         |   |          |      |     |          |      |
	 *        /    |          |      |     |          |      |
	 *     WebRtc  |__________|      |     |__________|      \
	 *                               |                      WebRtc
	 *                               |
	 *                              RTP
	 * </pre>
	 * @author david
	 * @param idStream id del stream al cual se quiere conectar
	 * @return {@link RefStream} conectado al host remoto
	 * */
	private RefStream connectRefStreamToRealStream(String idStream){
		log.warn("connectRefStreamToRealStream: {}",idStream);
		
		RefStream refStream = new RefStream(idStream, null);
		
		RtpEndpoint rtpRef = refStream.createAndGetRtpEndPoint();
		
		String offerGenerateRefStream = rtpRef.generateOffer();
		log.info("rtpRef {}",rtpRef);
		
		final String idHostContentStream = streamsClusterMap.get(idStream);
		
		log.info("enviando tarea a {} nodo con stream {}, offertGenerate \n{}",idHostContentStream,idStream,offerGenerateRefStream);
		
		@SuppressWarnings("unchecked")
		Future<String> processAnswer = (Future<String>) MemberCluster
		.getInstance()
		.submitTaskToMembers(
				new TaskConnectRtpRefToRtpReal(idStream, MemberCluster.getInstance().getIdMember(), offerGenerateRefStream),
				new MemberSelector() {
					@Override
					public boolean select(Member member) {
						return member.getUuid().equals(idHostContentStream);
					}
				});
		
		
		try {		
			log.info("esperando respuesta de rtp remoto");
			String answer = processAnswer.get();
			log.info("answer {}",answer);
			rtpRef.processAnswer(answer);
			
		} catch (InterruptedException | ExecutionException e) {
			
			e.printStackTrace();
		}
		streams.put(refStream.getId(), refStream);
		return refStream;
	}
	
	

	
}
