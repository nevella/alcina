package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayInteger;

public final class JavascriptIntLookup extends JavaScriptObject {
	protected JavascriptIntLookup() {
	}

	private class ValuesIterator implements Iterator {
		JsArrayInteger keysSnapshot;

		int idx = -1;

		private int itrModCount;

		private boolean nextCalled = false;

		public ValuesIterator() {
			keysSnapshot = keys();
			this.itrModCount = modCount();
		}

		@Override
		public boolean hasNext() {
			return idx + 1 < keysSnapshot.length();
		}

		@Override
		public Object next() {
			if (idx + 1 == keysSnapshot.length()) {
				throw new NoSuchElementException();
			}
			if (itrModCount != modCount()) {
				throw new ConcurrentModificationException();
			}
			nextCalled = true;
			int key = keysSnapshot.get(++idx);
			return JavascriptIntLookup.this.get(key);
		}

		@Override
		public void remove() {
			if (modCount() != itrModCount) {
				throw new ConcurrentModificationException();
			}
			if (!nextCalled) {
				throw new IllegalStateException();
			}
			if (JavascriptIntLookup.this.remove(keysSnapshot.get(idx))) {
				keysSnapshot = keys();
				this.itrModCount++;
				idx--;
				nextCalled = false;
			} else {
				throw new RuntimeException(
						"removing non-existent iterator value");
			}
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

	public native boolean remove(int key)/*-{
		if (this.valueLookup[key] === undefined) {
			return false;
		} else {
			delete this.valueLookup[key];
			this.modCount++;
			this.length--;
			return true;
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

	public native JsArrayInteger keys()/*-{
		var v = [];
		for ( var k in this.valueLookup) {
			if (this.valueLookup.hasOwnProperty(k)) {
				v.push(parseInt(k));
			}
		}
		return v;
	}-*/;
}