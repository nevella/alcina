package cc.alcina.framework.common.client.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Overrides List.equals to force a property change (plus, list.equals is
 * expensive)
 *
 * @author nick@alcina.cc
 *
 * @param <T>
 */
public class IdentityArrayList<T> extends ArrayList<T> {
	public static <T> IdentityArrayList<T> add(List<T> original, T delta) {
		IdentityArrayList<T> result = new IdentityArrayList<>(original);
		result.add(delta);
		return result;
	}

	public static <T> IdentityArrayList<T> copyOf(Collection<? extends T> c) {
		return new IdentityArrayList<>(c);
	}

	public static <T> IdentityArrayList<T> delta(List<T> original, T delta,
			boolean add) {
		IdentityArrayList<T> result = new IdentityArrayList<>(original);
		if (add) {
			result.add(delta);
		} else {
			result.remove(delta);
		}
		return result;
	}

	public static <T> List<T> insert(List<T> original, T insert, T after) {
		IdentityArrayList<T> result = new IdentityArrayList<>(original);
		int index = result.indexOf(after);
		result.add(index + 1, insert);
		return result;
	}

	public static <T> IdentityArrayList<T> remove(List<T> original, T delta) {
		IdentityArrayList<T> result = new IdentityArrayList<>(original);
		result.remove(delta);
		return result;
	}

	public IdentityArrayList() {
		super();
	}

	public IdentityArrayList(Collection<? extends T> c) {
		super(c == null ? Collections.emptyList() : c);
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