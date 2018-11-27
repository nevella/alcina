package cc.alcina.framework.entity.entityaccess.cache;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.domain.IDomainSegmentLoader;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.MultikeyMap;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.common.client.util.UnsortedMultikeyMap;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLoaderDatabase.LaterLookup.LaterItem;

public abstract class DomainSegmentLoader implements IDomainSegmentLoader {
	List<LaterItem> toResolve = new ArrayList<>();

	protected Multiset<Class, Set<Long>> toLoadIds = new Multiset<>();

	List<DomainSegmentLoaderProperty> properties = new ArrayList<>();

	// clazz,property,id,id
	MultikeyMap<Long> queried = new UnsortedMultikeyMap<>(3);

	public DomainSegmentLoader() {
		if (isReload()) {
			loadIds();
		}
	}

	public synchronized Collection<Long> filterForQueried(Class clazz,
			String property, Collection<Long> ids) {
		ids = new ArrayList<>(ids);
		ids.removeAll(queried.asMapEnsure(true, clazz, property).keySet());
		ids.forEach(id -> queried.put(clazz, property, id, id));
		return ids;
	}

	public abstract String getFilename();

	public abstract void initialiseSeedLookup() throws Exception;

	public boolean isReload() {
		return new File(getFilename()).exists();
	}

	public void loadIds() {
		SavedIdsHolder holder = KryoUtils.deserializeFromFile(
				new File(getFilename()), SavedIdsHolder.class);
		toLoadIds = holder.toLoadIds;
	}

	public synchronized void notifyLater(LaterItem item, Class type, long id) {
		if (properties.stream().anyMatch(property -> property.isIgnore(item))) {
			return;
		}
		toResolve.add(item);
		toLoadIds.add(type, id);
	}

	public int pendingCount() {
		return toLoadIds.allItems().size() + toResolve.size();
	}

	public void saveIds() {
		SavedIdsHolder holder = new SavedIdsHolder();
		toLoadIds.keySet().forEach(clazz -> holder.toLoadIds.addCollection(
				clazz, DomainStore.stores().databaseStore().cache.keys(clazz)));
		KryoUtils.serializeToFile(holder, new File(getFilename()));
	}

	protected void addProperty(Class<? extends HasIdAndLocalId> source,
			String propertyName, Class<? extends HasIdAndLocalId> target,
			DomainSegmentPropertyType type) {
		properties.add(new DomainSegmentLoaderProperty(source, propertyName,
				target, type));
	}

	synchronized void ensureClass(Class clazz) {
		toLoadIds.addCollection(clazz, new ArrayList<>());
	}

	public static class DomainSegmentLoaderProperty {
		Class<? extends HasIdAndLocalId> source;

		String propertyName;

		Class<? extends HasIdAndLocalId> target;

		DomainSegmentPropertyType type;

		public DomainSegmentLoaderProperty(
				Class<? extends HasIdAndLocalId> source, String propertyName,
				Class<? extends HasIdAndLocalId> target,
				DomainSegmentPropertyType type) {
			this.source = source;
			this.propertyName = propertyName;
			this.target = target;
			this.type = type;
		}

		public boolean isIgnore(LaterItem item) {
			if (type == DomainSegmentPropertyType.IGNORE) {
				if (item.pdOperator.pd.getName().equals(propertyName)
						&& item.source.getClass() == source) {
					return true;
				}
			}
			return false;
		}
	}

	public enum DomainSegmentPropertyType {
		TABLE_REF, STORE_REF, IGNORE
	}

	public static class SavedIdsHolder {
		public Multiset<Class, Set<Long>> toLoadIds = new Multiset<>();
	}
}