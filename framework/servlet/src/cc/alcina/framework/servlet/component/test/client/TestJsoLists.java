package cc.alcina.framework.servlet.component.test.client;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.impl.JavaScriptObjectList;

import cc.alcina.framework.gwt.client.util.ClientUtils;

class TestJsoLists {
	void run() {
		JavaScriptObjectList list = new JavaScriptObjectList();
		populate(list);
		Preconditions.checkState(list.javaArray.length == 2);
		Preconditions.checkState(list.jsArray.length() == 2);
		ClientUtils.consoleInfo("java array/js array length: %s",
				list.javaArray.length);
		ClientUtils.consoleInfo("   [TestJsoLists] Passed");
	}

	native Object populate(JavaScriptObjectList list) /*-{
		var arr = [];
		arr.push({hello:"world"});
		arr.push({world:"hello"});
		list.__gwt_java_js_object_list=arr;
		//side effect of this assignment (in devmode) will be population of the JavascriptObjectList.javaArray 
		// (since list will be marshalled when calling back to the jdk)
		list.@com.google.gwt.core.client.impl.JavaScriptObjectList::jsArray = arr;
		return list;
	}-*/;
}
