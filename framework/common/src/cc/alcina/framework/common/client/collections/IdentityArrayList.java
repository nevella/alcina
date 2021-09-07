package cc.alcina.framework.common.client.collections;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Overrides List.equals to force a property change (plus, list.equals is
 * expensive)
 * 
 * @author nick@alcina.cc
 *
 * @param <T>
 */
public class IdentityArrayList<T> extends ArrayList<T> {
	public IdentityArrayList() {
		super();
	}

	public IdentityArrayList(Collection<? extends T> c) {
		super(c);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj;
	}
	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}
}