package orchi.liveStreaming.connection;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.kurento.client.KurentoClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ConnectionManager {
	private static final Logger log = LoggerFactory.getLogger(ConnectionManager.class);
	private ConcurrentHashMap<String, Integer> mapCounterClientsPeerConnection = new ConcurrentHashMap<>();
	private static ConnectionManager instance;
	private Random ran = new Random();
	private AtomicInteger counter = new AtomicInteger();

	private ConcurrentHashMap<String, Connection> connections = new ConcurrentHashMap<>();

	public String addConnection(KurentoClient newConnection) {
		String id = UUID.randomUUID().toString();
		connections.put(id, new Connection(id, newConnection));
		getConnection(id);
		log.info("addConnection {}",id);
		return id;
	}

	public boolean removeConnection(String id) {
		log.info("removeConnection {}",id);
		mapCounterClientsPeerConnection.remove(id);
		return connections.remove(id) != null;
	}

	public void connectiToNKurentoClients(int n) {
		for (int x = 0; x < n; x++) {
			String id = UUID.randomUUID().toString();
			log.info("connectiToNKurentoClients {}",id);
			addConnection(KurentoClient.create());
		}
	}
	
	public Connection getNext(){
		String[] clients =  connections.keySet().toArray(new String[0]);
		String client = clients[counter.incrementAndGet()%connections.size()];
		log.info("getNext {}",client);
		return getConnection(client);
	}
	
	public Connection getRandon() {		
		String[] clients =  connections.keySet().toArray(new String[0]);
		String client = clients[ran.nextInt(clients.length)];
		log.info("getRandon {}",client);
		return getConnection(client);
	}
	
	
	public Connection getConnectionWithLeastClients() {	
		Integer minCount = 1000000000;
		Set<Entry<String, Integer>> entrySet = mapCounterClientsPeerConnection.entrySet();
		
		Entry<String, Integer> min = null;
		
		for(Entry<String, Integer> entry:entrySet){
			if(entry.getValue() < minCount){
				min = entry;
				minCount = entry.getValue();
			}
		}
				
		//Collections.min(mapCounterClientsPeerConnection);
		/* Optional<Entry<String, Integer>> min = mapCounterClientsPeerConnection.entrySet()
		.stream()
		.min(Comparator.comparingDouble(Map.Entry::getValue));
		*/
		log.info("getLeastClientConnection {} with {}",min.getKey(),min.getValue());
		return getConnection(min.getKey());
	}
	
	public Connection getConnection(String id){		
		Connection con = connections.get(id);
		int count = con.incrementClients();
		mapCounterClientsPeerConnection.put(id, count);
		return con ;
	}
	
	public static ConnectionManager getInstance(){
		if(instance == null){
			instance = new ConnectionManager();
		}
		return instance;
	}
}
