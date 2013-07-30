package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.CommonUtils;

public class FastIdLookupScript implements FastIdLookup {
	private JavascriptIntLookup idLookup = JavascriptIntLookup.create();

	private JavascriptIntLookup localIdLookup = JavascriptIntLookup.create();

	private FastIdLookupScriptValues values;

	public FastIdLookupScript() {
		this.values = new FastIdLookupScriptValues();
	}

	class FastIdLookupScriptValues extends AbstractCollection<HasIdAndLocalId> {
		@Override
		public Iterator<HasIdAndLocalId> iterator() {
			return new MultiIterator<HasIdAndLocalId>(false,
					localIdLookup.valuesIterator(), idLookup.valuesIterator());
		}

		@Override
		public boolean contains(Object o) {
			if (o instanceof HasIdAndLocalId) {
				HasIdAndLocalId hili = (HasIdAndLocalId) o;
				HasIdAndLocalId existing = null;
				if (hili.getLocalId() == 0) {
					existing = get(hili.getId(), false);
				} else {
					existing = get(hili.getLocalId(), true);
				}
				return existing != null
						&& hili.getClass() == existing.getClass();
			}
			return false;
		}

		@Override
		public int size() {
			return localIdLookup.size() + idLookup.size();
		}

		@Override
		public boolean add(HasIdAndLocalId hili) {
			boolean contains = contains(hili);
			put(hili, hili.getId() == 0);
			return !contains;
		}

		@Override
		public boolean remove(Object o) {
			if (o instanceof HasIdAndLocalId) {
				HasIdAndLocalId hili = (HasIdAndLocalId) o;
				boolean local = hili.getId() == 0;
				boolean contains = contains(o);
				FastIdLookupScript.this.remove(hili.getId(), false);
				FastIdLookupScript.this.remove(hili.getLocalId(), true);
			}
			return false;
		}
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("Lkp - [%s,%s]", idLookup.size(),
				localIdLookup.size());
	}

	@Override
	public HasIdAndLocalId get(long id, boolean local) {
		int idi = LongWrapperHash.fastIntValue(id);
		if (local) {
			return localIdLookup.get(idi);
		} else {
			return idLookup.get(idi);
		}
	}

	@Override
	public void put(HasIdAndLocalId hili, boolean local) {
		int idi = getApplicableId(hili, local);
		if (local) {
			localIdLookup.put(idi, hili);
		} else {
			idLookup.put(idi, hili);
		}
	}

	@Override
	public void remove(long id, boolean local) {
		int idi = LongWrapperHash.fastIntValue(id);
		if (local) {
			localIdLookup.remove(idi);
		} else {
			idLookup.remove(idi);
		}
	}

	int getApplicableId(HasIdAndLocalId hili, boolean local) {
		long id = local ? hili.getLocalId() : hili.getId();
		int idi = LongWrapperHash.fastIntValue(id);
		return idi;
	}

	@Override
	public Collection<HasIdAndLocalId> values() {
		return values;
	}
}