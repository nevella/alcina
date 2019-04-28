class gwt_hm_InvokeMessage extends gwt_hm_Message {
    thisRef;
    methodName;
    numArgs;
    args;
    constructor(thisRef, methodName,
        numArgs, args) {
        super();
        this.thisRef = thisRef;
        this.methodName = methodName;
        this.numArgs = numArgs;
        this.args = args;
    }
}
gwt_hm_InvokeMessage.TYPE = gwt_hm_BrowserChannel.MESSAGE_TYPE_INVOKE;
gwt_hm_InvokeMessage.receive = function(channel) {
    var methodName = channel.readString();
    var thisRef = channel.readValue();
    var numArgs = channel.readInt();
    var args = [];
    for (var idx = 0; idx < numArgs; idx++) {
        var val = channel.readValue();
        args.push(val);
    }
    return new gwt_hm_InvokeMessage(thisRef, methodName, numArgs, args);
}
gwt_hm_InvokeMessage.send = function(channel, thisRef, methodDispatchId, numArgs, args) {
    channel.ensureClear();
    channel.sendByte(gwt_hm_InvokeMessage.TYPE);
    channel.sendInt(methodDispatchId)
    channel.sendValue(thisRef)
    channel.sendInt(numArgs)
    for (var i = 0; i < numArgs; ++i) {
        channel.sendValue(args[i])
    }
}