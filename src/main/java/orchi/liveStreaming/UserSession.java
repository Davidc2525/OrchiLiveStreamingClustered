/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package orchi.liveStreaming;

import java.io.IOException;

import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.RtpEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.google.gson.JsonObject;

import orchi.liveStreaming.streams.RealStream;
import orchi.liveStreaming.streams.Stream;

public class UserSession {

	private static final Logger log = LoggerFactory.getLogger(UserSession.class);

	private UserType is = UserType.viewer;
	
	public String idUser;

	private final WebSocketSession session;
	
	/**borrar*/
	private WebRtcEndpoint webRtcEndpoint;

	private Stream stream;

	private RtpEndpoint rtp;

	public UserSession(WebSocketSession session) {
		this.session = session;
	}

	public void setStream(Stream stream) {
		this.stream = stream;
	}

	public Stream getStream() {
		return stream;
	}

	public WebSocketSession getSession() {
		return session;
	}

	public void sendMessage(JsonObject message) throws IOException {
		// log.info("Sending message from user with session Id '{}': {}",
		// session.getId(), message);
		session.sendMessage(new TextMessage(message.toString()));
	}

	public WebRtcEndpoint getWebRtcEndpoint() {
		return webRtcEndpoint;
	}

	public void setWebRtcEndpoint(WebRtcEndpoint webRtcEndpoint) {
		this.webRtcEndpoint = webRtcEndpoint;
	}

	public void addCandidate(IceCandidate candidate) {
		webRtcEndpoint.addIceCandidate(candidate);
	}

	public void setRtpEndPoint(RtpEndpoint rtp) {
		this.setRtp(rtp);
	}

	public RtpEndpoint getRtpEndPoint(RtpEndpoint rtp) {
		return this.getRtp();
	}

	/**
	 * @return the rtp
	 */
	private RtpEndpoint getRtp() {
		return rtp;
	}

	/**
	 * @param rtp
	 *            the rtp to set
	 */
	private void setRtp(RtpEndpoint rtp) {
		this.rtp = rtp;
	}

	/**
	 * @return the is
	 */
	public UserType getIs() {
		return is;
	}

	/**
	 * @param is the is to set
	 */
	public void setIs(UserType is) {
		this.is = is;
	}
}
