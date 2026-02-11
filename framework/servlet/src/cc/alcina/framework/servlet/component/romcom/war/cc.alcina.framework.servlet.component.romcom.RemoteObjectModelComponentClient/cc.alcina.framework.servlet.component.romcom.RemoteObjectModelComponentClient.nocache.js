<<<<<<< HEAD
function cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient(){
  var $wnd_0 = window;
  var $doc_0 = document;
  sendStats('bootstrap', 'begin');
  function isHostedMode(){
    var query = $wnd_0.location.search;
    return query.indexOf('gwt.codesvr.cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient=') != -1 || (query.indexOf('gwt.codesvr=') != -1 || query.indexOf('gwt.l') != -1);
  }

  function sendStats(evtGroupString, typeString){
    if ($wnd_0.__gwtStatsEvent) {
      $wnd_0.__gwtStatsEvent({'moduleName':'cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient', 'sessionId':$wnd_0.__gwtStatsSessionId, 'subSystem':'startup', 'evtGroup':evtGroupString, 'millis':(new Date).getTime(), 'type':typeString});
    }
  }

  cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__sendStats = sendStats;
  cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__moduleName = 'cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient';
  cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__errFn = null;
  cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__moduleBase = 'DUMMY';
  cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__softPermutationId = 0;
  cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__computePropValue = null;
  cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__getPropMap = null;
  cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__installRunAsyncCode = function(){
  }
  ;
  cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__gwtStartLoadingFragment = function(){
    return null;
  }
  ;
  cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__gwt_isKnownPropertyValue = function(){
    return false;
  }
  ;
  cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__gwt_getMetaProperty = function(){
    return null;
  }
  ;
  var __propertyErrorFunction = null;
  var activeModules = $wnd_0.__gwt_activeModules = $wnd_0.__gwt_activeModules || {};
  activeModules['cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient'] = {'moduleName':'cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient'};
  cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__moduleStartupDone = function(permProps){
    var oldBindings = activeModules['cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient'].bindings;
    activeModules['cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient'].bindings = function(){
      var props = oldBindings?oldBindings():{};
      var embeddedProps = permProps[cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__softPermutationId];
      for (var i = 0; i < embeddedProps.length; i++) {
        var pair = embeddedProps[i];
        props[pair[0]] = pair[1];
      }
      return props;
    }
    ;
  }
  ;
  var frameDoc;
  function getInstallLocationDoc(){
    setupInstallLocation();
    return frameDoc;
  }

  function setupInstallLocation(){
    if (frameDoc) {
      return;
    }
    var scriptFrame = $doc_0.createElement('iframe');
    scriptFrame.id = 'cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient';
    scriptFrame.title = 'Javascript container. No human-readable content';
    scriptFrame.style.cssText = 'position:absolute; width:0; height:0; border:none; left: -1000px;' + ' top: -1000px;';
    scriptFrame.tabIndex = -1;
    $doc_0.body.appendChild(scriptFrame);
    frameDoc = scriptFrame.contentDocument;
    if (!frameDoc) {
      frameDoc = scriptFrame.contentWindow.document;
    }
    frameDoc.open();
    var doctype = document.compatMode == 'CSS1Compat'?'<!doctype html>':'';
    frameDoc.write(doctype + '<html><head><\/head><body><\/body><\/html>');
    frameDoc.close();
  }

  function installScript(filename){
    function setupWaitForBodyLoad(callback){
      function isBodyLoaded(){
        if (typeof $doc_0.readyState == 'undefined') {
          return typeof $doc_0.body != 'undefined' && $doc_0.body != null;
        }
        return /loaded|complete/.test($doc_0.readyState);
      }

      var bodyDone = isBodyLoaded();
      if (bodyDone) {
        callback();
        return;
      }
      function checkBodyDone(){
        if (!bodyDone) {
          if (!isBodyLoaded()) {
            return;
          }
          bodyDone = true;
          callback();
          if ($doc_0.removeEventListener) {
            $doc_0.removeEventListener('readystatechange', checkBodyDone, false);
          }
          if (onBodyDoneTimerId) {
            clearInterval(onBodyDoneTimerId);
          }
        }
      }

      if ($doc_0.addEventListener) {
        $doc_0.addEventListener('readystatechange', checkBodyDone, false);
      }
      var onBodyDoneTimerId = setInterval(function(){
        checkBodyDone();
      }
      , 10);
    }

    function installCode(code_0){
      var doc = getInstallLocationDoc();
      var docbody = doc.body;
      var script = doc.createElement('script');
      script.language = 'javascript';
      script.src = code_0;
      if (cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__errFn) {
        script.onerror = function(){
          cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__errFn('cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient', new Error('Failed to load ' + code_0));
        }
        ;
      }
      docbody.appendChild(script);
      sendStats('moduleStartup', 'scriptTagAdded');
    }

    sendStats('moduleStartup', 'moduleRequested');
    setupWaitForBodyLoad(function(){
      installCode(filename);
    }
    );
  }

  cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__startLoadingFragment = function(fragmentFile){
    return computeUrlForResource(fragmentFile);
  }
  ;
  cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__installRunAsyncCode = function(code_0){
    var doc = getInstallLocationDoc();
    var docbody = doc.body;
    var script = doc.createElement('script');
    script.language = 'javascript';
    script.text = code_0;
    docbody.appendChild(script);
  }
  ;
  function processMetas(){
    var metaProps = {};
    var propertyErrorFunc;
    var onLoadErrorFunc;
    var metas = $doc_0.getElementsByTagName('meta');
    for (var i = 0, n = metas.length; i < n; ++i) {
      var meta = metas[i], name_1 = meta.getAttribute('name'), content_0;
      if (name_1) {
        name_1 = name_1.replace('cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient::', '');
        if (name_1.indexOf('::') >= 0) {
          continue;
        }
        if (name_1 == 'gwt:property') {
          content_0 = meta.getAttribute('content');
          if (content_0) {
            var value_1, eq = content_0.indexOf('=');
            if (eq >= 0) {
              name_1 = content_0.substring(0, eq);
              value_1 = content_0.substring(eq + 1);
            }
             else {
              name_1 = content_0;
              value_1 = '';
            }
            metaProps[name_1] = value_1;
          }
        }
         else if (name_1 == 'gwt:onPropertyErrorFn') {
          content_0 = meta.getAttribute('content');
          if (content_0) {
            try {
              propertyErrorFunc = eval(content_0);
            }
             catch (e) {
              alert('Bad handler "' + content_0 + '" for "gwt:onPropertyErrorFn"');
            }
          }
        }
         else if (name_1 == 'gwt:onLoadErrorFn') {
          content_0 = meta.getAttribute('content');
          if (content_0) {
            try {
              onLoadErrorFunc = eval(content_0);
            }
             catch (e) {
              alert('Bad handler "' + content_0 + '" for "gwt:onLoadErrorFn"');
            }
          }
        }
      }
    }
    __gwt_getMetaProperty = function(name_0){
      var value_0 = metaProps[name_0];
      return value_0 == null?null:value_0;
    }
    ;
    __propertyErrorFunction = propertyErrorFunc;
    cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__errFn = onLoadErrorFunc;
  }

  function computeScriptBase(){
    function getDirectoryOfFile(path){
      var hashIndex = path.lastIndexOf('#');
      if (hashIndex == -1) {
        hashIndex = path.length;
      }
      var queryIndex = path.indexOf('?');
      if (queryIndex == -1) {
        queryIndex = path.length;
      }
      var slashIndex = path.lastIndexOf('/', Math.min(queryIndex, hashIndex));
      return slashIndex >= 0?path.substring(0, slashIndex + 1):'';
    }

    function ensureAbsoluteUrl(url_0){
      if (url_0.match(/^\w+:\/\//)) {
      }
       else {
        var img = $doc_0.createElement('img');
        img.src = url_0 + 'clear.cache.gif';
        url_0 = getDirectoryOfFile(img.src);
      }
      return url_0;
    }

    function tryMetaTag(){
      var metaVal = __gwt_getMetaProperty('baseUrl');
      if (metaVal != null) {
        return metaVal;
      }
      return '';
    }

    function tryNocacheJsTag(){
      var scriptTags = $doc_0.getElementsByTagName('script');
      for (var i = 0; i < scriptTags.length; ++i) {
        if (scriptTags[i].src.indexOf('cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient.nocache.js') != -1) {
          return getDirectoryOfFile(scriptTags[i].src);
        }
      }
      return '';
    }

    function tryBaseTag(){
      var baseElements = $doc_0.getElementsByTagName('base');
      if (baseElements.length > 0) {
        return baseElements[baseElements.length - 1].href;
      }
      return '';
    }

    function isLocationOk(){
      var loc = $doc_0.location;
      return loc.href == loc.protocol + '//' + loc.host + loc.pathname + loc.search + loc.hash;
    }

    var tempBase = tryMetaTag();
    if (tempBase == '') {
      tempBase = tryNocacheJsTag();
    }
    if (tempBase == '') {
      tempBase = tryBaseTag();
    }
    if (tempBase == '' && isLocationOk()) {
      tempBase = getDirectoryOfFile($doc_0.location.href);
    }
    tempBase = ensureAbsoluteUrl(tempBase);
    return tempBase;
  }

  function computeUrlForResource(resource){
    if (resource.match(/^\//)) {
      return resource;
    }
    if (resource.match(/^[a-zA-Z]+:\/\//)) {
      return resource;
    }
    return cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__moduleBase + resource;
  }

  function getCompiledCodeFilename(){
    var answers = [];
    var softPermutationId = 0;
    var values = [];
    var providers = [];
    function computePropValue(propName){
      var value_0 = providers[propName](), allowedValuesMap = values[propName];
      if (value_0 in allowedValuesMap) {
        return value_0;
      }
      var allowedValuesList = [];
      for (var k in allowedValuesMap) {
        allowedValuesList[allowedValuesMap[k]] = k;
      }
      if (__propertyErrorFunction) {
        __propertyErrorFunction(propName, allowedValuesList, value_0);
      }
      throw null;
    }

    __gwt_isKnownPropertyValue = function(propName, propValue){
      return propValue in values[propName];
    }
    ;
    cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__getPropMap = function(){
      var result = {};
      for (var key in values) {
        if (values.hasOwnProperty(key)) {
          result[key] = computePropValue(key);
        }
      }
      return result;
    }
    ;
    cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__computePropValue = computePropValue;
    $wnd_0.__gwt_activeModules['cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient'].bindings = cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__getPropMap;
    sendStats('bootstrap', 'selectingPermutation');
    if (isHostedMode()) {
      return computeUrlForResource('cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient.devmode.js');
    }
    var strongName;
    try {
      strongName = 'EE9E53E110B51AA3CAFBB6D1B0FBE27F';
      var idx = strongName.indexOf(':');
      if (idx != -1) {
        softPermutationId = parseInt(strongName.substring(idx + 1), 10);
        strongName = strongName.substring(0, idx);
      }
    }
     catch (e) {
    }
    cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__softPermutationId = softPermutationId;
    return computeUrlForResource(strongName + '.cache.js');
  }

  function loadExternalStylesheets(){
    if (!$wnd_0.__gwt_stylesLoaded) {
      $wnd_0.__gwt_stylesLoaded = {};
    }
    sendStats('loadExternalRefs', 'begin');
    sendStats('loadExternalRefs', 'end');
  }

  processMetas();
  cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__moduleBase = computeScriptBase();
  activeModules['cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient'].moduleBase = cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__moduleBase;
  var filename_0 = getCompiledCodeFilename();
  if ($wnd_0) {
    var devModePermitted = !!($wnd_0.location.protocol == 'http:' || $wnd_0.location.protocol == 'file:');
    $wnd_0.__gwt_activeModules['cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient'].canRedirect = devModePermitted;
    function supportsSessionStorage(){
      var key = '_gwt_dummy_';
      try {
        $wnd_0.sessionStorage.setItem(key, key);
        $wnd_0.sessionStorage.removeItem(key);
        return true;
      }
       catch (e) {
        return false;
      }
    }

    if (devModePermitted && supportsSessionStorage()) {
      var devModeKey = '__gwtDevModeHook:cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient';
      var devModeUrl = $wnd_0.sessionStorage[devModeKey];
      if (!/^http:\/\/(localhost|127\.0\.0\.1)(:\d+)?\/.*$/.test(devModeUrl)) {
        if (devModeUrl && (window.console && console.log)) {
          console.log('Ignoring non-whitelisted Dev Mode URL: ' + devModeUrl);
        }
        devModeUrl = '';
      }
      if (devModeUrl && !$wnd_0[devModeKey]) {
        $wnd_0[devModeKey] = true;
        $wnd_0[devModeKey + ':moduleBase'] = computeScriptBase();
        var devModeScript = $doc_0.createElement('script');
        devModeScript.src = devModeUrl;
        var head = $doc_0.getElementsByTagName('head')[0];
        head.insertBefore(devModeScript, head.firstElementChild || head.children[0]);
        return false;
      }
    }
  }
  loadExternalStylesheets();
  sendStats('bootstrap', 'end');
  installScript(filename_0);
  return true;
}

cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.succeeded = cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient();
=======
function cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient(){var N='bootstrap',O='begin',P='gwt.codesvr.cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient=',Q='gwt.codesvr=',R='gwt.l',S='cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient',T='startup',U='DUMMY',V=0,W=1,X='iframe',Y='Javascript container. No human-readable content',Z='position:absolute; width:0; height:0; border:none; left: -1000px;',$=' top: -1000px;',_='CSS1Compat',ab='<!doctype html>',bb='',cb='<html><head><\/head><body><\/body><\/html>',db='undefined',eb='readystatechange',fb=10,gb='script',hb='javascript',ib='cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient',jb='Failed to load ',kb='moduleStartup',lb='scriptTagAdded',mb='moduleRequested',nb='meta',ob='name',pb='cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient::',qb='::',rb='gwt:property',sb='content',tb='=',ub='gwt:onPropertyErrorFn',vb='Bad handler "',wb='" for "gwt:onPropertyErrorFn"',xb='gwt:onLoadErrorFn',yb='" for "gwt:onLoadErrorFn"',zb='#',Ab='?',Bb='/',Cb='img',Db='clear.cache.gif',Eb='baseUrl',Fb='cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient.nocache.js',Gb='base',Hb='//',Ib='selectingPermutation',Jb='cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient.devmode.js',Kb='1EDD0022BA7F1A6FEA06F51A873503D4',Lb=':',Mb='.cache.js',Nb='loadExternalRefs',Ob='end',Pb='http:',Qb='file:',Rb='_gwt_dummy_',Sb='__gwtDevModeHook:cc.alcina.framework.servlet.component.romcom.RemoteObjectModelComponentClient',Tb='Ignoring non-whitelisted Dev Mode URL: ',Ub=':moduleBase',Vb='head';var n=window;var o=document;q(N,O);function p(){var a=n.location.search;return a.indexOf(P)!=-1||(a.indexOf(Q)!=-1||a.indexOf(R)!=-1)}
function q(a,b){if(n.__gwtStatsEvent){n.__gwtStatsEvent({'moduleName':S,'sessionId':n.__gwtStatsSessionId,'subSystem':T,'evtGroup':a,'millis':(new Date).getTime(),'type':b})}}
cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__sendStats=q;cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__moduleName=S;cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__errFn=null;cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__moduleBase=U;cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__softPermutationId=V;cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__computePropValue=null;cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__getPropMap=null;cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__installRunAsyncCode=function(){};cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__gwtStartLoadingFragment=function(){return null};cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__gwt_isKnownPropertyValue=function(){return false};cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__gwt_getMetaProperty=function(){return null};var r=null;var s=n.__gwt_activeModules=n.__gwt_activeModules||{};s[S]={'moduleName':S};cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__moduleStartupDone=function(e){var f=s[S].bindings;s[S].bindings=function(){var a=f?f():{};var b=e[cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__softPermutationId];for(var c=V;c<b.length;c++){var d=b[c];a[d[V]]=d[W]}return a}};var t;function u(){v();return t}
function v(){if(t){return}var a=o.createElement(X);a.id=S;a.title=Y;a.style.cssText=Z+$;a.tabIndex=-1;o.body.appendChild(a);t=a.contentDocument;if(!t){t=a.contentWindow.document}t.open();var b=document.compatMode==_?ab:bb;t.write(b+cb);t.close()}
function w(f){function g(a){function b(){if(typeof o.readyState==db){return typeof o.body!=db&&o.body!=null}return /loaded|complete/.test(o.readyState)}
var c=b();if(c){a();return}function d(){if(!c){if(!b()){return}c=true;a();if(o.removeEventListener){o.removeEventListener(eb,d,false)}if(e){clearInterval(e)}}}
if(o.addEventListener){o.addEventListener(eb,d,false)}var e=setInterval(function(){d()},fb)}
function h(a){var b=u();var c=b.body;var d=b.createElement(gb);d.language=hb;d.src=a;if(cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__errFn){d.onerror=function(){cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__errFn(ib,new Error(jb+a))}}c.appendChild(d);q(kb,lb)}
q(kb,mb);g(function(){h(f)})}
cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__startLoadingFragment=function(a){return C(a)};cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__installRunAsyncCode=function(a){var b=u();var c=b.body;var d=b.createElement(gb);d.language=hb;d.text=a;c.appendChild(d);c.removeChild(d)};function A(){var c={};var d;var e;var f=o.getElementsByTagName(nb);for(var g=V,h=f.length;g<h;++g){var i=f[g],j=i.getAttribute(ob),k;if(j){j=j.replace(pb,bb);if(j.indexOf(qb)>=V){continue}if(j==rb){k=i.getAttribute(sb);if(k){var l,m=k.indexOf(tb);if(m>=V){j=k.substring(V,m);l=k.substring(m+W)}else{j=k;l=bb}c[j]=l}}else if(j==ub){k=i.getAttribute(sb);if(k){try{d=eval(k)}catch(a){alert(vb+k+wb)}}}else if(j==xb){k=i.getAttribute(sb);if(k){try{e=eval(k)}catch(a){alert(vb+k+yb)}}}}}__gwt_getMetaProperty=function(a){var b=c[a];return b==null?null:b};r=d;cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__errFn=e}
function B(){function e(a){var b=a.lastIndexOf(zb);if(b==-1){b=a.length}var c=a.indexOf(Ab);if(c==-1){c=a.length}var d=a.lastIndexOf(Bb,Math.min(c,b));return d>=V?a.substring(V,d+W):bb}
function f(a){if(a.match(/^\w+:\/\//)){}else{var b=o.createElement(Cb);b.src=a+Db;a=e(b.src)}return a}
function g(){var a=__gwt_getMetaProperty(Eb);if(a!=null){return a}return bb}
function h(){var a=o.getElementsByTagName(gb);for(var b=V;b<a.length;++b){if(a[b].src.indexOf(Fb)!=-1){return e(a[b].src)}}return bb}
function i(){var a=o.getElementsByTagName(Gb);if(a.length>V){return a[a.length-W].href}return bb}
function j(){var a=o.location;return a.href==a.protocol+Hb+a.host+a.pathname+a.search+a.hash}
var k=g();if(k==bb){k=h()}if(k==bb){k=i()}if(k==bb&&j()){k=e(o.location.href)}k=f(k);return k}
function C(a){if(a.match(/^\//)){return a}if(a.match(/^[a-zA-Z]+:\/\//)){return a}return cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__moduleBase+a}
function D(){var f=[];var g=V;var h=[];var i=[];function j(a){var b=i[a](),c=h[a];if(b in c){return b}var d=[];for(var e in c){d[c[e]]=e}if(r){r(a,d,b)}throw null}
__gwt_isKnownPropertyValue=function(a,b){return b in h[a]};cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__getPropMap=function(){var a={};for(var b in h){if(h.hasOwnProperty(b)){a[b]=j(b)}}return a};cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__computePropValue=j;n.__gwt_activeModules[S].bindings=cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__getPropMap;q(N,Ib);if(p()){return C(Jb)}var k;try{k=Kb;var l=k.indexOf(Lb);if(l!=-1){g=parseInt(k.substring(l+W),fb);k=k.substring(V,l)}}catch(a){}cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__softPermutationId=g;return C(k+Mb)}
function F(){if(!n.__gwt_stylesLoaded){n.__gwt_stylesLoaded={}}q(Nb,O);q(Nb,Ob)}
A();cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__moduleBase=B();s[S].moduleBase=cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.__moduleBase;var G=D();if(n){var H=!!(n.location.protocol==Pb||n.location.protocol==Qb);n.__gwt_activeModules[S].canRedirect=H;function I(){var b=Rb;try{n.sessionStorage.setItem(b,b);n.sessionStorage.removeItem(b);return true}catch(a){return false}}
if(H&&I()){var J=Sb;var K=n.sessionStorage[J];if(!/^http:\/\/(localhost|127\.0\.0\.1)(:\d+)?\/.*$/.test(K)){if(K&&(window.console&&console.log)){console.log(Tb+K)}K=bb}if(K&&!n[J]){n[J]=true;n[J+Ub]=B();var L=o.createElement(gb);L.src=K;var M=o.getElementsByTagName(Vb)[V];M.insertBefore(L,M.firstElementChild||M.children[V]);return false}}}F();q(N,Ob);w(G);return true}
cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient.succeeded=cc_alcina_framework_servlet_component_romcom_RemoteObjectModelComponentClient();
>>>>>>> dev
