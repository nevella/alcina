package cc.alcina.framework.servlet.component.entity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.NotificationObservable;

@Feature.Ref(Feature_EntityBrowser._QueryCache.class)
class NonOptimisedQueryCache {
	Map<String, List> queryCache = new ConcurrentHashMap<>();

	List get(String cacheMarker) {
		return queryCache.get(cacheMarker);
	}

	void put(String cacheMarker, List results) {
		queryCache.put(cacheMarker, results);
		entrySummary.updateItems();
	}

	EntrySummary entrySummary = new EntrySummary();

	public Object createUi() {
		return entrySummary;
	}

	void clear() {
		queryCache.clear();
		entrySummary.updateItems();
	}

	@TypedProperties
	class EntrySummary extends Model.All implements DomEvents.Click.Handler {
		Object message;

		PackageProperties._NonOptimisedQueryCache_EntrySummary.InstanceProperties
				properties() {
			return PackageProperties.nonOptimisedQueryCache_entrySummary
					.instance(this);
		}

		@Override
		public void onClick(Click event) {
			clear();
			NotificationObservable.of("Cleared query cache").publish();
			;
		}

		public void updateItems() {
			Object message = null;
			if (queryCache.size() > 0) {
				String label = Ax.format("Cached queries: %s",
						queryCache.size());
				String title = queryCache.keySet().stream()
						.collect(Collectors.joining("\n"));
				message = new LeafModel.TextTitle(label, title);
			}
			properties().message().set(message);
		}
	}
}