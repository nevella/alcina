class gwt_hm_ServerMethods  {
    constructor() {
    }
}
gwt_hm_ServerMethods.freeJava = function(channel, handler, idCount,ids) {
  gwt_hm_FreeValueMessage.send(idCount,ids);
}