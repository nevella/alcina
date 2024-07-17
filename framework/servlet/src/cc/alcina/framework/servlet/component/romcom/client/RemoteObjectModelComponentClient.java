package cc.alcina.framework.servlet.component.romcom.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.GWT.UncaughtExceptionHandler;
import com.google.gwt.core.client.impl.JavaScriptObjectList;
import com.google.gwt.dom.client.ElementJso;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.servlet.component.romcom.client.common.logic.RemoteComponentInit;

/**
 * Thin gwt app which manipulates dom, posts events via send/receive on the the
 * rc-rpc protocol channel
 *
 * 
 *
 */
public class RemoteObjectModelComponentClient implements EntryPoint {
	static native void consoleError(String s) /*-{
    try {
      $wnd.console.error(s);
    } catch (e) {

    }
	}-*/;

	private void init0() {
		GWT.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
			@Override
			public void onUncaughtException(Throwable e) {
				e.printStackTrace();
				consoleError(CommonUtils.toSimpleExceptionMessage(e));
			}
		});
		new RemoteComponentInit().init();
	}

	@Override
	public void onModuleLoad() {
		JavaScriptObjectList list = new JavaScriptObjectList();
		populate(list);
		ElementJso e0 = list.javaArray[0].cast();
		ElementJso e1 = list.javaArray[1].cast();
		Client.Init.init();
	}

	native void populate(JavaScriptObjectList list) /*-{
		var arr = [];
		arr.push($doc.head);
		arr.push($doc.body);
		list.__gwt_java_js_object_array=arr;
		//side effect of this assignment (in devmode) will be population of the JavascriptObjectList.javaArray 
		// (since list will be marshalled when calling back to the jdk)
		list.@com.google.gwt.core.client.impl.JavaScriptObjectList::jsArray = arr;
		return list;
	}-*/;
}
