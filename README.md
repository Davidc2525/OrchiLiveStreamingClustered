LiveStream Orchi ©
# OrchiLiveStreamingClustered

            Host A, no
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
