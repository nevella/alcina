package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

/**
 * This is the super-source for the gwt client
 * (/alcina/framework/emul/src/cc/alcina
 * /framework/emul/alcina/cc/alcina/framework
 * /common/client/logic/domaintransform/lookup/LiSet.java)
 * 
 * @author nick@alcina.cc
 * 
 * @param <H>
 */
public class LiSetScript<H extends HasIdAndLocalId> extends AbstractSet<H>
		implements Cloneable {
	private Collection<H> values = (Collection<H>) new FastIdLookupScript()
			.values();

	@Override
	public Iterator<H> iterator() {
		return values.iterator();
	}

	public LiSetScript() {
	}

	public LiSetScript(Collection<? extends H> c) {
		addAll(c);
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
		return new LiSetScript<H>(this);
	}
}
