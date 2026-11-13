class gwt_hm_LoadJsniMessage extends gwt_hm_Message {
    js;
    constructor(js) {
        super();
        this.js = js;
    }
}
gwt_hm_LoadJsniMessage.TYPE = gwt_hm_BrowserChannel.MESSAGE_TYPE_LOADJSNI;
gwt_hm_LoadJsniMessage.receive = function(channel) {
    var js = channel.readString();
    return new gwt_hm_LoadJsniMessage(js);
}