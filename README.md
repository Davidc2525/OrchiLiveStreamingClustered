Inicio de proyecto para clusterisar servicio de streaming en Orchi
LiveStreaming Orchi
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
