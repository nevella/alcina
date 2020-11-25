package cc.alcina.framework.common.client.logic.domaintransform.lookup;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.Ax;

public class FastIdLookupScript implements FastIdLookup {
	private JavascriptIntLookup idLookup = JavascriptIntLookup.create();

	private JavascriptIntLookup localIdLookup = JavascriptIntLookup.create();
	private JavascriptIntLookup localIdToPromoted = JavascriptIntLookup.create();

	private FastIdLookupScriptValues values;

	public FastIdLookupScript() {
		this.values = new FastIdLookupScriptValues();
	}

	@Override
	public Entity get(long id, boolean local) {
		int idi = LongWrapperHash.fastIntValue(id);
		if (local) {
			Entity entity = localIdLookup.get(idi);
			if(entity==null) {
				entity=localIdToPromoted.get(idi);
			}
			return entity;
		} else {
			return idLookup.get(idi);
		}
	}

	@Override
	public void put(Entity entity, boolean local) {
		int idi = getApplicableId(entity, local);
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
		int idi = LongWrapperHash.fastIntValue(id);
		if (local) {
			localIdLookup.remove(idi);
		} else {
			idLookup.remove(idi);
		}
	}

	@Override
	public String toString() {
		return Ax.format("Lkp - [%s,%s]", idLookup.size(),
				localIdLookup.size());
	}

	@Override
	public Collection<Entity> values() {
		return values;
	}

	int getApplicableId(Entity entity, boolean local) {
		long id = local ? entity.getLocalId() : entity.getId();
		int idi = LongWrapperHash.fastIntValue(id);
		return idi;
	}

	class FastIdLookupScriptValues extends AbstractCollection<Entity> {
		@Override
		public boolean add(Entity entity) {
			boolean contains = contains(entity);
			put(entity, entity.getId() == 0);
			return !contains;
		}

		@Override
		public boolean contains(Object o) {
			if (o instanceof Entity) {
				Entity entity = (Entity) o;
				Entity existing = null;
				if (entity.getLocalId() == 0) {
					existing = get(entity.getId(), false);
				} else {
					existing = get(entity.getLocalId(), true);
				}
				return existing != null
						&& entity.getClass() == existing.getClass();
			}
			return false;
		}

		@Override
		public Iterator<Entity> iterator() {
			return new MultiIterator<Entity>(true, null,
					localIdLookup.valuesIterator(), idLookup.valuesIterator());
		}

		@Override
		public boolean remove(Object o) {
			if (o instanceof Entity) {
				Entity entity = (Entity) o;
				boolean local = entity.getId() == 0;
				boolean contains = contains(o);
				FastIdLookupScript.this.remove(entity.getId(), false);
				FastIdLookupScript.this.remove(entity.getLocalId(), true);
			}
			return false;
		}

		@Override
		public int size() {
			return localIdLookup.size() + idLookup.size();
		}
	}
	@Override
	public void changeMapping(Entity entity, long id, long localId) {
		remove(id, false);
		remove(localId, true);
		int localIdi = LongWrapperHash.fastIntValue(localId);
		localIdToPromoted.put(localIdi, entity);
	}
}