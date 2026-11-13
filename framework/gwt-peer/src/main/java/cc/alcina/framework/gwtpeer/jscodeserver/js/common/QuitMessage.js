class gwt_hm_QuitMessage extends gwt_hm_Message {
    version;
    constructor(version) {
        super();
        this.version = version;
    }
}
gwt_hm_QuitMessage.TYPE = gwt_hm_BrowserChannel.MESSAGE_TYPE_QUIT;
gwt_hm_QuitMessage.send = function(channel) {
    channel.closeSocket = true;
    channel.sendByte(gwt_hm_QuitMessage.TYPE);
    channel.flush();
}