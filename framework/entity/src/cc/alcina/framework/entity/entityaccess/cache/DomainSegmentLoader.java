package cc.alcina.framework.entity.entityaccess.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.domain.IDomainSegmentLoader;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.util.Multiset;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLoaderDatabase.LaterLookup.LaterItem;

public abstract class DomainSegmentLoader implements IDomainSegmentLoader {
	List<LaterItem> toResolve = new ArrayList<>();

	protected Multiset<Class, Set<Long>> toLoadIds = new Multiset<>();

	List<DomainSegmentLoaderProperty> properties = new ArrayList<>();

	public abstract void initialiseSeedLookup();

	public void notifyLater(LaterItem item, Class type, long id) {
		if (properties.stream().anyMatch(property -> property.isIgnore(item))) {
			return;
		}
		toResolve.add(item);
		toLoadIds.add(type, id);
	}

	public int pendingCount() {
		return toLoadIds.allItems().size() + toResolve.size();
	}

	protected void addProperty(Class<? extends HasIdAndLocalId> source,
			String propertyName, Class<? extends HasIdAndLocalId> target,
			DomainSegmentPropertyType type) {
		properties.add(new DomainSegmentLoaderProperty(source, propertyName,
				target, type));
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
}