package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.gwt.core.client.JavaScriptObject;

public final class JavascriptIntLookup extends JavaScriptObject {
	protected JavascriptIntLookup() {
	}

	private class ValuesIterator implements Iterator {
		JavascriptJavaObjectArray array;

		int idx = 0;

		private int itrModCount;

		public ValuesIterator() {
			array = values();
			this.itrModCount = modCount();
		}

		@Override
		public boolean hasNext() {
			return idx < array.length();
		}

		@Override
		public Object next() {
			if (idx == array.length()) {
				throw new NoSuchElementException();
			}
			if(itrModCount!=modCount()){
				throw new ConcurrentModificationException();
			}
			return array.get(idx++);
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	public Iterator valuesIterator() {
		return new ValuesIterator();
	}

	public static native JavascriptIntLookup create()/*-{
		var obj = {
			length : 0,
			modCount : 0,
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
			this.modCount++;
		}
		this.valueLookup[key] = value;
	}-*/;

	public native void remove(int key)/*-{
		if (this.valueLookup[key] === undefined) {

		} else {
			delete this.valueLookup[key];
			this.modCount--;
			this.length--;
		}
		;
	}-*/;

	public native int size()/*-{
		//should really be an assert here...
		return this.length >= 0 ? this.length : 0;
	}-*/;

	native int modCount()/*-{
		return this.modCount;
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