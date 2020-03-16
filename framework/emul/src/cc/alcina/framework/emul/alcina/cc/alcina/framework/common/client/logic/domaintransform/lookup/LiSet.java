package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

import com.google.gwt.core.client.GwtScriptOnly;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.LiSet;

@GwtScriptOnly
public class LiSet<H extends Entity> extends AbstractSet<H> implements
		Cloneable {
	public LiSet() {
	}

	public LiSet(Collection<? extends H> c) {
		addAll(c);
	}

	public static <H extends Entity> LiSet<H> of(H h) {
		LiSet<H> result = new LiSet<>();
		result.add(h);
		return result;
	}
	
	private Collection<H> values = (Collection<H>) new FastIdLookupScript()
			.values();

	@Override
	public Iterator<H> iterator() {
		return values.iterator();
	}

	@Override
	public int size() {
		return values.size();
	}

	@Override
	public boolean add(H obj) {
		return values.add(obj);
	}

	@Override
	public boolean remove(Object o) {
		return values.remove(o);
	}

	@Override
	public boolean contains(Object o) {
		return values.contains(o);
	}

	public Object clone() {
		return new LiSet<H>(this);
	}
}
