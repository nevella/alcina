package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;

import com.google.gwt.core.client.GWT;

public class FastIdLookupJvm implements FastIdLookup {
	private Map<Long, HasIdAndLocalId> idLookup = new LinkedHashMap<Long, HasIdAndLocalId>();

	private Map<Long, HasIdAndLocalId> localIdLookup = new LinkedHashMap<Long, HasIdAndLocalId>();

	private Class clazz;

	private FastIdLookupDevValues values;

	public FastIdLookupJvm(Class clazz) {
		this.clazz = clazz;
		this.values = new FastIdLookupDevValues();
	}

	class FastIdLookupDevValues extends AbstractCollection<HasIdAndLocalId> {
		@Override
		public Iterator<HasIdAndLocalId> iterator() {
			return new MultiIterator<HasIdAndLocalId>(false, localIdLookup
					.values().iterator(), idLookup.values().iterator());
		}

		@Override
		public String toString() {
			return "[" + CommonUtils.join(this, ", ") + "]";
		}

		@Override
		public boolean contains(Object o) {
			if (o instanceof HasIdAndLocalId) {
				HasIdAndLocalId hili = (HasIdAndLocalId) o;
				if (hili.getLocalId() == 0) {
					return get(hili.getId(), false) != null;
				} else {
					return get(hili.getLocalId(), true) != null;
				}
			}
			return false;
		}

		@Override
		public int size() {
			return localIdLookup.size() + idLookup.size();
		}
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("Lkp - %s - [%s,%s]",
				CommonUtils.classSimpleName(clazz), idLookup.size(),
				localIdLookup.size());
	}

	@Override
	public HasIdAndLocalId get(long id, boolean local) {
		checkId(id);
		if (local) {
			return localIdLookup.get(id);
		} else {
			return idLookup.get(id);
		}
	}

	@Override
	public void put(HasIdAndLocalId hili, boolean local) {
		long idi = getApplicableId(hili, local);
		if (local) {
			localIdLookup.put(idi, hili);
		} else {
			idLookup.put(idi, hili);
		}
	}

	@Override
	public void remove(long id, boolean local) {
		checkId(id);
		if (local) {
			localIdLookup.remove(id);
		} else {
			idLookup.remove(id);
		}
	}

	long getApplicableId(HasIdAndLocalId hili, boolean local) {
		long id = local ? hili.getLocalId() : hili.getId();
		checkId(id);
		return id;
	}

	public void checkId(long id) {
		if (GWT.isClient() && id > LongWrapperHash.MAX) {
			throw new RuntimeException("losing higher bits from long");
		}
	}

	@Override
	public Collection<HasIdAndLocalId> values() {
		return values;
	}
}