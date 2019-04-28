class gwt_hm_ReturnMessage extends gwt_hm_Message {
    isException;
    retValue;
    constructor(isException, retValue) {
        super();
        this.isException = isException;
        this.retValue = retValue;
    }
}
gwt_hm_ReturnMessage.TYPE = gwt_hm_BrowserChannel.MESSAGE_TYPE_RETURN;
gwt_hm_ReturnMessage.receive = function(channel) {
    var isException = channel.readByte();
    var retValue = channel.readValue();
    return new gwt_hm_ReturnMessage(isException != 0, retValue);
}
gwt_hm_ReturnMessage.send = function(channel, isException, retValue) {
    channel.sendByte(gwt_hm_ReturnMessage.TYPE);
    channel.sendByte(isException ? 1 : 0);
    channel.sendValue(retValue);
}