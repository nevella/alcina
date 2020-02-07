package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;

public class FastIdLookupJvm implements FastIdLookup {
	private Map<Long, HasIdAndLocalId> idLookup = new LinkedHashMap<Long, HasIdAndLocalId>();

	private Map<Long, HasIdAndLocalId> localIdLookup = new LinkedHashMap<Long, HasIdAndLocalId>();

	private FastIdLookupDevValues values;

	public FastIdLookupJvm() {
		this.values = new FastIdLookupDevValues();
	}

	public void checkId(long id) {
		if (GWT.isClient() && id > LongWrapperHash.MAX) {
			throw new RuntimeException("losing higher bits from long");
		}
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
	public void putAll(Collection<HasIdAndLocalId> values, boolean local) {
		for (HasIdAndLocalId value : values) {
			put(value, local);
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

	@Override
	public String toString() {
		return CommonUtils.formatJ("Lkp  - [%s,%s]", idLookup.size(),
				localIdLookup.size());
	}

	@Override
	public Collection<HasIdAndLocalId> values() {
		return values;
	}

	long getApplicableId(HasIdAndLocalId hili, boolean local) {
		long id = local ? hili.getLocalId() : hili.getId();
		checkId(id);
		return id;
	}

	class FastIdLookupDevValues extends AbstractCollection<HasIdAndLocalId> {
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
		public Iterator<HasIdAndLocalId> iterator() {
			return new MultiIterator<HasIdAndLocalId>(false, null,
					localIdLookup.values().iterator(),
					idLookup.values().iterator());
		}

		@Override
		public int size() {
			return localIdLookup.size() + idLookup.size();
		}

		@Override
		public String toString() {
			return "[" + CommonUtils.join(this, ", ") + "]";
		}
	}
}