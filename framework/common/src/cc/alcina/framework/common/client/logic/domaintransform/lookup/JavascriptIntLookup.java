package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import com.google.gwt.core.client.JavaScriptObject;

public final class JavascriptIntLookup extends JavaScriptObject {
	protected JavascriptIntLookup() {
	}

	public static native JavascriptIntLookup create()/*-{
		var obj = {
			length : 0,
			valueLookup : {}
		};
		return obj;
	}-*/;

	public native <V> V get(int key)/*-{
		return this.valueLookup[key];
	}-*/;

	public native void put(int key, Object value)/*-{
		if (this.valueLookup[key] === undefined) {
			this.length++;
		}
		this.valueLookup[key] = value;
	}-*/;

	public native void remove(int key)/*-{
		if (this.valueLookup[key] === undefined) {

		} else {
			delete this.valueLookup[key];
			this.length--;
		}
		;
	}-*/;

	public native int size()/*-{
		//should really be an assert here...
		return this.length >= 0 ? this.length : 0;
	}-*/;

	public native JavascriptJavaObjectArray values()/*-{
		var v = [];
		for ( var k in this.valueLookup) {
			if (this.valueLookup.hasOwnProperty(k)) {
				v.push(this.valueLookup[k]);
			}
		}
		return v;
	}-*/;
}