var $wnd = $wnd || window.parent;
var __gwtModuleFunction = $wnd.cc_alcina_extras_dev_console_remote_RemoteConsoleClient;
var $sendStats = __gwtModuleFunction.__sendStats;
$sendStats('moduleStartup', 'moduleEvalStart');
var $gwt_version = "0.0.0";
var $strongName = '';
var $gwt = {};
var $doc = $wnd.document;
var $moduleName, $moduleBase;
function __gwtStartLoadingFragment(frag) {
var fragFile = 'deferredjs/' + $strongName + '/' + frag + '.cache.js';
return __gwtModuleFunction.__startLoadingFragment(fragFile);
}
function __gwtInstallCode(code) {return __gwtModuleFunction.__installRunAsyncCode(code);}
function __gwt_isKnownPropertyValue(propName, propValue) {
return __gwtModuleFunction.__gwt_isKnownPropertyValue(propName, propValue);
}
function __gwt_getMetaProperty(name) {
return __gwtModuleFunction.__gwt_getMetaProperty(name);
}
var $stats = $wnd.__gwtStatsEvent ? function(a) {
return $wnd.__gwtStatsEvent && $wnd.__gwtStatsEvent(a);
} : null;
var $sessionId = $wnd.__gwtStatsSessionId ? $wnd.__gwtStatsSessionId : null;
/******************************************************************************
 * Variables used by the Plugin
 *****************************************************************************/
var $entry;
var $hostedHtmlVersion="2.1";

var $errFn;
var $moduleName;
var $moduleBase;
var __gwt_getProperty;

/******************************************************************************
 * WRITE ME - what does this invokes stuff do??? Probably related to invoking
 * calls...
 *****************************************************************************/
var __gwt_javaInvokes = [];

// Wrapper to call JS methods, which we need both to be able to supply a
// different this for method lookup and to get the exception back
function __gwt_jsInvoke(thisObj, methodName) {
 try {
   var args = Array.prototype.slice.call(arguments, 2);
   return [0, window[methodName].apply(thisObj, args)];
 } catch (e) {
   return [1, e];
 }
}

function __gwt_makeJavaInvoke(argCount) {
  return __gwt_javaInvokes[argCount] || __gwt_doMakeJavaInvoke(argCount);
}

function __gwt_doMakeJavaInvoke(argCount) {
  // IE6 won't eval() anonymous functions except as r-values
  var argList = "";
  for (var i = 0; i < argCount; i++) {
    argList += ",p" + i;
  }
  var argListNoComma = argList.substring(1);

  return eval(
    "__gwt_javaInvokes[" + argCount + "] =\n" +
    "  function(thisObj, dispId" + argList + ") {\n" +
    "    var result = __static(dispId, thisObj" + argList + ");\n" +
    "    if (result[0]) {\n" +
    "      throw result[1];\n" +
    "    } else {\n" +
    "      return result[1];\n" +
    "    }\n" +
    "  }\n"
  ); 
}


/******************************************************************************
 * Functions used to create tear-offs of Java methods.  Each function corresponds
 * to exactly one dispId, and also embeds the argument count.  We get the "this"
 * value from the context in which the function is being executed.
 * Function-object identity is preserved by caching in a sparse array.
 *****************************************************************************/
var __gwt_tearOffs = [];
var __gwt_tearOffGenerators = [];
function __gwt_makeTearOff(proxy, dispId, argCount) {
  return __gwt_tearOffs[dispId] || __gwt_doMakeTearOff(dispId, argCount);
}

function __gwt_doMakeTearOff(dispId, argCount) {
  return __gwt_tearOffs[dispId] = 
      (__gwt_tearOffGenerators[argCount] || __gwt_doMakeTearOffGenerator(argCount))(dispId);
}

function __gwt_doMakeTearOffGenerator(argCount) {
  // IE6 won't eval() anonymous functions except as r-values
  var argList = "";
  for (var i = 0; i < argCount; i++) {
    argList += ",p" + i;
  }
  var argListNoComma = argList.substring(1);

  return eval(
    "__gwt_tearOffGenerators[" + argCount + "] =\n" +
    "  function(dispId) {\n" +
    "    return function(" + argListNoComma + ") {\n" +
    "      var result = __static(dispId, this" + argList + ");\n" +
    "      if (result[0]) {\n" +
    "        throw result[1];\n" +
    "      } else {\n" +
    "        return result[1];\n" +
    "      }\n" +
    "    }\n" +
    "  }\n"
  ); 
}


/******************************************************************************
 *Code to give visual feedback when something goes wrong in Dev Mode
 *****************************************************************************/
function __gwt_disconnected() {
  // Prevent double-invocation.
  window.__gwt_disconnected = new Function();
  // Do it in a timeout so we can be sure we have a clean stack.
  window.setTimeout(__gwt_disconnected_impl, 1);
}

function __gwt_disconnected_impl() {
  __gwt_displayGlassMessage('GWT Code Server Disconnected',
      'Most likely, you closed GWT Development Mode. Or, you might have lost '
      + 'network connectivity. To fix this, try restarting GWT Development Mode and '
      + 'refresh this page.');
}

// Keep track of z-index to allow layering of multiple glass messages
var __gwt_glassMessageZIndex = 2147483647;

// Note this method is also used by ModuleSpace.java
function __gwt_displayGlassMessage(summary, details) {
  var topWin = window.top;
  var topDoc = topWin.document;
  var outer = topDoc.createElement("div");
  // Do not insert whitespace or outer.firstChild will get a text node.
  outer.innerHTML = 
    '<div style="position:absolute;z-index:' + __gwt_glassMessageZIndex-- +
    ';left:50px;top:50px;width:600px;color:#FFF;font-family:verdana;text-align:left;">' +
    '<div style="font-size:30px;font-weight:bold;">' + summary + '</div>' +
    '<div style="font-size:15px;">' + details + '</div>' +
    '</div>' +
    '<div style="position:absolute;z-index:' + __gwt_glassMessageZIndex-- +
    ';left:0px;top:0px;right:0px;bottom:0px;filter:alpha(opacity=60);opacity:0.6;background-color:#000;"></div>'
  ;
  topDoc.body.appendChild(outer);
  var glass = outer.firstChild;
  var glassStyle = glass.style;

  // Scroll to the top and remove scrollbars.
  topWin.scrollTo(0, 0);
  if (topDoc.compatMode == "BackCompat") {
    topDoc.body.style["overflow"] = "hidden";
  } else {
    topDoc.documentElement.style["overflow"] = "hidden";
  }

  // Steal focus.
  glass.focus();

  if ((navigator.userAgent.indexOf("MSIE") >= 0) && (topDoc.compatMode == "BackCompat")) {
    // IE quirks mode doesn't support right/bottom, but does support this.
    glassStyle.width = "125%";
    glassStyle.height = "100%";
  } else if (navigator.userAgent.indexOf("MSIE 6") >= 0) {
    // IE6 doesn't have a real standards mode, so we have to use hacks.
    glassStyle.width = "125%"; // Get past scroll bar area.
    // Nasty CSS; onresize would be better but the outer window won't let us add a listener IE.
    glassStyle.setExpression("height", "document.documentElement.clientHeight");
  }

  $doc.title = summary + " [" + $doc.title + "]";
}


/******************************************************************************
 * Other functions called by the Plugin
 *****************************************************************************/
function __gwt_makeResult(isException, result) {
  return [isException, result];
}

//should be prefixed with "__gwt_"
function fireOnModuleLoadStart(className) {
  $sendStats("moduleStartup", "onModuleLoadStart");
}


/******************************************************************************
 * Helper functions for the Development Mode startup code. Listed alphabetically
 *****************************************************************************/
function doBrowserSpecificFixes() {
  var ua = navigator.userAgent.toLowerCase();
  if (ua.indexOf("gecko") != -1) {
    // install eval wrapper on FF to avoid EvalError problem
    var __eval = window.eval;
    window.eval = function(s) {
      return __eval(s);
    }
  }
  if (ua.indexOf("chrome") != -1) {
    // work around __gwt_ObjectId appearing in JS objects
    var hop = window.Object.prototype.hasOwnProperty;
    window.Object.prototype.hasOwnProperty = function(prop) {
      return prop != "__gwt_ObjectId" && hop.call(this, prop);
    };
    var hop2 = window.Object.prototype.propertyIsEnumerable;
    window.Object.prototype.propertyIsEnumerable = function(prop) {
      return prop != "__gwt_ObjectId" && hop2.call(this, prop);
    };
    // do the same in the main window if it is different from our window
    if ($wnd != window) {
      var hop3 = $wnd.Object.prototype.hasOwnProperty;
      $wnd.Object.prototype.hasOwnProperty = function(prop) {
        return prop != "__gwt_ObjectId" && hop3.call(this, prop);
      };
      var hop4 = $wnd.Object.prototype.propertyIsEnumerable;
      $wnd.Object.prototype.propertyIsEnumerable = function(prop) {
        return prop != "__gwt_ObjectId" && hop4.call(this, prop);
      };
    }
  }
}

function embedPlugin() {
  var embed = document.createElement('embed');
  embed.id = 'pluginEmbed';
  embed.type = 'application/x-gwt-hosted-mode';
  embed.width = '10';
  embed.height = '20';

  var obj = document.createElement('object');
  obj.id = 'pluginObject';
  obj.classid = 'clsid:1D6156B6-002B-49E7-B5CA-C138FB843B4E';

  document.body.appendChild(embed);
  document.body.appendChild(obj);
}

function findPluginObject() {
  try {
    return document.getElementById('pluginObject');
  } catch (e) {
    return null;
  }
}

function findPluginEmbed() {
  try {
    return document.getElementById('pluginEmbed')
  } catch (e) {
    return null;
  }
}

function findPluginXPCOM() {
  try {
    return __gwt_HostedModePlugin;
  } catch (e) {
    return null;
  }
}
function loadPluginJsCodeServer() {
  try {
    var script = $doc.createElement('script');
    script.src = "/jscodeserver/GwtJsPlugin.js";
    document.getElementsByTagName('head')[0].appendChild(script);
  } catch (e) {
  }
}
function findPluginJsCodeServer() {
  try {
    return __gwt_jsCodeServerPlugin;
  } catch (e) {
    return null;
  }
}

function getCodeServer() {
  var server = "localhost:9997";
  var query = $wnd.location.search;
  var idx = query.indexOf("gwt.codesvr=");
  if (idx >= 0) {
    idx += 12;  // "gwt.codesvr=".length == 12
  } else {
    idx = query.indexOf("gwt.codesvr.cc.alcina.extras.dev.console.remote.RemoteConsoleClient=");
    idx += (13 + "cc.alcina.extras.dev.console.remote.RemoteConsoleClient".length);  // 
  }
  if (idx >= 0) {
    var amp = query.indexOf("&", idx);
    if (amp >= 0) {
      server = query.substring(idx, amp);
    } else {
      server = query.substring(idx);
    }
    // According to RFC 3986, some of this component's characters (e.g., ':')
    // are reserved and *may* be escaped.
    return decodeURIComponent(server);
  }
}

function generateSessionId() {
  var ASCII_EXCLAMATION = 33;
  var ASCII_TILDE = 126;
  var chars = [];
  for (var i = 0; i < 16; ++i) {
    chars.push(Math.floor(ASCII_EXCLAMATION
    + Math.random() * (ASCII_TILDE - ASCII_EXCLAMATION + 1)));
  }
  return String.fromCharCode.apply(null, chars);
}

function loadIframe(url) {
  var topDoc = window.top.document;

  // create an iframe
  var iframeDiv = topDoc.createElement("div");
  iframeDiv.innerHTML = "<iframe scrolling=no frameborder=0 src='" + url + "'>";
  var iframe = iframeDiv.firstChild;

  // mess with the iframe style a little
  var iframeStyle = iframe.style;
  iframeStyle.position = "absolute";
  iframeStyle.borderWidth = "0";
  iframeStyle.left = "0";
  iframeStyle.top = "0";
  iframeStyle.width = "100%";
  iframeStyle.backgroundColor = "#ffffff";
  iframeStyle.zIndex = "1";
  iframeStyle.height = "100%";

  // update the top window's document's body's style
  var hostBodyStyle = window.top.document.body.style; 
  hostBodyStyle.margin = "0";
  hostBodyStyle.height = iframeStyle.height;
  hostBodyStyle.overflow = "hidden";

  // insert the iframe
  topDoc.body.insertBefore(iframe, topDoc.body.firstChild);
}

function pluginConnectionError(codeServer) {
  __gwt_displayGlassMessage(
      "Plugin failed to connect to Development Mode server at " + simpleEscape(codeServer),
      "Follow the troubleshooting instructions at "
      + "<a href='http://code.google.com/p/google-web-toolkit/wiki/TroubleshootingOOPHM'>"
      + "http://code.google.com/p/google-web-toolkit/wiki/TroubleshootingOOPHM</a>");
  if ($errFn) {
    $errFn($moduleName);
  }
}

function simpleEscape(originalString) {
  return originalString.replace(/&/g,"&amp;")
    .replace(/</g,"&lt;")
    .replace(/>/g,"&gt;")
    .replace(/\'/g, "&#39;")
    .replace(/\"/g,"&quot;");
}

function tryConnectingToPlugin(sessionId, url) {
  // Note that the order is important
  var pluginFinders = [findPluginXPCOM, findPluginObject, findPluginEmbed, findPluginJsCodeServer];
  var codeServer = getCodeServer();
  var plugin = null;
  for (var i = 0; i < pluginFinders.length; ++i) {
    try {
      var maybePlugin = pluginFinders[i]();
      if (maybePlugin != null && maybePlugin.init(window)) {
        plugin = maybePlugin;
        break;
      }
    } catch (e) {
    }
  }

  if (plugin == null) {
    // Plugin initialization failed. Show the missing-plugin page.
    return null;
  }
  if (!plugin.connect(url, sessionId, codeServer, $moduleName,
                      $hostedHtmlVersion)) {
    // Connection failed. Show the error alert and troubleshooting page.
    pluginConnectionError(codeServer);
  }

  return plugin;
}


/******************************************************************************
 * Development Mode startup code
 *****************************************************************************/
function gwtOnLoad(errFn, moduleName, moduleBase, softPermutationId, computePropValue) {
  if(typeof __gwt_jsCodeServerPlugin == "undefined"){
    loadPluginJsCodeServer();
    window.setTimeout( function(){
      gwtOnLoad0(errFn, moduleName, moduleBase, softPermutationId, computePropValue);
    },50);
  }else{
    gwtOnLoad0(errFn, moduleName, moduleBase, softPermutationId, computePropValue);
  }
}
function gwtOnLoad0(errFn, moduleName, moduleBase, softPermutationId, computePropValue) {
  $errFn = errFn;
  $moduleName = moduleName;
  $moduleBase = moduleBase;
  __gwt_getProperty = computePropValue;
  
  doBrowserSpecificFixes();

  if (!findPluginXPCOM()) {
    embedPlugin();
  }

  var topWin = window.top;
  if (!topWin.__gwt_SessionID) {
    topWin.__gwt_SessionID = generateSessionId();
  }

  var plugin = tryConnectingToPlugin(topWin.__gwt_SessionID, topWin.location.href);
  if (plugin == null) {
    loadIframe("http://www.gwtproject.org/missing-plugin/");
  } else {
    // take over the onunload function, wrapping any existing call if it exists
    var oldUnload = window.onunload;
    window.onunload = function() {
      // run wrapped unload first in case it is running gwt code
      !!oldUnload && oldUnload();
      try {
        // wrap in try/catch since plugins are not required to supply this
        plugin.disconnect();
      } catch (e) {
      }
    };
  }
}

class GwtJsPlugin{
  constructor(){
    
  };
  init(win){
    this.win=win;
    return true;
  };
  connect(url, sessionId, codeServer, moduleName,
      hostedHtmlVersion){
    this.url=url;
    this.sessionId=sessionId;
    this.codeServer=codeServer;
    this.moduleName = moduleName;
    this.hostedHtmlVersion=hostedHtmlVersion;
    var self=this;
    if(typeof gwt_hm_Message != "undefined"){
      self.connectAfterLoad.apply(self);
      return true;
    }
    var scriptNames=["common/Message.js","common/BrowserChannel.js","impl/JavaObject.js","impl/ScriptableInstance.js","impl/LocalObjectTable.js","common/ieee754.js","common/Platform.js","common/HashMap.js","common/FatalErrorMessage.js","common/HostChannel.js","common/InvokeMessage.js","common/LoadModuleMessage.js","common/InvokeSpecialMessage.js","common/AllowedConnections.js","common/DebugLevel.js","common/Socket.js","common/Debug.js","common/QuitMessage.js","common/SwitchTransportMessage.js","common/ProtocolVersionMessage.js","common/ChooseTransportMessage.js","common/SessionHandler.js","common/ByteOrder.js","common/ReturnMessage.js","common/ServerMethods.js","common/LoadJsniMessage.js","common/Value.js","common/CheckVersionsMessage.js","common/FreeValueMessage.js"];
    scriptNames.forEach(function(scriptName){
      var script = $doc.createElement('script');
      script.src = `/jscodeserver/${scriptName}`;
      document.getElementsByTagName('head')[0].appendChild(script);
    });
    window.setTimeout(function(){
      self.connectAfterLoad.apply(self);
    },250);
    return true;
  };
  connectAfterLoad(){
    this.scriptableInstance = new gwt_hm_ScriptableInstance();
    this.scriptableInstance.init(this.win);
    this.scriptableInstance.connect(this.url, this.sessionId, this.codeServer, this.moduleName,
        this.hostedHtmlVersion)
  }
  
}
var __gwt_jsCodeServerPlugin = new GwtJsPlugin();

class gwt_hm_Message {
  constructor(){
    
  }
  getType() {
    throw "abstract";
  }
  isAsynchronous() {
    return false;
  }
}



/* from BrowserChannel.BROWSERCHANNEL_PROTOCOL_VERSION */
class gwt_hm_BrowserChannel{
  
}
gwt_hm_BrowserChannel.BROWSERCHANNEL_PROTOCOL_VERSION=2

/* from com.google.gwt.dev.shell.BrowserChannel.SpecialDispatchId */
gwt_hm_BrowserChannel.SPECIAL_HAS_METHOD=0
gwt_hm_BrowserChannel.SPECIAL_HAS_PROPERTY=1
gwt_hm_BrowserChannel.SPECIAL_GET_PROPERTY=2
gwt_hm_BrowserChannel.SPECIAL_SET_PROPERTY=3

/* from com.google.gwt.dev.shell.BrowserChannel.MessageType */
gwt_hm_BrowserChannel.MESSAGE_TYPE_INVOKE=0
gwt_hm_BrowserChannel.MESSAGE_TYPE_RETURN=1
gwt_hm_BrowserChannel.MESSAGE_TYPE_OLD_LOAD_MODULE=2
gwt_hm_BrowserChannel.MESSAGE_TYPE_QUIT=3
gwt_hm_BrowserChannel.MESSAGE_TYPE_LOADJSNI=4
gwt_hm_BrowserChannel.MESSAGE_TYPE_INVOKESPECIAL=5
gwt_hm_BrowserChannel.MESSAGE_TYPE_FREEVALUE=6
gwt_hm_BrowserChannel.MESSAGE_TYPE_FATAL_ERROR=7
gwt_hm_BrowserChannel.MESSAGE_TYPE_CHECK_VERSIONS=8
gwt_hm_BrowserChannel.MESSAGE_TYPE_PROTOCOL_VERSION=9
gwt_hm_BrowserChannel.MESSAGE_TYPE_CHOOSE_TRANSPORT=10
gwt_hm_BrowserChannel.MESSAGE_TYPE_SWITCH_TRANSPORT=11
gwt_hm_BrowserChannel.MESSAGE_TYPE_LOAD_MODULE=12

/* from com.google.gwt.dev.shell.BrowserChannel.Value.ValueType */
gwt_hm_BrowserChannel.VALUE_TYPE_NULL=0
gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN=1
gwt_hm_BrowserChannel.VALUE_TYPE_BYTE=2
gwt_hm_BrowserChannel.VALUE_TYPE_CHAR=3
gwt_hm_BrowserChannel.VALUE_TYPE_SHORT=4
gwt_hm_BrowserChannel.VALUE_TYPE_INT=5
gwt_hm_BrowserChannel.VALUE_TYPE_LONG=6
gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT=7
gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE=8
gwt_hm_BrowserChannel.VALUE_TYPE_STRING=9
gwt_hm_BrowserChannel.VALUE_TYPE_JAVA_OBJECT=10
gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT=11
gwt_hm_BrowserChannel.VALUE_TYPE_UNDEFINED=12


//just statics - we need a function with extras
class gwt_hm_JavaObject {
}
gwt_hm_JavaObject.isInstance = function(javaObject) {
    return (javaObject) && javaObject.hasOwnProperty("__gwt_java_object_id");
}
gwt_hm_JavaObject.getJavaObjectId = function(javaObject) {
  return javaObject.__gwt_java_object_id;
}
gwt_hm_JavaObject.dispatch = function(javaObject,sourceArguments) {
  var args = [];
  for(var idx=2;idx<sourceArguments.length;idx++){
    args.push(sourceArguments[idx]);
  }
  var dispId = sourceArguments[0];
  var objectId = gwt_hm_JavaObject.getJavaObjectId(javaObject);
  var thisObj = sourceArguments[1];
  if (gwt_hm_JavaObject.isInstance(thisObj)) {
      objectId = gwt_hm_JavaObject.getJavaObjectId(thisObj);
  }
  return javaObject.__gwt_plugin.javaObjectInvoke(objectId, dispId, args, args.length);
}
gwt_hm_JavaObject.propertyDispatcher={
    set: function(javaObject, prop, value) {
      var objectId = javaObject.__gwt_java_object_id;
      var dispId = prop;
      return javaObject.__gwt_plugin.javaObjectSet(objectId, dispId, value);
    },
    get: function(javaObject, prop) {
      var objectId = javaObject.__gwt_java_object_id;
      var dispId = prop;
      if(isNaN(parseInt(dispId))){
        // string-valued -- e.g. hasOwnProperty
        return javaObject[prop];
       }
      return javaObject.__gwt_plugin.javaObjectGet(objectId, dispId);
    },
    has: function(javaObject, prop) {
      throw "nope";
      return javaObject.hasOwnProperty(prop);
    }
};
gwt_hm_JavaObject.create = function(plugin,id) {
    var dispatcher = function() {
      // we use a function rather than an object because the original NPAPI impl
      // expects a "default" call target:
      //
      // e.g. __static(55) and not __static.callRemote(55)
      //
      // we then use power-of-js to add properties to the function (and then
      // proxy...lordy)
      //
      // works though. could rewrite without function/proxy by rewriting
      // generated js in hostedmode -
      // but no need, that would be for pre-proxy js engines
      return gwt_hm_JavaObject.dispatch(arguments.callee,arguments);
    }
    dispatcher.__gwt_java_object_id = id; 
    dispatcher.__gwt_plugin = plugin;
    return new Proxy(dispatcher, gwt_hm_JavaObject.propertyDispatcher);
}

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

class gwt_hm_LocalObjectTable {
    objects = new Map();
    ids = new Map();
    nextId = 0;
    dontFree = false;
    jsIdentitySafe = true;
    constructor() {}
    setFree(id) {
        var obj = this.getById(id);
        this.objects.remove(id);
        this.ids.remove(obj);
    }
    add(obj) {
        var id = this.nextId++;
        this.set(id, obj);
        if (!this.jsIdentitySafe) {
            throw "not implemented";
        }
    }
    set(id, obj) {
        if (!this.jsIdentitySafe) {
            throw "not implemented";
        }
        this.objects.set(id, obj);
        this.ids.set(obj, id);
    }
    freeAll() {
        this.objects.clear();
        this.ids.clear();
    }
    getById(id) {
        if (!this.objects.has(id)) {
            throw `Object not found: ${id}`;
        }
        return this.objects.get(id);
    }
    getObjectId(object) {
        if (!this.ids.has(object)) {
            throw `Id not found: ${object}`;
        }
        return this.ids.get(object);
    }
    ensureObjectRef(obj) {
      if (!this.ids.has(obj)) {
          this.add(obj);
      }
      return this.ids.get(obj);
  }
   
}

var ieee754={}
ieee754.read = function (buffer, offset, isLE, mLen, nBytes) {
  var e, m
  var eLen = (nBytes * 8) - mLen - 1
  var eMax = (1 << eLen) - 1
  var eBias = eMax >> 1
  var nBits = -7
  var i = isLE ? (nBytes - 1) : 0
  var d = isLE ? -1 : 1
  var s = buffer[offset + i]

  i += d

  e = s & ((1 << (-nBits)) - 1)
  s >>= (-nBits)
  nBits += eLen
  for (; nBits > 0; e = (e * 256) + buffer[offset + i], i += d, nBits -= 8) {}

  m = e & ((1 << (-nBits)) - 1)
  e >>= (-nBits)
  nBits += mLen
  for (; nBits > 0; m = (m * 256) + buffer[offset + i], i += d, nBits -= 8) {}

  if (e === 0) {
    e = 1 - eBias
  } else if (e === eMax) {
    return m ? NaN : ((s ? -1 : 1) * Infinity)
  } else {
    m = m + Math.pow(2, mLen)
    e = e - eBias
  }
  return (s ? -1 : 1) * m * Math.pow(2, e - mLen)
}

ieee754.write = function (buffer, value, offset, isLE, mLen, nBytes) {
  var e, m, c
  var eLen = (nBytes * 8) - mLen - 1
  var eMax = (1 << eLen) - 1
  var eBias = eMax >> 1
  var rt = (mLen === 23 ? Math.pow(2, -24) - Math.pow(2, -77) : 0)
  var i = isLE ? 0 : (nBytes - 1)
  var d = isLE ? 1 : -1
  var s = value < 0 || (value === 0 && 1 / value < 0) ? 1 : 0

  value = Math.abs(value)

  if (isNaN(value) || value === Infinity) {
    m = isNaN(value) ? 1 : 0
    e = eMax
  } else {
    e = Math.floor(Math.log(value) / Math.LN2)
    if (value * (c = Math.pow(2, -e)) < 1) {
      e--
      c *= 2
    }
    if (e + eBias >= 1) {
      value += rt / c
    } else {
      value += rt * Math.pow(2, 1 - eBias)
    }
    if (value * c >= 2) {
      e++
      c /= 2
    }

    if (e + eBias >= eMax) {
      m = 0
      e = eMax
    } else if (e + eBias >= 1) {
      m = ((value * c) - 1) * Math.pow(2, mLen)
      e = e + eBias
    } else {
      m = value * Math.pow(2, eBias - 1) * Math.pow(2, mLen)
      e = 0
    }
  }

  for (; mLen >= 8; buffer[offset + i] = m & 0xff, i += d, m /= 256, mLen -= 8) {}

  e = (e << mLen) | m
  eLen += mLen
  for (; eLen > 0; buffer[offset + i] = e & 0xff, i += d, e /= 256, eLen -= 8) {}

  buffer[offset + i - d] |= s * 128
}







class gwt_hm_HostChannel {
    // codepoints (binary string)
    buf_out = "";
    // codepoints (binary string)
    buf_in;
    buf_in_idx;
    handler;
    channelId;
    closeSocket = false;
    tcpHost = "";
    connectToHost(host, port) {
        // ignore host (for the mo) - assume local, otherwise will need CORS
        //
        //correction, always use local (rather than routing through remote) - network round trips more than compensates
        //      this.host = "";
        this.host = "http://127.0.0.1:10005"
        this.port = port;
    }
    init(handler, minVersion, maxVersion,
        hostedHtmlVersion) {
        this.handler = handler;
        gwt_hm_CheckVersionsMessage.send(this, minVersion, maxVersion, hostedHtmlVersion);
        var type = this.readByte();
        switch (type) {
            case gwt_hm_BrowserChannel.MESSAGE_TYPE_PROTOCOL_VERSION:
                {
                    var message = gwt_hm_ProtocolVersionMessage.receive(this);
                    break;
                }
            case MESSAGE_TYPE_FATAL_ERROR:
                {
                    var message = gwt_hm_FatalErrorMessage.receive(this);
                    handler.fatalError(this, message.getError());
                    return false;
                }
            default:
                return false;
        }
        var self = this;
        window.addEventListener("unload", function(event) {
            self.disconnectFromHost();
        });
        return true;
    }
    isConnected() {
        return true
    }
    readBytes(dataLen) {
        var count = dataLen;
        var buf = "";
        while (count > 0) {
            buf += this.readByte();
            --count;
        }
    }
    sendBytes(data) {
        for (var idx = 0; idx < data.length(); idx++) {
            this.sendByte(data.charCodeAt(idx));
        }
    }
    readInt() {
        var b0 = this.readByte();
        var b1 = this.readByte();
        var b2 = this.readByte();
        var b3 = this.readByte();
        return (b0 << 24) + (b1 << 16) + (b2 << 8) + b3;
    }
    sendInt(v) {
        this.sendByte((v >>> 24) & 0xFF);
        this.sendByte((v >>> 16) & 0xFF);
        this.sendByte((v >>> 8) & 0xFF);
        this.sendByte((v >>> 0) & 0xFF);
    }
    readShort() {
        var b0 = this.readByte();
        var b1 = this.readByte();
        return (b0 << 8) + b1;
    }
    sendShort(v) {
        out.write((v >>> 8) & 0xFF);
        out.write((v >>> 0) & 0xFF);
    }
    readLong() {
        var i0 = this.readInt(data);
        var i1 = this.readInt(data);
        var ret = {
            l: i1 & gwt_hm_HostChannel.long_MASK,
            m: i1 >>> gwt_hm_HostChannel.long_BITS | ((i0 << 10) & gwt_hm_HostChannel.long_MASK),
            h: (i0 >>> 10) & gwt_hm_HostChannel.long_MASK
        };
        return ret;
    }
    sendLong(v) {
        var i0 = ((v.m >>> 10) & gwt_hm_HostChannel.long_MASK) | (v.h << 12);
        var i1 = v.l | (v.m << gwt_hm_HostChannel.long_BITS);
        this.sendInt(io);
        this.sendInt(i1);
    }
    readFloat() {
        var v = [this.readByte(), this.readByte(), this.readByte(), this.readByte()];
        return ieee754.read(v, 0, false, 23, 4);
    }
    sendFloat(v) {
        var buf = [];
        ieee754.write(buf, v, 0, false, 23, 4);
        for (var idx = 0; idx <= 3; idx++) {
            this.sendByte(buf[idx]);
        }
    }
    readDouble() {
        var v = [this.readByte(), this.readByte(), this.readByte(), this.readByte(), this.readByte(), this.readByte(), this.readByte(), this.readByte()];
        return ieee754.read(v, 0, false, 52, 8);
    }
    sendDouble(v) {
        var buf = [];
        ieee754.write(buf, v, 0, false, 52, 8);
        for (var idx = 0; idx <= 7; idx++) {
            this.sendByte(buf[idx]);
        }
    }
    readByte() {
        if (this.buf_in_idx >= this.buf_in.length) {
            throw "stream exhausted";
        }
        return this.buf_in.charCodeAt(this.buf_in_idx++);
    }
    sendByte(c) {
        if (this.buf_out.length == 0) {
//            console.log(`send >> ${c}`);
        }
        this.buf_out += String.fromCharCode(c);
    }
    readStringLength() {
        return this.readInt();
    }
    readString() {
        var len = this.readInt();
        var utf8 = this.buf_in.substring(this.buf_in_idx, this.buf_in_idx + len);
        var ret = this.utf8BinaryStringToStr(utf8);
        this.buf_in_idx += len;
        return ret;
    }
    sendString(str) {
        var utf8 = this.utf16ToUtf8(str);
        this.sendInt(utf8.length);
        this.buf_out += utf8;
    }
    utf8BinaryStringToStr(str) {
        var utf8safe = true;
        for (var i = 0; i < str.length; i++) {
            var charcode = str.charCodeAt(i);
            if (charcode > 0x80) {
                utf8safe = false;
                break;
            }
        }
        if (utf8safe) {
            return str;
        }
        var out, i, len, c;
        var char2, char3;
        out = "";
        len = str.length;
        i = 0;
        while (i < len) {
            c = str.charCodeAt(i++);
            switch (c >> 4) {
                case 0:
                case 1:
                case 2:
                case 3:
                case 4:
                case 5:
                case 6:
                case 7:
                    // 0xxxxxxx
                    out += String.fromCharCode(c);
                    break;
                case 12:
                case 13:
                    // 110x xxxx 10xx xxxx
                    char2 = str.charCodeAt(i++);
                    out += String.fromCharCode(((c & 0x1F) << 6) | (char2 & 0x3F));
                    break;
                case 14:
                    // 1110 xxxx 10xx xxxx 10xx xxxx
                    char2 = str.charCodeAt(i++);
                    char3 = str.charCodeAt(i++);
                    out += String.fromCharCode(((c & 0x0F) << 12) |
                        ((char2 & 0x3F) << 6) |
                        ((char3 & 0x3F) << 0));
                    break;
            }
        }
        return out;
    }
    utf16ToUtf8(str) {
        var utf8safe = true;
        for (var i = 0; i < str.length; i++) {
            var charcode = str.charCodeAt(i);
            if (charcode > 0x80) {
                utf8safe = false;
                break;
            }
        }
        if (utf8safe) {
            return str;
        }
        var utf8 = "";
        var lastCheck = 0;
        for (var i = 0; i < str.length; i++) {
            var charcode = str.charCodeAt(i);
            if (charcode < 0x80) {
                utf8 += String.fromCharCode(charcode);
            } else if (charcode < 0x800) {
                utf8 += String.fromCharCode(0xc0 | (charcode >> 6));
                utf8 += String.fromCharCode(0x80 | (charcode & 0x3f));
            } else if (charcode < 0xd800 || charcode >= 0xe000) {
                utf8 += String.fromCharCode(0xe0 | (charcode >> 12));
                utf8 += String.fromCharCode(0x80 | ((charcode >> 6) & 0x3f));
                utf8 += String.fromCharCode(0x80 | (charcode & 0x3f));
            }
            // surrogate pair
            else {
                i++;
                charcode = ((charcode & 0x3ff) << 10) | (str.charCodeAt(i) & 0x3ff)
                utf8 += String.fromCharCode(0xf0 | (charcode >> 18));
                utf8 += String.fromCharCode(0x80 | ((charcode >> 12) & 0x3f));
                utf8 += String.fromCharCode(0x80 | ((charcode >> 6) & 0x3f));
                utf8 += String.fromCharCode(0x80 | (charcode & 0x3f));
            }
            for (; lastCheck < utf8.length; lastCheck++) {
                if (utf8.charCodeAt(lastCheck) > 0xff) {
                    debugger;
                }
            }
        }
        return utf8;
    }
    readValue() {
        var type = this.readByte();
        var value = new gwt_hm_Value();
        switch (type) {
            case gwt_hm_BrowserChannel.VALUE_TYPE_NULL:
                value.setNull();
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_UNDEFINED:
                value.setUndefined();
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN:
                {
                    var val = this.readByte();
                    value.setBoolean(val != 0);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_BYTE:
                {
                    var val = this.readByte();
                    value.setByte(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_CHAR:
                {
                    var val = this.readShort();
                    value.setChar(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_SHORT:
                {
                    var val = this.readShort();
                    value.setShort(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_STRING:
                {
                    var val = this.readString();
                    value.setString(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_INT:
                {
                    var val = this.readInt();
                    value.setInt(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_LONG:
                {
                    var val = this.readLong();
                    value.setLong(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE:
                {
                    var val = this.readDouble();
                    value.setDouble(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_JAVA_OBJECT:
                {
                    var val = this.readInt();
                    value.setJavaObjectId(val);
                }
                return value;
            case gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT:
                {
                    var val = this.readInt();
                    value.setJsObjectId(val);
                }
                return value;
            default:
                throw "Unhandled value type sent from server: " + type;
        }
        return false;
    }
    sendValue(value) {
        var type = value.type;
        this.sendByte(type);
        switch (type) {
            case gwt_hm_BrowserChannel.VALUE_TYPE_NULL:
            case gwt_hm_BrowserChannel.VALUE_TYPE_UNDEFINED:
                return;
            case gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN:
                return this.sendByte(value.getBoolean() ? 1 : 0);
            case gwt_hm_BrowserChannel.VALUE_TYPE_BYTE:
                return this.sendByte(value.getByte());
            case gwt_hm_BrowserChannel.VALUE_TYPE_CHAR:
                return this.sendShort(short(value.getChar()));
            case gwt_hm_BrowserChannel.VALUE_TYPE_SHORT:
                return this.sendShort(value.getShort());
            case gwt_hm_BrowserChannel.VALUE_TYPE_INT:
                return this.sendInt(value.getInt());
            case gwt_hm_BrowserChannel.VALUE_TYPE_LONG:
                return this.sendLong(value.getLong());
            case gwt_hm_BrowserChannel.VALUE_TYPE_STRING:
                return this.sendString(value.getString());
            case gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE:
                return this.sendDouble(value.getDouble());
            case gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT:
                return this.sendFloat(value.getFloat());
            case gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT:
                return this.sendInt(value.getJsObjectId());
            case gwt_hm_BrowserChannel.VALUE_TYPE_JAVA_OBJECT:
                return this.sendInt(value.getJavaObjectId());
            default:
                throw "Unhandled value type sent to server: " + type;
        }
    }
    reactToMessages(handler, expectReturn) {
        try {
            return this.reactToMessagesOrThrow(handler, expectReturn);
        } catch (e) {
            console.warn(e);
            this.disconnectFromHost();
        }
    }
    reactToMessagesOrThrow(handler, expectReturn) {
        while (true) {
            this.flush();
            var type = this.readByte(type);
//            console.log(`message: ${this.messageId} :: ${type} `);
            switch (type) {
                case gwt_hm_BrowserChannel.MESSAGE_TYPE_INVOKE:
                    {
                        var message = gwt_hm_InvokeMessage.receive(this);
                        if (parseInt(this.messageId) > 0) {
//                            console.log(`invoke: ${this.messageId} :: ${message.methodName} [${message.thisRef.intValue}]`);
                        }
                        var result = handler.invoke(this, message.thisRef, message.methodName,
                            message.numArgs, message.args);
                        handler.sendFreeValues(this);
                        gwt_hm_ReturnMessage.send(this, result.exception, result.value);
                    }
                    break;
                case gwt_hm_BrowserChannel.MESSAGE_TYPE_INVOKESPECIAL:
                    {
                        // scottb: I think this is never used; I think server
                        // never sends invokeSpecial
                        var message = gwt_hm_InvokeSpecialMessage.receive(this);
                        var result = handler.invokeSpecial(this, message._dispatchId, message.methodName,
                            message.numArgs, message.args);
                        handler.sendFreeValues(this);
                        gwt_hm_ReturnMessage.send(this, result.exception, result.value);
                    }
                    break;
                case gwt_hm_BrowserChannel.MESSAGE_TYPE_FREEVALUE:
                    {
                        var message = gwt_hm_FreeValueMessage.receive(this);
                        handler.freeValue(this, message.idCount, message.ids);
                    }
                    // do not send a response
                    break;
                case gwt_hm_BrowserChannel.MESSAGE_TYPE_LOADJSNI:
                    {
                        var message = gwt_hm_LoadJsniMessage.receive(this);
                        handler.loadJsni(this, message.js);
                    }
                    // do not send a response
                    break;
                case gwt_hm_BrowserChannel.MESSAGE_TYPE_RETURN:
                    if (!expectReturn) {
                        throw "Received unexpected RETURN";
                    }
                    return gwt_hm_ReturnMessage.receive(this);
                case gwt_hm_BrowserChannel.MESSAGE_TYPE_QUIT:
                    if (expectReturn) {
                        throw "Received QUIT while waiting for return";
                    }
                    this.disconnectFromHost();
                    return 0;
                default:
                    // TODO(jat): error handling
                    throw "Unexpected message type " + type;
            }
        }
    }
    reactToMessagesWhileNotWaitingForReturn(handler) {
        return !this.reactToMessages(handler, false);
    }
    flush() {
        let body = null;
        try {
            body = btoa(this.buf_out);
        } catch (e) {
            debugger;
        }
        this.buf_out = "";
        this.flushWithBody(body);
    }
    flushWithBody(body) {
        var xhr = new XMLHttpRequest();
        var url = `${this.host}/jsCodeServer.tcp`;
        xhr.open("POST", url, false);
        xhr.setRequestHeader("XhrTcpBridge.codeserver_port", this.port);
        if (this.channelId) {
            xhr.setRequestHeader("XhrTcpBridge.handle_id", this.channelId);
        }
        if (this.closeSocket) {
            xhr.setRequestHeader("XhrTcpBridge.meta", "close_socket");
        }
        try {
            xhr.send(body);
        } catch (e) {
            if (this.channelId || this.host) {
                throw e;
            } else {
                // retry with alt code server;
                this.host = "http://127.0.0.1:10005";
                this.flushWithBody(body);
                return;
            }
        }
        if (this.closeSocket) {
            return;
        }
        var xhrChannelId = xhr.getResponseHeader("XhrTcpBridge.handle_id");
        this.messageId = xhr.getResponseHeader("XhrTcpBridge.message_id");
        if (this.channelId && this.channelId != xhrChannelId) {
            throw "Different channel id";
        }
        this.channelId = xhrChannelId;
        this.buf_in = atob(xhr.responseText);
        this.buf_in_idx = 0;
    }
    ensureClear() {
        if (this.buf_out.length > 0) {
            throw "pending message";
        }
    }
    reactToMessagesWhileWaitingForReturn(handler) {
        return this.reactToMessages(handler, true);
    }
    disconnectFromHost() {
        new gwt_hm_QuitMessage.send(this);
    }
}
gwt_hm_HostChannel.long_BITS = 22;
gwt_hm_HostChannel.long_BITS01 = 2 * gwt_hm_HostChannel.long_BITS;
gwt_hm_HostChannel.long_BITS2 = 64 - gwt_hm_HostChannel.long_BITS01;
gwt_hm_HostChannel.long_MASK = (1 << gwt_hm_HostChannel.long_BITS) - 1;
gwt_hm_HostChannel.long_MASK_2 = (1 << gwt_hm_HostChannel.long_BITS2) - 1;

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

class gwt_hm_AllowedConnections {
  /*
   * static bool matchesRule(const std::string& webHost, const std::string&
   * codeServer, bool* allowed);
   */
  
  matchesRule(webHost,codeServer){
    return true;
  }

}










class gwt_hm_QuitMessage extends gwt_hm_Message {
    version;
    constructor(version) {
        super();
        this.version = version;
    }
}
gwt_hm_QuitMessage.TYPE = gwt_hm_BrowserChannel.MESSAGE_TYPE_QUIT;
gwt_hm_QuitMessage.send = function(channel) {
    channel.closeSocket=true;
    channel.sendByte(gwt_hm_QuitMessage.TYPE);
    channel.flush();
}



class gwt_hm_ProtocolVersionMessage extends gwt_hm_Message {
    version;
    constructor(version) {
        super();
        this.version = version;
    }
}
gwt_hm_ProtocolVersionMessage.TYPE = gwt_hm_BrowserChannel.MESSAGE_TYPE_PROTOCOL_VERSION;
gwt_hm_ProtocolVersionMessage.send = function(channel, version) {
    channel.sendByte(gwt_hm_ProtocolVersionMessage.TYPE);
    channel.sendInt(version);
}
gwt_hm_ProtocolVersionMessage.receive = function(channel) {
    var version = channel.readInt();
    return new gwt_hm_ProtocolVersionMessage(version);
}





class gwt_hm_ByteOrder {
  
  //TODO-jscs- 
  floatFromBytes(bytes){
    throw "nope";
   return 0.0; 
  }
  doubleFromBytes(bytes){
    throw "nope";
    return 0.0; 
   }
  bytesFromDouble(double){
    throw "nope";
    return []; 
   }
  bytesFromFloat(float){
    throw "nope";
    return []; 
   }
}



class gwt_hm_ReturnMessage extends gwt_hm_Message {
    isException;
    retValue;
    constructor(isException, retValue) {
        super();
        this.isException = isException;
        this.retValue = retValue;
    }
}
gwt_hm_ReturnMessage.TYPE = gwt_hm_BrowserChannel.MESSAGE_TYPE_RETURN;
gwt_hm_ReturnMessage.receive = function(channel) {
    var isException = channel.readByte();
    var retValue = channel.readValue();
    return new gwt_hm_ReturnMessage(isException != 0, retValue);
}
gwt_hm_ReturnMessage.send = function(channel, isException, retValue) {
    channel.sendByte(gwt_hm_ReturnMessage.TYPE);
    channel.sendByte(isException ? 1 : 0);
    channel.sendValue(retValue);
}

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

class gwt_hm_Value {
    type;
    boolValue;
    byteValue;
    charValue;
    doubleValue;
    floatValue;
    intValue;
    longValue;
    shortValue;
    stringValue;
    constructor() {
        this.type = gwt_hm_BrowserChannel.VALUE_TYPE_UNDEFINED;
    }
    getBoolean() {
        this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN);
        return this.boolValue;
    }
    getByte() {
        this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_BYTE);
        return this.byteValue;
    }
    getChar() {
        this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_CHAR);
        return this.charValue;
    }
    getDouble() {
        this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE);
        return this.doubleValue;
    }
    getFloat() {
        this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT);
        return this.floatValue;
    }
    getInt() {
        this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_INT);
        return this.intValue;
    }
    getJavaObjectId() {
        this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_JAVA_OBJECT);
        return this.intValue;
    }
    getJsObjectId() {
        this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT);
        return this.intValue;
    }
    getLong() {
        this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_LONG);
        return this.longValue;
    }
    getShort() {
        this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_SHORT);
        return this.shortValue;
    }
    getString() {
        this.assertType(gwt_hm_BrowserChannel.VALUE_TYPE_STRING);
        return this.stringValue;
    }
    getType() {
        return type;
    }
    isBoolean() {
        return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN;
    }
    isByte() {
        return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_BYTE;
    }
    isChar() {
        return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_CHAR;
    }
    isDouble() {
        return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE;
    }
    isFloat() {
        return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT;
    }
    isInt() {
        return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_INT;
    }
    isJavaObject() {
        return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_JAVA_OBJECT;
    }
    isJsObject() {
        return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT;
    }
    isLong() {
        return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_LONG;
    }
    isNull() {
        return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_NULL;
    }
    isNumber() {
        switch (this.type) {
            case gwt_hm_BrowserChannel.VALUE_TYPE_BYTE:
            case gwt_hm_BrowserChannel.VALUE_TYPE_CHAR:
            case gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE:
            case gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT:
            case gwt_hm_BrowserChannel.VALUE_TYPE_INT:
            case gwt_hm_BrowserChannel.VALUE_TYPE_LONG:
            case gwt_hm_BrowserChannel.VALUE_TYPE_SHORT:
                return true;
            default:
                return false;
        }
    }
    isPrimitive() {
        switch (this.type) {
            case gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN:
            case gwt_hm_BrowserChannel.VALUE_TYPE_BYTE:
            case gwt_hm_BrowserChannel.VALUE_TYPE_CHAR:
            case gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE:
            case gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT:
            case gwt_hm_BrowserChannel.VALUE_TYPE_INT:
            case gwt_hm_BrowserChannel.VALUE_TYPE_LONG:
            case gwt_hm_BrowserChannel.VALUE_TYPE_SHORT:
                return true;
            default:
                return false;
        }
    }
    isShort() {
        return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_SHORT;
    }
    isString() {
        return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_STRING;
    }
    isUndefined() {
        return this.type == gwt_hm_BrowserChannel.VALUE_TYPE_UNDEFINED;
    }
    setBoolean(val) {
        this.type = gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN;
        this.boolValue = val;
    }
    setByte(val) {
        this.type = gwt_hm_BrowserChannel.VALUE_TYPE_BYTE;
        this.byteValue = val;
    }
    setChar(val) {
        this.type = gwt_hm_BrowserChannel.VALUE_TYPE_CHAR;
        this.charValue = val;
    }
    setDouble(val) {
        this.type = gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE;
        this.doubleValue = val;
    }
    setDouble(val) {
        this.type = gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE;
        this.doubleValue = val;
    }
    setFloat(val) {
        this.type = gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT;
        this.floatValue = val;
    }
    setInt(val) {
        this.type = gwt_hm_BrowserChannel.VALUE_TYPE_INT;
        this.intValue = val;
    }
    setJavaObjectId(objectId) {
        this.type = gwt_hm_BrowserChannel.VALUE_TYPE_JAVA_OBJECT;
        this.intValue = objectId;
    }
    setJsObjectId(val) {
        this.type = gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT;
        this.intValue = val;
    }
    setLong(val) {
        this.type = gwt_hm_BrowserChannel.VALUE_TYPE_LONG;
        this.longValue = val;
    }
    setNull() {
        this.type = gwt_hm_BrowserChannel.VALUE_TYPE_NULL;
    }
    setShort(val) {
        this.type = gwt_hm_BrowserChannel.VALUE_TYPE_SHORT;
        this.shortValue = val;
    }
    setString(val) {
        this.type = gwt_hm_BrowserChannel.VALUE_TYPE_STRING;
        this.stringValue = val;
    }
    setUndefined() {
        this.type = gwt_hm_BrowserChannel.VALUE_TYPE_UNDEFINED;
    }
    assertType(reqType) {
        if (this.type != reqType) {
            throw "Value::assertType - expecting type " + reqType;
        }
    }
    toString() {
        switch (this.type) {
            case gwt_hm_BrowserChannel.VALUE_TYPE_BOOLEAN:
                return `boolean: ${this.getBoolean()}`;
            case gwt_hm_BrowserChannel.VALUE_TYPE_BYTE:
                return `byte: ${this.getByte()}`;
            case gwt_hm_BrowserChannel.VALUE_TYPE_CHAR:
                return `char: ${this.getChar()}`;
            case gwt_hm_BrowserChannel.VALUE_TYPE_DOUBLE:
                return `double: ${this.getDouble()}`;
            case gwt_hm_BrowserChannel.VALUE_TYPE_FLOAT:
                return `float: ${this.getFloat()}`;
            case gwt_hm_BrowserChannel.VALUE_TYPE_INT:
                return `int: ${this.getInt()}`;
            case gwt_hm_BrowserChannel.VALUE_TYPE_LONG:
                return `long: ${this.getLong()}`;
            case gwt_hm_BrowserChannel.VALUE_TYPE_SHORT:
                return `short: ${this.getShort()}`;
            case gwt_hm_BrowserChannel.VALUE_TYPE_NULL:
                return "null";
            case gwt_hm_BrowserChannel.VALUE_TYPE_STRING:
                return `string: ${this.getString()}`;
            case gwt_hm_BrowserChannel.VALUE_TYPE_JAVA_OBJECT:
                return `javaobj: ${this.getJavaObjectId()}`;
            case gwt_hm_BrowserChannel.VALUE_TYPE_JS_OBJECT:
                return `jsobj: ${this.getJsObjectId()}`;
            case gwt_hm_BrowserChannel.VALUE_TYPE_UNDEFINED:
                return "undefined";
            default:
                return "Unknown type";
        }
    }
}

class gwt_hm_CheckVersionsMessage extends gwt_hm_Message {
    minVersion;
    maxVersion;
    hostedHtmlVersion;
    constructor(minVersion, maxVersion, hostedHtmlVersion) {
        super();
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.hostedHtmlVersion = hostedHtmlVersion;
    }
}
gwt_hm_CheckVersionsMessage.TYPE = gwt_hm_BrowserChannel.MESSAGE_TYPE_CHECK_VERSIONS;
gwt_hm_CheckVersionsMessage.receive = function(channel) {
    var minVersion = channel.readInt();
    var maxVersion = channel.readInt();
    var hostedHtmlVersion = channel.readString();
    return new gwt_hm_CheckVersionsMessage(minVersion, maxVersion, hostedHtmlVersion);
}
gwt_hm_CheckVersionsMessage.send = function(channel, minVersion, maxVersion,
    hostedHtmlVersion) {
    channel.sendByte(gwt_hm_CheckVersionsMessage.TYPE);
    channel.sendInt(minVersion);
    channel.sendInt(maxVersion);
    channel.sendString(hostedHtmlVersion);
    channel.flush();
}

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
$sendStats('moduleStartup', 'moduleEvalEnd');
gwtOnLoad(__gwtModuleFunction.__errFn, __gwtModuleFunction.__moduleName, __gwtModuleFunction.__moduleBase, __gwtModuleFunction.__softPermutationId,__gwtModuleFunction.__computePropValue);
$sendStats('moduleStartup', 'end');
$gwt && $gwt.permProps && __gwtModuleFunction.__moduleStartupDone($gwt.permProps);
//# sourceURL=cc.alcina.extras.dev.console.remote.RemoteConsoleClient-0.js

