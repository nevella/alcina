package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.gwt.core.client.JavaScriptObject;

public final class JavascriptKeyableLookup extends JavaScriptObject {
	public static native JavascriptKeyableLookup create(boolean intLookup)/*-{
        return new $wnd.AlcJsKeyableMap(intLookup);
	}-*/;

	public static native void initJs()/*-{
		if($wnd.AlcJsKeyableMap){
			return;
		}
        function AlcJsKeyableMap(intLookup) {
            this.length = 0;
            this.modCount = 0;
            this.values = {};
            this.keys = {};
            this.intLookup = intLookup;

        }
        AlcJsKeyableMap.prototype.get = function(key) {
            return this.values[key];
        }
        AlcJsKeyableMap.prototype.put = function(key, value) {
            var old = null;
            if (this.values[key] === undefined) {
                this.length++;
                this.modCount++;
            } else {
                old = this.values[key];
            }
            this.values[key] = value;
            this.keys[key] = key;
            return old;
        }
        AlcJsKeyableMap.prototype.remove = function(key) {
            if (this.values[key] === undefined) {
                return null;
            } else {
                var old = this.values[key];
                delete this.values[key];
                delete this.keys[key];
                this.modCount++;
                this.length--;
                return old;
            }
        }
        AlcJsKeyableMap.prototype.containsKey = function(key) {
            if (this.values[key] === undefined) {
                return false;
            } else {
                return true;
            }
        }
        AlcJsKeyableMap.prototype.clear = function(key) {
            this.values = {};
            this.keys = {};
            this.length = 0;
            this.modCount++;
        }
        $wnd.AlcJsKeyableMap = AlcJsKeyableMap;

	}-*/;

	protected JavascriptKeyableLookup() {
	}

	public native void clear() /*-{
        this.clear();
	}-*/;

	public boolean containsKey(Object key) {
		return intLookup() ? containsKey0(key2int(key)) : containsKey0(key);
	}

	public Iterator entryIterator() {
		return new EntryIterator();
	}

	public <V> V get(Object key) {
		return intLookup() ? get0(key2int(key)) : get0(key);
	}

	public native JavascriptJavaObjectArray keys()/*-{
        var v = [];
        for ( var k in this.keys) {
            if (this.keys.hasOwnProperty(k)) {
                var key = this.keys[k];
                if (this.intLookup && typeof (key) != "Number") {
                    v.push(parseInt(key));
                } else {
                    v.push(key);
                }
            }
        }
        return v;
	}-*/;

	public Object put(Object key, Object value) {
		return intLookup() ? put0(key2int(key), value) : put0(key, value);
	}

	public Object remove(Object key) {
		return intLookup() ? remove0(key2int(key)) : remove0(key);
	}

	public native int size()/*-{
        //should really be an assert here...
        return this.length >= 0 ? this.length : 0;
	}-*/;

	private native boolean containsKey0(int key)/*-{
        return this.containsKey(key);
	}-*/;

	private native boolean containsKey0(Object key)/*-{
        return this.containsKey(key);
	}-*/;

	private native <V> V get0(int key)/*-{
        return this.get(key);
	}-*/;

	private native <V> V get0(Object key)/*-{
        return this.get(key);
	}-*/;

	private native boolean intLookup()/*-{
        return this.intLookup;
	}-*/;

	private int key2int(Object key) {
		return ((Integer) key).intValue();
	}

	private native Object put0(int key, Object value)/*-{
        return this.put(key, value);
	}-*/;

	private native Object put0(Object key, Object value)/*-{
        return this.put(key, value);
	}-*/;

	private native Object remove0(int key)/*-{
        return this.remove(key);
	}-*/;

	private native Object remove0(Object key)/*-{
        return this.remove(key);
	}-*/;

	native int modCount()/*-{
        return this.modCount;
	}-*/;

	class EntryIterator implements Iterator {
		JavascriptJavaObjectArray keysSnapshot;

		int idx = -1;

		int itrModCount;

		boolean nextCalled = false;

		Object key;

		public EntryIterator() {
			keysSnapshot = keys();
			this.itrModCount = modCount();
		}

		@Override
		// try-catch due to broken optimisation in chrome 30
		public boolean hasNext() {
			try {
				return idx + 1 < keysSnapshot.length();
			} catch (RuntimeException e) {
				throw e;
			}
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
			if (intLookup()) {
				int intKey = keysSnapshot.getInt(++idx);
				key = Integer.valueOf(intKey);
				return JavascriptKeyableLookup.this.get0(intKey);
			} else {
				key = keysSnapshot.get(++idx);
				return JavascriptKeyableLookup.this.get0(key);
			}
		}

		@Override
		public void remove() {
			if (modCount() != itrModCount) {
				throw new ConcurrentModificationException();
			}
			if (!nextCalled) {
				throw new IllegalStateException();
			}
			if (JavascriptKeyableLookup.this
					.containsKey(keysSnapshot.get(idx))) {
				JavascriptKeyableLookup.this.remove(keysSnapshot.get(idx));
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
}