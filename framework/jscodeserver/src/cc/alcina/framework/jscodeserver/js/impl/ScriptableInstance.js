class gwt_hm_ScriptableInstance {
    javaObjects = new Map();
    javaObjectsToFree = new Set();
    localObjects = new gwt_hm_LocalObjectTable();
    classes = [];
    channel;
    constructor() {}
    init(win) {
        this.win = win;
        return true;
    }
    connect(url, sessionId, codeServer, moduleName,
        hostedHtmlVersion) {
        this.url = url;
        this.sessionId = sessionId;
        this.codeServer = codeServer;
        this.moduleName = moduleName;
        this.hostedHtmlVersion = hostedHtmlVersion;
        this.channel = new gwt_hm_HostChannel();
        var idx = codeServer.indexOf(":");
        var host = codeServer.substring(0, idx);
        var port = parseInt(codeServer.substring(idx + 1));
        this.channel.connectToHost(host, port);
        if (!this.channel.init(this, gwt_hm_BrowserChannel.BROWSERCHANNEL_PROTOCOL_VERSION,
                gwt_hm_BrowserChannel.BROWSERCHANNEL_PROTOCOL_VERSION, this.hostedHtmlVersion)) {
            return false;
        }
        gwt_hm_LoadModuleMessage.send(this.channel, this.url, "", sessionId,
            moduleName, window.navigator.userAgent, this);
    }
    loadJsni(channel, js) {
        window.eval(js);
    }
    invoke(channel, _thisRef,
        methodName, numArgs, args) {
        var retValue = {
            value: null,
            exception: false
        };
        try {
            var thisRef = this.resolveLocal(_thisRef);
            //            console.log(thisRef);
            thisRef = (thisRef) ? thisRef : this.win;
            var varArgs = [];
            for (var idx = 0; idx < numArgs; idx++) {
                varArgs.push(this.resolveLocal(args[idx]));
            }
            var ret = this.win[methodName].apply(thisRef, varArgs);
            retValue.value = this.getAsValue(ret);
        } catch (e) {
            console.warn(e);
            retValue.value = this.getAsValue(e.toString());
            retValue.exception = true;
        }
        return retValue;
    }
    sendFreeValues(channel) {
        if (this.javaObjectsToFree.size) {
            var ids = [];
            for (let item of ids) {
                ids.push(item);
            }
            if (gwt_hm_ServerMethods.freeJava(channel, this, this.javaObjectsToFree.size, ids)) {
                this.javaObjectsToFree.clear();
            }
        }
    }
    //free js object refs
    freeValue(idCount, ids) {
        for (var idx = 0; idx < idCount; idx++) {
            this.localObjects.setFree(ids[idx]);
        }
    }
    getAsValue(value) {
        var scriptInstance = this;
        var val = new gwt_hm_Value();
        var unwrapJava = true;
        if (value === undefined) {
            val.setUndefined();
        } else if (value === null) {
            val.setNull();
        } else if (typeof value == "boolean") {
            val.setBoolean(value);
        } else if (Number.isInteger(value)) {
            val.setInt(value);
        } else if (typeof value == "number") {
            val.setDouble(value);
        } else if (typeof value == "string") {
            val.setString(value);
        } else if (typeof value == "object" || typeof value == "function") {
            if (unwrapJava && gwt_hm_JavaObject.isInstance(value)) {
                val.setJavaObjectId(gwt_hm_JavaObject.getJavaObjectId(value));
            } else {
                val.setJsObjectId(scriptInstance.getLocalObjectRef(value));
            }
        } else {
            throw "Unsupported NPVariant type " + val;
        }
        return val;
    }
    getLocalObjectRef(obj) {
        return this.localObjects.ensureObjectRef(obj);
    }
    resolveLocal(val) {
        switch (val.type) {
            case gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN:
                return val.getBoolean()
            case gwt_hm_BrowserChannel.VALUE_TYPE_BYTE:
                return val.getByte();
            case gwt_hm_BrowserChannel.VALUE_TYPE_CHAR:
                return val.getChar();
            case gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE:
                return val.getDouble();
            case gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT:
                return val.getFloat();
            case gwt_hm_BrowserChannel.VALUE_TYPE_INT:
                return val.getInt();
            case gwt_hm_BrowserChannel.VALUE_TYPE_LONG:
                return val.getLong();
            case gwt_hm_BrowserChannel.VALUE_TYPE_SHORT:
                return val.getShort();
            case gwt_hm_BrowserChannel.VALUE_TYPE_NULL:
                return null;
            case gwt_hm_BrowserChannel.VALUE_TYPE_STRING:
                return val.getString();
            case gwt_hm_BrowserChannel.VALUE_TYPE_JAVA_OBJECT:
                var id = val.getJavaObjectId();
                if (!this.javaObjects.has(id)) {
                    this.javaObjects.set(id, gwt_hm_JavaObject.create(this, id));
                }
                return this.javaObjects.get(id);
            case gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT:
                return this.localObjects.getById(val.getJsObjectId());
            case gwt_hm_BrowserChannel.VALUE_TYPE_UNDEFINED:
                return undefined
            default:
                throw "Unknown type";
        }
    }
    javaObjectInvoke(javaThisId, dispId, args, numArgs) {
        var varArgs = [];
        for (var idx = 0; idx < numArgs; idx++) {
            varArgs.push(this.getAsValue(args[idx]));
        }
        var javaThisValue = new gwt_hm_Value();
        javaThisValue.setJavaObjectId(javaThisId);
        gwt_hm_InvokeMessage.send(this.channel, javaThisValue, dispId, numArgs, varArgs);
        var ret = this.channel.reactToMessagesWhileWaitingForReturn(this);
        var retArr = [];
        retArr.push(ret.isException);
        if (ret.isException) {
            var debug = 3;
        }
        retArr.push(this.resolveLocal(ret.retValue));
        return retArr;
    }
    javaObjectSet(objectId, dispId, value) {
      var ret = gwt_hm_ServerMethods.setProperty(this.channel, this, objectId, dispId, this.getAsValue(value));
      if(ret.isException){
      //tostring
        throw ret.retValue.toString();
      }
    }
    javaObjectGet(objectId, dispId) {
        var ret = gwt_hm_ServerMethods.getProperty(this.channel, this, objectId, dispId);
        if(ret.isException){
          //tostring
          throw ret.retValue.toString();
        }
        return this.resolveLocal(ret.retValue);
    }
}