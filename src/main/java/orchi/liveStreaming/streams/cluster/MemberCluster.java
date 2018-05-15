package orchi.liveStreaming.streams.cluster;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberSelector;

import orchi.liveStreaming.streams.ManagerStream;

public class MemberCluster{
	private static MemberCluster instance;
	private  HazelcastInstance hazel;
	private String idMember;
	private IExecutorService executorService;
	
	public MemberCluster(){
		ClientConfig ClientConfig;
		try {
			Config confg = new XmlConfigBuilder(ManagerStream.class.getResource("/hazelcastClient.xml").openStream())
					.build();
			setHazel(Hazelcast.newHazelcastInstance(confg));

			setIdMember(getHazel().getCluster().getLocalMember().getUuid());
			
			
			executorService = getHazel().getExecutorService("streamsConnector");

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public Future<?> submitTask(Callable<?> task){
		return executorService.submit(task);
	}
	
	public Future<?> submitTaskToMembers(Callable<?> task,MemberSelector to){
		return executorService.submit(task,to);
	}
	
	public void ececuteTask(Runnable task){
		 executorService.submit(task);
	}
	
	public void executeToMembers(Runnable task,MemberSelector to){
		executorService.executeOnMembers(task,to);
	}
	
	
	public void executeToMember(Runnable task,String to){
		Iterator<Member> iter = getHazel().getCluster().getMembers().iterator();
		while(iter.hasNext()){
			Member member = iter.next();
			if(member.getUuid().equals(to)){
				executorService.executeOnMember(task,member);
			}
		}
		
	}
	
	
	
	/**
	 * @return the hazel
	 */
	public HazelcastInstance getHazel() {
		return hazel;
	}

	/**
	 * @param hazel the hazel to set
	 */
	public void setHazel(HazelcastInstance hazel) {
		this.hazel = hazel;
	}
	
	
	public static MemberCluster getInstance(){
		if(instance == null){
			instance = new MemberCluster();
		}
		
		return instance;
		
	}


	/**
	 * @return the idMember
	 */
	public String getIdMember() {
		return idMember;
	}


	/**
	 * @param idMember the idMember to set
	 */
	private void setIdMember(String idMember) {
		this.idMember = idMember;
	}
}
