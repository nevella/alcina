package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import com.google.gwt.core.client.JavaScriptObject;

public final class JavascriptStringMap<T> extends JavaScriptObject {
	public static native JavascriptStringMap create()/*-{
        return {};
	}-*/;

	protected JavascriptStringMap() {
	}

	public final native String get(String key) /*-{
        return this[key];
	}-*/;

	public final native void put(String key, String value) /*-{
        this[key] = value;
	}-*/;
}