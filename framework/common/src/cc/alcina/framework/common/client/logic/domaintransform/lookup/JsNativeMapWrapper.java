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

public class JsNativeMapWrapper<K, V> implements Map<K, V> {
	private JsNativeMap<K,V> map;
	private boolean weak;

	public final void clear() {
		this.map.clear();
	}

	public final boolean containsKey(Object key) {
		return this.map.containsKey(key);
	}

	public final boolean containsValue(Object value) {
		return this.map.containsValue(value);
	}

	public final Set<java.util.Map.Entry<K, V>> entrySet() {
		if(weak){
			throw new UnsupportedOperationException();
		}
		return this.map.entrySet();
	}

	public final V get(Object key) {
		return this.map.get(key);
	}

	public final boolean isEmpty() {
		return this.map.isEmpty();
	}

	public final Set<K> keySet() {
		return this.map.keySet();
	}

	public final V put(K key, V value) {
		return this.map.put(key, value);
	}

	public final void putAll(Map<? extends K, ? extends V> m) {
		this.map.putAll(m);
	}

	public final V remove(Object key) {
		return this.map.remove(key);
	}

	public final int size() {
		return this.map.size();
	}

	public final Collection<V> values() {
		return this.map.values();
	}

	JsNativeMapWrapper(boolean weak) {
		this.weak = weak;
		map = JsNativeMap.createJsNativeMap(weak);
	}

}