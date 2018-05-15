Inicio de proyecto para clusterisar servicio de streaming en Orchi
<<<<<<< HEAD
LiveStreaming Orchi
=======
LiveStreaming Orchi ©
>>>>>>> 7d78fb32779eeca7f35e0c29b5f56bce57eebba7
# OrchiLiveStreamingClustered



                Host A, no tiene el stream solisitado
	              Pero Host B, si
	              ____________            ____________
	              |          |            |          |
	              |       rtp| <--------- |rtp       |
	              |          |      |     |          |
	 viewer <---- |  host A  |      |     |  host B  |  <------- presenter
	          |   |          |      |     |          |      |
	         /    |          |      |     |          |      |
	      WebRtc  |__________|      |     |__________|      \
	                                |                      WebRtc
	                                |
	                               RTP
