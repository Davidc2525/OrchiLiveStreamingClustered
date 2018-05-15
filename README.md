Inicio de proyecto para clusterisar servicio de streaming en Orchi
LiveStreaming Orchi
# OrchiLiveStreamingClustered

![modelo](https://08197413588645424907.googlegroups.com/attach/41736f6dc2131/orchi%20live%20stream%20arquitectura%20cluster.jpg?part=0.1&view=1&vt=ANaJVrHN4r7jkEHEIpa0J5BTK4qHYAqeXYcv-tiBJ1m_pNL3jGcrPssketr3QEKXG4W92VYmsxI2ZTGYxD3CetQTcJrcIiKnW_Sp10ADOTYozBMzf8wDfJw)



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
