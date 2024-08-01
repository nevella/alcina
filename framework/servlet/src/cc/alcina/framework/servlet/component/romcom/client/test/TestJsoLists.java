package cc.alcina.framework.servlet.component.romcom.client.test;

import com.google.gwt.core.client.impl.JavaScriptObjectList;

import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.servlet.component.romcom.client.RemoteObjectModelComponentClient;

public class TestJsoLists {
	public void onModuleLoad() {
		JavaScriptObjectList list = new JavaScriptObjectList();
		populate(list);
		RemoteObjectModelComponentClient
				.consoleError(list.javaArray.length + "");
		RemoteObjectModelComponentClient.consoleError(
				list.jsArray == null ? "null" : list.jsArray.length() + "");
		Client.Init.init();
	}

	native void populate(JavaScriptObjectList list) /*-{
		var arr = [];
		arr.push({hello:"world"});
		arr.push({world:"hello"});
		list.__gwt_java_js_object_array=arr;
		//side effect of this assignment (in devmode) will be population of the JavascriptObjectList.javaArray 
		// (since list will be marshalled when calling back to the jdk)
		list.@com.google.gwt.core.client.impl.JavaScriptObjectList::jsArray = arr;
		return list;
	}-*/;
}
