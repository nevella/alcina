class GwtJsPlugin {
    constructor() {

    };
    init(win) {
        this.win = win;
        return true;
    };
    connect(url, sessionId, codeServer, moduleName,
        hostedHtmlVersion) {
        this.url = url;
        this.sessionId = sessionId;
        this.codeServer = codeServer;
        this.moduleName = moduleName;
        this.hostedHtmlVersion = hostedHtmlVersion;
        let codeServerWs = null;
        //
        // always use codeServerWs
        //
        //
        //        if (window.location.search.indexOf("gwt.ws.server=") != -1) {
        //            codeServerWs = window.location.search.replace(/.*gwt.ws.server=([a-zA-Z0-9_:]+).*/, "$1");
        //        }
        let regexp = /(.+):([0-9]+)/;
        codeServerWs = codeServer.replace(regexp, "$1") + ":" + (parseInt(codeServer.replace(regexp, "$2")) + 1);
        this.codeServerWs = codeServerWs;
        var self = this;
        if (typeof gwt_hm_Message != "undefined") {
            self.connectAfterLoad.apply(self);
            return true;
        }
        var scriptNames = ["common/Message.js", "common/BrowserChannel.js", "impl/JavaObject.js", "impl/ScriptableInstance.js", "impl/LocalObjectTable.js", "common/ieee754.js", "common/Platform.js", "common/HashMap.js", "common/FatalErrorMessage.js", "common/HostChannel.js", "common/InvokeMessage.js", "common/LoadModuleMessage.js", "common/InvokeSpecialMessage.js", "common/AllowedConnections.js", "common/DebugLevel.js", "common/Socket.js", "common/Debug.js", "common/QuitMessage.js", "common/SwitchTransportMessage.js", "common/ProtocolVersionMessage.js", "common/ChooseTransportMessage.js", "common/SessionHandler.js", "common/ByteOrder.js", "common/ReturnMessage.js", "common/ServerMethods.js", "common/LoadJsniMessage.js", "common/Value.js", "common/CheckVersionsMessage.js", "common/FreeValueMessage.js", "common/WebSocketTransport.js", "common/WebSocketTransportClient.js"];
        scriptNames.forEach(function(scriptName) {
            var script = $doc.createElement('script');
            script.src = `/jscodeserver/${scriptName}`;
            document.getElementsByTagName('head')[0].appendChild(script);
        });
        window.setTimeout(function() {
            self.connectAfterLoad.apply(self);
        }, 250);
        return true;
    };
    connectAfterLoad() {
        this.scriptableInstance = new gwt_hm_ScriptableInstance();
        this.scriptableInstance.init(this.win);
        this.scriptableInstance.connect(this.url, this.sessionId, this.codeServer, this.moduleName,
            this.hostedHtmlVersion, this.codeServerWs);
    }

}
var __gwt_jsCodeServerPlugin = new GwtJsPlugin();