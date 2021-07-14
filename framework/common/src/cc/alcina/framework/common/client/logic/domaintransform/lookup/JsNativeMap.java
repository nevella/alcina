package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import com.google.gwt.core.client.JavaScriptObject;

public class JsNativeMap<K, V> extends JavaScriptObject {
	static native JsNativeMap createJsNativeMap(boolean weak)/*-{
    return weak ? new WeakMap() : new Map();
	}-*/;

	protected JsNativeMap() {
	}

	public final native void clear()/*-{
    this.clear();
	}-*/;

	public final native boolean containsKey(Object key) /*-{
    return this.has(key);
	}-*/;

	public final native V get(Object key) /*-{
    return this.get(key);
	}-*/;

	public final boolean isEmpty() {
		return size() == 0;
	}

	public final native JavascriptJavaObjectArray keys()/*-{
    var map = this;
    var v = [];
    var itr = map.keys();
    result = itr.next();
    while (!result.done) {
      v.push(result.value);
      result = itr.next();
    }
    return v;
	}-*/;

	public final native V put(K key, V value) /*-{
    this.set(key, value);
    return null;
	}-*/;

	public final native V remove(Object key) /*-{
    var v = this.get(key);
    //gwt js validator doesn't like this.delete(key)
    this["delete"](key);
    return v;
	}-*/;

	public final native int size() /*-{
    return this.size;
	}-*/;
}