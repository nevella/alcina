# Additions to the GWT devmode protocol

### To test -

```
@Override
	public void onModuleLoad() {
		JavascriptObjectList list = new JavascriptObjectList();
		populate(list);
		Ax.out(list.javaArray.length);
	}

	native JavascriptObjectList populate(JavascriptObjectList list) /*-{
		var arr = [];
		arr.push({hello:"world"});
		arr.push({world:"hello"});
		list@com.google.gwt.core.client.impl.JavascriptObjectList::jsArray = arr;
		return list;
	}-*/;
```

### See

/g/alcina/framework/jscodeserver/src/cc/alcina/framework/jscodeserver/jscodeserver-readme.md

### Notes on the dev mode protocol dispatch

When a js method is called from javascript:

- The rewritten class (it's compiled bytecode with 'native' methods converted to calls to JavaScriptHost) calls
  JavaScriptHost.invokeNativeObject
- Through to ModuleSpaceOOPHM.doInvoke
- This marshalls the method args as an array of JsValueOOPHM
- The marshalling calls through to JsValueOOPHM.setWrappedJavaObject(cl, obj);
- The value set on JsValueOOPHM is an instanceof DispatchObjectOOPHM

### OOPHM debugging

com.google.gwt.dev.shell.BrowserChannel.SessionHandler.ExceptionOrReturnValue.ExceptionOrReturnValue(boolean isException, Value returnValue)

where isException = true

### WS dispatch

Server->Client(Browser)

- BrowserChannel.writeJavaObject -> (writes to a DataOutputStream)
- JsCodeserverTcpClientJava.receiveMessageBytes => receiveMessage (it reads/decodes the message from the server side of the ws)
  ( this is probably because we cos don't know how many bytes up front - so decoding the message is the easiest way)
- JsCodeserverTcpClientJava.receiveMessageBytes => message.send() (send to the browser side of the ws)
