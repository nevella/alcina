class gwt_hm_ServerMethods {
    constructor() {}
}
gwt_hm_ServerMethods.freeJava = function(channel, handler, idCount, ids) {
    gwt_hm_FreeValueMessage.send(idCount, ids);
}
gwt_hm_ServerMethods.setProperty = function(channel, handler, objectId, dispId, value) {
    let args = [];
    args[0] = new gwt_hm_Value();
    args[1] = new gwt_hm_Value();
    args[2] = value;
    args[0].setInt(objectId);
    args[1].setInt(dispId);
    gwt_hm_InvokeSpecialMessage.send(channel, gwt_hm_BrowserChannel.SPECIAL_SET_PROPERTY, 3, args);
    return channel.reactToMessagesWhileWaitingForReturn(handler);
}
gwt_hm_ServerMethods.getProperty = function(channel, handler, objectId, dispId) {
  let args = [];
  args[0] = new gwt_hm_Value();
  args[1] = new gwt_hm_Value();
  args[0].setInt(objectId);
  args[1].setInt(dispId);
  gwt_hm_InvokeSpecialMessage.send(channel, gwt_hm_BrowserChannel.SPECIAL_GET_PROPERTY, 2, args);
  return channel.reactToMessagesWhileWaitingForReturn(handler);
}