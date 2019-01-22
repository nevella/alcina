// Installs the script directly, by simply appending a script tag with the
// src set to the correct location to the install location.
function installScript(filename) {
  // Provides the setupWaitForBodyLoad()function
  __WAIT_FOR_BODY_LOADED__
  
  function installCode(code) {
    var doc = getInstallLocationDoc();
    var docbody = doc.body;
    var script = doc.createElement('script');
    script.language='javascript';
    script.src = code;
    if (__MODULE_FUNC__.__errFn) {
      script.onerror = function() {
        __MODULE_FUNC__.__errFn('__MODULE_FUNC__', new Error("Failed to load " + code));
      }
    }
    var __win = doc.parentWindow || doc.defaultView;
    var __frame = __win.frameElement;
    var resetOnce = false;
    var checkFrameNotReset = function(){
      if(resetOnce){
        return;
      }
      var __win_ = __win;
      var __frame_ = __frame;
      var __script = script;
      if (!__script||(__script.ownerDocument != __frame_.contentDocument)){
        //frame is wiped and script gc-d (in chromium 3673)
        console.log("patch Chromium 73.0.3673.0");
        var script_ = __frame_.contentDocument.createElement('script');
        script_.language='javascript';
        script_.src = code;
        __frame_.contentDocument.body.appendChild(script_);
        resetOnce=true;
      }
      
    };
    setTimeout(checkFrameNotReset,200);
    setTimeout(checkFrameNotReset,500);

    docbody.appendChild(script);
    sendStats('moduleStartup', 'scriptTagAdded');
  }

  // Start measuring from the time the caller asked for this file,
  // for consistency with installScriptEarlyDownload.js.
  // The elapsed time will include waiting for the body.
  sendStats('moduleStartup', 'moduleRequested');

  // Just pass along the filename so that a script tag can be installed in the
  // iframe to download it.  Since we will be adding the iframe to the body,
  // we still need to wait for the body to load before going forward.
  setupWaitForBodyLoad(function() {
    installCode(filename);
  });
}
