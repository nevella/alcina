package cc.alcina.framework.common.client.csobjects;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Used because otherwise gwt serializer will add all sorts of weird collections
 * (which subclass arraylist) to our beautiful RPC lists - e.g.
 * ChangeListenerCollection etc etc
 * 
 * @author nick@alcina.cc
 *
 */
public class GArrayList<E> extends ArrayList<E> {
	public GArrayList() {
		super();
	}

	public GArrayList(Collection<? extends E> c) {
		super(c);
	}

	public GArrayList(int initialCapacity) {
		super(initialCapacity);
	}
}
