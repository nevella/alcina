class gwt_hm_InvokeSpecialMessage extends gwt_hm_Message {
    dispatchId;
    numArgs;
    args;
    constructor(dispatchId, numArgs, args) {
        super();
        this.dispatchId = dispatchId;
        this.numArgs = numArgs;
        this.args = args;
    }
}
gwt_hm_InvokeSpecialMessage.TYPE = gwt_hm_BrowserChannel.MESSAGE_TYPE_INVOKESPECIAL;
gwt_hm_InvokeSpecialMessage.receive = function(channel) {
    var dispatchId = channel.readByte();
    var numArgs = channel.readInt();
    var args = [];
    for (var idx = 0; idx < numArgs; idx++) {
        var val = channel.readValue();
        args.push(val);
    }
    return new gwt_hm_InvokeSpecialMessage(dispatchId, numArgs, args);
}
gwt_hm_InvokeSpecialMessage.send = function(channel, dispatchId, numArgs, args) {
    channel.ensureClear();
    channel.sendByte(gwt_hm_InvokeSpecialMessage.TYPE);
    channel.sendByte(dispatchId)
    channel.sendInt(numArgs)
    for (var i = 0; i < numArgs; ++i) {
        channel.sendValue(args[i])
    }
}