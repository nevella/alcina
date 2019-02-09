package cc.alcina.framework.servlet.rolling;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.domain.Domain;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.entity.entityaccess.RollingDataItem;
import cc.alcina.framework.servlet.servlet.ServletLayerTransforms;

//TODO - lowpri - formal support for "go back a bit" in transform sequence - probably using transform utc date
public abstract class RollingData<K extends Comparable, V> {
    protected String typeKey;

    public RollingData(String typeKey) {
        this.typeKey = typeKey;
    }

    public SortedMap<K, V> getValues(K earliestKey) {
        synchronized (getClass()) {
            return getValues0(earliestKey);
        }
    }

    private SortedMap<K, V> getValues0(K earliestKey) {
        Class<? extends RollingDataItem> rdImplClass = Registry
                .impl(CommonPersistenceProvider.class)
                .getCommonPersistenceExTransaction()
                .getImplementation(RollingDataItem.class);
        Function<String, K> keyDeserializer = keyDeserializer();
        List<? extends RollingDataItem> list = Domain.query(rdImplClass)
                .filter("typeKey", typeKey).list();
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
            if (maxRetrieved.equals(max)) {
            } else {
                RollingDataItem toPersist = TransformManager.get()
                        .createDomainObject(rdImplClass);
                toPersist.setTypeKey(typeKey);
                String dataStr = serializer().apply(data);
                toPersist.setData(dataStr);
                toPersist.setDate(new Date());
                if (maxRetrieved.isPresent()) {
                    toPersist.setMaxKey(maxRetrieved.get().toString());
                }
                ServletLayerTransforms.pushTransformsAsRoot();
            }
        }
        list = Domain.query(rdImplClass).filter("typeKey", typeKey).list();
        TreeMap<K, V> map = new TreeMap<K, V>();
        Function<String, List<V>> deserializer = deserializer();
        try {
            for (RollingDataItem item : list) {
                List<V> values = deserializer.apply(item.getData());
                for (V v : values) {
                    map.put(keyMaker.apply(v), v);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out
                    .println("Exception with list deserialization - deleting");
            TransformManager.get().deleteMultiple(list);
            ServletLayerTransforms.pushTransformsAsRoot();
            return getValues0(earliestKey);
        }
        return map;
    }

    protected abstract Function<String, List<V>> deserializer();

    protected abstract List<V> getData(K from);

    protected abstract Function<String, K> keyDeserializer();

    protected abstract Function<V, K> keyMaker();

    protected abstract Function<List<V>, String> serializer();
}
