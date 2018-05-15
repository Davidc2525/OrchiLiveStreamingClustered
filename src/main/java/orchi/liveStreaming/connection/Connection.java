package orchi.liveStreaming.connection;

import java.util.concurrent.atomic.AtomicInteger;

import org.kurento.client.KurentoClient;

public class Connection {
	private String id;
	private KurentoClient connection;
	private AtomicInteger counter = new AtomicInteger();
	
	public Connection(String id,KurentoClient connection){
		this.setId(id);
		this.setConnection(connection);		
	}


	@Override
	public String toString() {
		return "Connection {id=" + id + ", connection=" + connection + ", counter=" + counter + "}";
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
	}

	/**
	 * @return the connection
	 */
	public KurentoClient getConnection() {
		return connection;
	}

	/**
	 * @param connection the connection to set
	 */
	public void setConnection(KurentoClient connection) {
		this.connection = connection;
	}
	

	public int decrementClients(){
		return counter.incrementAndGet();
	}
	
	public int incrementClients(){
		return counter.incrementAndGet();
	}
	
	
}
