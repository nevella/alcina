package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.gwt.core.client.JavaScriptObject;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.JavascriptKeyableLookup.EntryIterator;

public class JsNativeMap<K, V> extends JavaScriptObject {
	protected JsNativeMap() {
	}

	static native JsNativeMap createJsNativeMap()/*-{
        return new Map();
	}-*/;

	public final native void clear()/*-{
        this.clear();
	}-*/;

	public final boolean containsKey(Object key) {
		return this.get(key) != null;
		// throw new UnsupportedOperationException();
	}

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
		throw new UnsupportedOperationException();
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
        this.set(key, null);
        return v;
        //    return this.delete(key);
	}-*/;

	public final int size() {
		throw new UnsupportedOperationException();
	}

	public final Collection<V> values() {
		throw new UnsupportedOperationException();
	}
}