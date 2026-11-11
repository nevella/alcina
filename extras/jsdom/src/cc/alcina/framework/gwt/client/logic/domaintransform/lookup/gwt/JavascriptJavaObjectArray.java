package cc.alcina.framework.gwt.client.logic.domaintransform.lookup.gwt;

import com.google.gwt.core.client.JavaScriptObject;

public final class JavascriptJavaObjectArray<T> extends JavaScriptObject {
	protected JavascriptJavaObjectArray() {
	}

	public final native T get(int index) /*-{
											return this[index];
											}-*/;

	public final native int getInt(int index) /*-{
												return this[index];
												}-*/;

	public final native int length() /*-{
										return this.length;
										}-*/;
}