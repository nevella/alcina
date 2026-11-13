class gwt_hm_FreeValueMessage extends gwt_hm_Message {
    idCount;
    ids;
    constructor(idCount, ids) {
        super();
        this.idCount = idCount;
        this.ids = ids;
    }
}
gwt_hm_FreeValueMessage.TYPE = gwt_hm_BrowserChannel.MESSAGE_TYPE_FREEVALUE;
gwt_hm_FreeValueMessage.receive = function(channel) {
    var idCount = channel.readInt();
    var ids = [];
    for (var idx = 0; idx < idCount; idx++) {
        ids.push(channel.readInt());
    }
    return new gwt_hm_FreeValueMessage(idCount, ids);
}
gwt_hm_FreeValueMessage.send = function(channel, idCount, ids) {
    channel.sendByte(gwt_hm_FreeValueMessage.TYPE);
    channel.sendInt(idCount);
    for (var idx = 0; idx < idCount; idx++) {
        channel.sendInt(ids[idx]);
    }
    channel.flush();
}