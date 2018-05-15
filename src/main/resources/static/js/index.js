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
var mediaConstraints = {
    audio: true,
    video: {

        mandatory : {

            maxWidth: 1280,
            maxHeight: 720,
            minAspectRatio: 1.77,
            minFrameRate: 3,
            maxFrameRate: 64
        }
    }

}
var elementIdStream = null;
window.idUser="david"
window.idStream=""
var ws = new WebSocket('wss://' + location.host + '/streaming');
var video;
var webRtcPeer;

window.onload = function() {
    elementIdStream = document.getElementById("idStream");
    elementIdStream.onkeyup=(e)=>{window.idStream = e.target.value}
    window.idUser="david"
    window.idStream=elementIdStream.value;
	console = new Console();
	video = document.getElementById('video');
	disableStopButton();
}

window.onbeforeunload = function() {
	ws.close();
}

ws.onmessage = function(message) {
	var parsedMessage = JSON.parse(message.data);
	console.info('Received message: ' + message.data);

	switch (parsedMessage.id) {
	case 'presenterResponse':
		presenterResponse(parsedMessage);
		break;
	case 'viewerResponse':
		viewerResponse(parsedMessage);
		break;
	case 'iceCandidate':
		webRtcPeer.addIceCandidate(parsedMessage.candidate, function(error) {
			if (error)
				return console.error('Error adding candidate: ' + error);
		});
		break;
	case 'stopCommunication':
		dispose();
		break;
	default:
		console.error('Unrecognized message', parsedMessage);
	}
}

function presenterResponse(message) {
	if (message.response != 'accepted') {
		var errorMsg = message.message ? message.message : 'Unknow error';
		console.info('Call not accepted for the following reason: ' + errorMsg);
		dispose();
	} else {
	    elementIdStream.value=message.idStream
		webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
			if (error)
				return console.error(error);
		});
	}
}

function viewerResponse(message) {
	if (message.response != 'accepted') {
		var errorMsg = message.message ? message.message : 'Unknow error';
		console.info('Call not accepted for the following reason: ' + errorMsg);
		dispose();
	} else {
		webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
			if (error)
				return console.error(error);


		});
	}
}

function presenter() {
    if(ws.readyState != WebSocket.OPEN){
        alert("no esta conectado")
        return;
    }
	if (!webRtcPeer) {
		showSpinner(video);

		var options = {
			localVideo : video,
			onicecandidate : onIceCandidate,
			mediaConstraints: mediaConstraints
		}
		webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options,
				function(error) {
					if (error) {
						return console.error(error);
					}
					webRtcPeer.generateOffer(onOfferPresenter);
				});

		enableStopButton();
	}
}

function onOfferPresenter(error, offerSdp) {
	if (error)
		return console.error('Error generating the offer');
	console.info('Invoking SDP offer callback function ' + location.host);
	var message = {
	    idUser:window.idUser,
	    idStream:window.idStream,
		id : 'presenter',
		sdpOffer : offerSdp
	}
	sendMessage(message);
}

function viewer() {
	if (!webRtcPeer) {
		showSpinner(video);

		var options = {
			remoteVideo : video,
			onicecandidate : onIceCandidate,
			mediaConstraints: mediaConstraints
		}
		webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
				function(error) {
					if (error) {
						return console.error(error);
					}
					this.generateOffer(onOfferViewer);
				});

		enableStopButton();
	}
}

function onOfferViewer(error, offerSdp) {
	if (error)
		return console.error('Error generating the offer');
	console.info('Invoking SDP offer callback function ' + location.host);
	var message = {
	    idUserToView:window.idUser,
	    idStream:window.idStream,
		id : 'viewer',
		sdpOffer : offerSdp
	}
	sendMessage(message);
}

function onIceCandidate(candidate) {
	console.log("Local candidate" + JSON.stringify(candidate));

	var message = {
		id : 'onIceCandidate',
		idUser:window.idUser,
		candidate : candidate
	};
	sendMessage(message);
}

function stop() {
	var message = {
		id : 'stop',
		idUserToView:window.idUser
	}
	sendMessage(message);
	dispose();
}

function dispose() {
	if (webRtcPeer) {
		webRtcPeer.dispose();
		webRtcPeer = null;
	}
	hideSpinner(video);

	disableStopButton();
}

function disableStopButton() {
	enableButton('#presenter', 'presenter()');
	enableButton('#viewer', 'viewer()');
	disableButton('#stop');
}

function enableStopButton() {
	disableButton('#presenter');
	disableButton('#viewer');
	enableButton('#stop', 'stop()');
}

function disableButton(id) {
	$(id).attr('disabled', true);
	$(id).removeAttr('onclick');
}

function enableButton(id, functionName) {
	$(id).attr('disabled', false);
	$(id).attr('onclick', functionName);
}

function sendMessage(message) {
	var jsonMessage = JSON.stringify(message);
	console.log('Senging message: ' + jsonMessage);
	ws.send(jsonMessage);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent-1px.png';
		arguments[i].style.background = 'center transparent url("./img/spinner.gif") no-repeat';
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].src = '';
		arguments[i].poster = './img/webrtc.png';
		arguments[i].style.background = '';
	}
}

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});
