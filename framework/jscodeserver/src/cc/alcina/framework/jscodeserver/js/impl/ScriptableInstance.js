class gwt_hm_ScriptableInstance {
  init(win) {
    this.win=win;
    return true;
  }
  connect(url, sessionId, codeServer, moduleName,
      hostedHtmlVersion){
    this.url=url;
    this.sessionId=sessionId;
    this.codeServer=codeServer;
    this.moduleName = moduleName;
    this.hostedHtmlVersion=hostedHtmlVersion;
    this.channel = new gwt_hm_HostChannel();
    var idx = codeServer.indexOf(":");
    var host = codeServer.substring(0,idx);
    var port = parseInt(codeServer.substring(idx+1));
    this.channel.connectToHost(host, port);
    if (!this.channel.init(this, gwt_hm_BrowserChannel.BROWSERCHANNEL_PROTOCOL_VERSION,
        gwt_hm_BrowserChannel.BROWSERCHANNEL_PROTOCOL_VERSION, this.hostedHtmlVersion)) {
      return false;
    }
    gwt_hm_LoadModuleMessage.send(this.channel, this.url, "", sessionId,
        moduleName, window.navigator.userAgent, this);
  }
  loadJsni(js){
    window.eval(js);
  }
}