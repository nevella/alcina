package cc.alcina.framework.entity.entityaccess.cache.mvcc;

import java.util.AbstractMap;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

public class MvccMap<K, V> extends AbstractMap<K, V> {
    private MvccLayer base = new MvccLayer();

    private ConcurrentSkipListMap<MvccTransaction, MvccLayer> transactions;

    @Override
    public Set<Entry<K, V>> entrySet() {
        // TODO Auto-generated method stub
        return null;
    }
}
