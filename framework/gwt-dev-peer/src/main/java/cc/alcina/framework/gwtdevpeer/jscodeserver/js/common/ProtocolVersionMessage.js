class gwt_hm_ProtocolVersionMessage extends gwt_hm_Message {
    version;
    constructor(version) {
        super();
        this.version = version;
    }
}
gwt_hm_ProtocolVersionMessage.TYPE = gwt_hm_BrowserChannel.MESSAGE_TYPE_PROTOCOL_VERSION;
gwt_hm_ProtocolVersionMessage.send = function(channel, version) {
    channel.sendByte(gwt_hm_ProtocolVersionMessage.TYPE);
    channel.sendInt(version);
}
gwt_hm_ProtocolVersionMessage.receive = function(channel) {
    var version = channel.readInt();
    return new gwt_hm_ProtocolVersionMessage(version);
}