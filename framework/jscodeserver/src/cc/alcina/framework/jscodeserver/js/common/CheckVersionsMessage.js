class gwt_hm_CheckVersionsMessage extends gwt_hm_Message {
    minVersion;
    maxVersion;
    hostedHtmlVersion;
    constructor(minVersion, maxVersion, hostedHtmlVersion) {
        super();
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.hostedHtmlVersion = hostedHtmlVersion;
    }
    getType() {
        return gwt_hm_CheckVersionsMessage.TYPE;
    }
    getHostedHtmlVersion() {
        return hostedHtmlVersion;
    }
    receive(channel) {
        var minVersion = channel.readInt();
        var maxVersion = channel.readInt();
        var hostedHtmlVersion = channel.readString();
        return new gwt_hm_CheckVersionsMessage(minVersion, maxVersion, hostedHtmlVersion);
    }
    
}
gwt_hm_CheckVersionsMessage.TYPE = gwt_hm_BrowserChannel.MESSAGE_TYPE_CHECK_VERSIONS;
gwt_hm_CheckVersionsMessage.send=function(channel, minVersion, maxVersion,
    hostedHtmlVersion) {
    channel.sendByte(gwt_hm_CheckVersionsMessage.TYPE);
    channel.sendInt(minVersion);
    channel.sendInt(maxVersion);
    channel.sendString(hostedHtmlVersion);
    channel.flush();
}