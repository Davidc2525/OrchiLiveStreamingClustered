package orchi.liveStreaming.streams.cluster.task;

import java.io.Serializable;
import java.util.concurrent.Callable;

import orchi.liveStreaming.streams.ManagerStream;
import orchi.liveStreaming.streams.RealStream;

public class TaskConnectRtpRefToRtpReal implements Callable<String>, Serializable {

	private String idStream;
	private String offer;
	private String idMember;

	public TaskConnectRtpRefToRtpReal(String idStream, String idMember, String offer) {
		//ambito de un pc
		ManagerStream.log.info("nueva tarea");
		this.idMember = idMember;
		this.idStream = idStream;
		this.offer = offer;
	}

	@Override
	public String call() throws Exception {
		// ambito del otro pc
		ManagerStream.log.info("realizando tarea en remoto");
		RealStream stream = (RealStream) ManagerStream.getInstance().getStream(idStream, false);
		if (stream == null) {
			// log.info("No existe stream en este nodo");
			throw new Exception("No existe stream en este nodo");
		}
		String procesAnswer = stream.getRtpByHostid(this.idMember).processOffer(offer);
		return procesAnswer;
	}

}