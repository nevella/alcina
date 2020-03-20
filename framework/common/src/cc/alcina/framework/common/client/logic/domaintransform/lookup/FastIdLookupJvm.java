package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;

public class FastIdLookupJvm implements FastIdLookup {
	private Map<Long, Entity> idLookup = new LinkedHashMap<Long, Entity>();

	private Map<Long, Entity> localIdLookup = new LinkedHashMap<Long, Entity>();

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
	public Entity get(long id, boolean local) {
		checkId(id);
		if (local) {
			return localIdLookup.get(id);
		} else {
			return idLookup.get(id);
		}
	}

	@Override
	public void put(Entity entity, boolean local) {
		long idi = getApplicableId(entity, local);
		if (local) {
			localIdLookup.put(idi, entity);
		} else {
			idLookup.put(idi, entity);
		}
	}

	@Override
	public void putAll(Collection<Entity> values, boolean local) {
		for (Entity value : values) {
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
		return Ax.format("Lkp  - [%s,%s]", idLookup.size(),
				localIdLookup.size());
	}

	@Override
	public Collection<Entity> values() {
		return values;
	}

	long getApplicableId(Entity entity, boolean local) {
		long id = local ? entity.getLocalId() : entity.getId();
		checkId(id);
		return id;
	}

	class FastIdLookupDevValues extends AbstractCollection<Entity> {
		@Override
		public boolean contains(Object o) {
			if (o instanceof Entity) {
				Entity entity = (Entity) o;
				if (entity.getLocalId() == 0) {
					return get(entity.getId(), false) != null;
				} else {
					return get(entity.getLocalId(), true) != null;
				}
			}
			return false;
		}

		@Override
		public Iterator<Entity> iterator() {
			return new MultiIterator<Entity>(false, null,
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