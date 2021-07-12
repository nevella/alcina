package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

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

	public final boolean containsValue(Object value) {
		throw new UnsupportedOperationException();
	}

	public final Set<Map.Entry<K, V>> entrySet() {
		throw new UnsupportedOperationException();
	}

	public final native V get(Object key) /*-{
    return this.get(key);
	}-*/;

	public final boolean isEmpty() {
		return size() == 0;
	}

	public final Set<K> keySet() {
		throw new UnsupportedOperationException();
	}

	public final native V put(K key, V value) /*-{
    this.set(key, value);
    return null;
	}-*/;

	public final void putAll(Map<? extends K, ? extends V> m) {
		throw new UnsupportedOperationException();
	}

	public final native V remove(Object key) /*-{
    var v = this.get(key);
    //gwt js validator doesn't like this.delete(key)
    this["delete"](key);
    return v;
	}-*/;

	public final native int size() /*-{
    return this.size;
	}-*/;

	public final Collection<V> values() {
		throw new UnsupportedOperationException();
	}
}