class gwt_hm_LoadModuleMessage extends gwt_hm_Message {
    version;
    constructor(version) {
        super();
        this.version = version;
    }
}
gwt_hm_LoadModuleMessage.TYPE = gwt_hm_BrowserChannel.MESSAGE_TYPE_LOAD_MODULE;
gwt_hm_LoadModuleMessage.send = function(channel, url, tabKey, sessionKey, moduleName, userAgent, handler) {
    channel.sendByte(gwt_hm_LoadModuleMessage.TYPE);
    channel.sendString(url);
    channel.sendString(tabKey);
    channel.sendString(sessionKey);
    channel.sendString(moduleName);
    channel.sendString(userAgent);
    var ret = channel.reactToMessagesWhileWaitingForReturn(
        handler);
}