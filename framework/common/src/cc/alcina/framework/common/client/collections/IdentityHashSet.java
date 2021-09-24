package cc.alcina.framework.common.client.collections;

import java.util.Collection;
import java.util.HashSet;

/**
 * Overrides Set.equals to force a property change (plus, set.equals is
 * expensive)
 * 
 * @author nick@alcina.cc
 *
 * @param <T>
 */
public class IdentityHashSet<T> extends HashSet<T> {
	public IdentityHashSet() {
		super();
	}

	public IdentityHashSet(Collection<? extends T> c) {
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