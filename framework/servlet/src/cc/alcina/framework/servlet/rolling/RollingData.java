package cc.alcina.framework.servlet.rolling;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.RollingDataItem;
import cc.alcina.framework.entity.entityaccess.cache.AlcinaMemCacheQuery;
import cc.alcina.framework.servlet.ServletLayerUtils;

public abstract class RollingData<K extends Comparable, V> {
	protected String typeKey;

	public abstract Function<String, List<V>> deserializer();

	public abstract Function<List<V>, String> serializer();

	public abstract List<V> getData(K from);

	public abstract Function<V, K> keyMaker();

	public abstract Function<String, K> keyDeserializer();

	public SortedMap<K, V> getValues(K earliestKey, String typeKey) {
		this.typeKey = typeKey;
		Class<? extends RollingDataItem> rdImplClass = Registry
				.impl(CommonPersistenceProvider.class)
				.getCommonPersistenceExTransaction()
				.getImplementation(RollingDataItem.class);
		Function<String, K> keyDeserializer = keyDeserializer();
		List<? extends RollingDataItem> list = new AlcinaMemCacheQuery()
				.filter("typeKey", typeKey).list(rdImplClass);
		List<K> existingKeys = list.stream().map(RollingDataItem::getMaxKey)
				.map(k -> keyDeserializer.apply(k))
				.collect(Collectors.toList());
		Optional<K> max = existingKeys.stream().max(Comparator.naturalOrder());
		K from = max.orElse(earliestKey);
		Function<V, K> keyMaker = keyMaker();
		List<V> data = getData(from);
		if (data.size() > 0) {
			Optional<K> maxRetrieved = data.stream().map(keyMaker)
					.max(Comparator.naturalOrder());
			RollingDataItem toPersist = TransformManager.get()
					.createDomainObject(rdImplClass);
			toPersist.setTypeKey(typeKey);
			String dataStr = serializer().apply(data);
			toPersist.setData(dataStr);
			toPersist.setDate(new Date());
			if (maxRetrieved.isPresent()) {
				toPersist.setMaxKey(maxRetrieved.get().toString());
			}
			ServletLayerUtils.pushTransformsAsRoot();
		}
		list = new AlcinaMemCacheQuery().filter("typeKey", typeKey).list(
				rdImplClass);
		TreeMap<K, V> map = new TreeMap<K, V>();
		Function<String, List<V>> deserializer = deserializer();
		for (RollingDataItem item : list) {
			List<V> values = deserializer.apply(item.getData());
			for (V v : values) {
				map.put(keyMaker.apply(v), v);
			}
		}
		return map;
	}
}
