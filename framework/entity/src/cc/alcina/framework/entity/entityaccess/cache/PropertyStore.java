package cc.alcina.framework.entity.entityaccess.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.collections.CollectionFilter;
import cc.alcina.framework.common.client.collections.FilterContext;
import cc.alcina.framework.common.client.collections.PropertyFilter;
import cc.alcina.framework.common.client.collections.PropertyPathFilter;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.lookup.DetachedEntityCache;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.entity.entityaccess.cache.DomainStoreLoaderDatabase.PdOperator;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

/**
 * Stores object properties as arrays
 * 
 * @author nick@alcina.cc
 *
 */
public class PropertyStore {
    List<FieldStore> stores = new ArrayList<>();

    int idIndex = 0;

    Long2IntOpenHashMap rowLookup;

    private List<PdOperator> pds;

    protected boolean coerceLongsToInts = false;

    private PdOperator idOperator;

    protected int emptyRowIdx;

    protected int tableSize;

    private List<PropertyStoreLookup> lookups = new ArrayList<>();

    private List<PropertyStoreProjection> projections = new ArrayList<>();

    public PropertyStore() {
    }

    public void addRow(Object[] row) throws SQLException {
        long id = (long) row[idIndex];
        int rowIdx = ensureRow(id);
        for (int idx = 0; idx < pds.size(); idx++) {
            PdOperator pd = pds.get(idx);
            stores.get(pd.idx).putRowField(row, idx, rowIdx);
        }
        for (PropertyStoreLookup lookup : lookups) {
            lookup.insert(row, id);
        }
        for (PropertyStoreProjection projection : projections) {
            projection.insert(row, id);
        }
    }

    public FilterContext createContext(DetachedEntityCache cache) {
        return new PsFilterContext(cache);
    }

    public <T> List<T> fieldValues(String propertyPath) {
        if (pds == null) {
            return new ArrayList<>();
        }
        PdOperator descriptor = getDescriptor(propertyPath);
        FieldStore store = stores.get(descriptor.idx);
        return (List<T>) (List) rowOffsets().stream().filter(idx -> idx >= 0)
                .map(idx -> store.getWrapped(idx)).collect(Collectors.toList());
    }

    public boolean getBooleanValue(PdOperator pd, int rowOffset) {
        if (rowOffset != -1) {
            return ((BooleanStore) stores.get(pd.idx)).get(rowOffset);
        }
        return false;
    }

    public PdOperator getDescriptor(String propertyPath) {
        return pds.stream()
                .filter(pd -> pd.pd.getName().equals(propertyPath)
                        || pd.pd.getName().equals(propertyPath + "Id"))
                .findFirst().orElse(null);
    }

    public Set<Long> getIds() {
        LongOpenHashSet res = new LongOpenHashSet();
        LongIterator itr = rowLookup.keySet().iterator();
        while (itr.hasNext()) {
            res.add(itr.nextLong());
        }
        return res;
    }

    public Integer getIntegerValue(PdOperator pd, int rowOffset) {
        int value = getPrimitiveIntValue(pd, rowOffset);
        return value == 0 ? null : value;
    }

    public Long getLongValue(PdOperator pd, int rowOffset) {
        long value = getPrimitiveLongValue(pd, rowOffset);
        return value == 0 ? null : value;
    }

    public int getPrimitiveIntValue(PdOperator pd, int rowOffset) {
        if (rowOffset != -1) {
            return ((IntStore) stores.get(pd.idx)).get(rowOffset);
        }
        return 0;
    }

    public long getPrimitiveLongValue(PdOperator pd, int rowOffset) {
        if (rowOffset != -1) {
            return ((LongStore) stores.get(pd.idx)).get(rowOffset);
        }
        return 0;
    }

    public String getStringValue(PdOperator pd, int rowOffset) {
        if (rowOffset != -1) {
            return ((StringStore) stores.get(pd.idx)).get(rowOffset);
        }
        return null;
    }

    public Object getValue(PdOperator pd, Long id) {
        int rowOffset = getRowOffset(id);
        if (rowOffset != -1) {
            return stores.get(pd.idx).getWrapped(rowOffset);
        }
        return null;
    }

    public void index(HasIdAndLocalId obj, boolean add) {
        for (PropertyStoreLookup lookup : lookups) {
            lookup.index(obj, add);
        }
        for (PropertyStoreProjection projection : projections) {
            projection.index(obj, add);
        }
    }

    public void init(List<PdOperator> pds) {
        this.pds = pds;
        stores = new ArrayList();
        initRowLookup();
        String propertyName = "id";
        this.idOperator = getDescriptor(propertyName);
        idIndex = pds.indexOf(idOperator);
        tableSize = getInitialSize();
        pds.forEach(pd -> {
            stores.add(getFieldStoreFor(pd.pd.getPropertyType()));
        });
        lookups.forEach(lkp -> lkp.initPds());
    }

    public void remove(long id) {
        rowLookup.remove(id);
    }

    public void setBooleanValue(PdOperator pd, int rowIdx, boolean value) {
        ((BooleanStore) stores.get(pd.idx)).put(value, rowIdx);
    }

    public void setIntegerValue(PdOperator pd, int rowIdx, Integer value) {
        ((LongStore) stores.get(pd.idx)).put(value, rowIdx);
    }

    public void setLongValue(PdOperator pd, int rowIdx, Long value) {
        ((LongStore) stores.get(pd.idx)).put(value, rowIdx);
    }

    public void setStringValue(PdOperator pd, int rowIdx, String value) {
        ((StringStore) stores.get(pd.idx)).put(value, rowIdx);
    }

    protected int ensureRow(long id) {
        if (!rowLookup.containsKey(id)) {
            rowLookup.put(id, emptyRowIdx++);
            ensureStoreSizes(rowLookup.size());
        }
        return rowLookup.get(id);
    }

    protected void ensureStoreSizes(int size) {
        for (FieldStore store : stores) {
            store.ensureCapacity(size);
        }
    }

    protected FieldStore getFieldStoreFor(Class<?> propertyType) {
        if (propertyType == long.class || propertyType == Long.class) {
            return new LongStore(tableSize);
        } else if (propertyType == boolean.class
                || propertyType == Boolean.class) {
            return new BooleanStore(tableSize);
        } else if (propertyType == String.class) {
            return new DuplicateStringStore(tableSize);
        } else if (propertyType == int.class || propertyType == Integer.class) {
            return new IntStore(tableSize);
        }
        throw new UnsupportedOperationException();
    }

    protected int getInitialSize() {
        return 100;
    }

    protected Object getLongArray() {
        return new long[tableSize];
    }

    protected int getRowOffset(Long id) {
        if (rowLookup.containsKey(id)) {
            return rowLookup.get(id);
        }
        return -1;
    }

    protected void initRowLookup() {
        rowLookup = new Long2IntOpenHashMap(getInitialSize());
    }

    protected IntCollection rowOffsets() {
        return rowLookup.values();
    }

    void addLookup(PropertyStoreLookup lookup) {
        lookups.add(lookup);
    }

    void addProjection(PropertyStoreProjection projection) {
        projections.add(projection);
    }

    static class BooleanStore extends FieldStore<Boolean> {
        BooleanArrayList list;

        public BooleanStore(int size) {
            super(size);
            list = new BooleanArrayList(size);
        }

        @Override
        public void ensureCapacity(int capacity) {
            if (list.size() < capacity) {
                list.add(false);
            }
        }

        @Override
        public void putRowField(Object[] row, int colIdx, int rowIdx) {
            put(CommonUtils.bv((Boolean) row[colIdx]), rowIdx);
        }

        @Override
        public void putRsField(ResultSet rs, int colIdx, int rowIdx)
                throws SQLException {
            put(rs.getBoolean(colIdx), rowIdx);
        }

        @Override
        protected Boolean getWrapped(int rowOffset) {
            return get(rowOffset);
        }

        boolean get(int rowIdx) {
            return list.getBoolean(rowIdx);
        }

        void put(boolean value, int rowIdx) {
            if (list.size() == rowIdx) {
                list.add(value);
            } else {
                list.set(rowIdx, value);
            }
        }
    }

    static class DuplicateStringStore extends StringStore {
        Object2IntOpenHashMap<String> stringIdLookup;

        Int2ObjectOpenHashMap<String> idStringLookup;

        Int2IntOpenHashMap rowIdLookup;

        public DuplicateStringStore(int size) {
            super(size);
            stringIdLookup = new Object2IntOpenHashMap<String>(size / 10);
            idStringLookup = new Int2ObjectOpenHashMap<String>(size / 10);
            rowIdLookup = new Int2IntOpenHashMap(size);
        }

        @Override
        public void ensureCapacity(int size) {
            // noop
        }

        @Override
        public void putRowField(Object[] row, int colIdx, int rowIdx) {
            put((String) row[colIdx], rowIdx);
        }

        @Override
        public void putRsField(ResultSet rs, int colIdx, int rowIdx)
                throws SQLException {
            put(rs.getString(colIdx), rowIdx);
        }

        @Override
        protected String getWrapped(int rowOffset) {
            return get(rowOffset);
        }

        @Override
        String get(int rowIdx) {
            if (rowIdLookup.containsKey(rowIdx)) {
                int stringId = rowIdLookup.get(rowIdx);
                return idStringLookup.get(stringId);
            }
            return null;
        }

        // not synchronized
        @Override
        void put(String string, int rowIdx) {
            if (!stringIdLookup.containsKey(string)) {
                int stringId = stringIdLookup.size();
                stringIdLookup.put(string, stringId);
                idStringLookup.put(stringId, string);
            }
            int stringId = stringIdLookup.getInt(string);
            rowIdLookup.put(rowIdx, stringId);
        }
    }

    abstract static class FieldStore<T> {
        public FieldStore(int size) {
        }

        public abstract void ensureCapacity(int size);

        public abstract void putRowField(Object[] row, int idx, int rowIdx);

        public abstract void putRsField(ResultSet rs, int colIdx, int rowIdx)
                throws SQLException;

        protected abstract T getWrapped(int rowOffset);
    }

    static class IntStore extends FieldStore<Integer> {
        IntArrayList list;

        public IntStore(int size) {
            super(size);
            list = new IntArrayList(size);
        }

        @Override
        public void ensureCapacity(int capacity) {
            if (list.size() < capacity) {
                list.add(0);
            }
        }

        @Override
        public void putRowField(Object[] row, int colIdx, int rowIdx) {
            put(CommonUtils.iv((Integer) row[colIdx]), rowIdx);
        }

        @Override
        public void putRsField(ResultSet rs, int colIdx, int rowIdx)
                throws SQLException {
            put(rs.getInt(colIdx), rowIdx);
        }

        @Override
        protected Integer getWrapped(int rowOffset) {
            return get(rowOffset);
        }

        Integer get(int rowIdx) {
            return list.getInt(rowIdx);
        }

        void put(int value, int rowIdx) {
            if (list.size() == rowIdx) {
                list.add(value);
            } else {
                list.set(rowIdx, value);
            }
        }
    }

    static class LongStore extends FieldStore<Long> {
        LongArrayList list;

        public LongStore(int size) {
            super(size);
            list = new LongArrayList(size);
        }

        @Override
        public void ensureCapacity(int capacity) {
            if (list.size() < capacity) {
                list.add(0);
            }
        }

        @Override
        public void putRowField(Object[] row, int colIdx, int rowIdx) {
            put(CommonUtils.lv((Long) row[colIdx]), rowIdx);
        }

        @Override
        public void putRsField(ResultSet rs, int colIdx, int rowIdx)
                throws SQLException {
            put(rs.getLong(colIdx), rowIdx);
        }

        @Override
        protected Long getWrapped(int rowOffset) {
            return get(rowOffset);
        }

        long get(int rowIdx) {
            return list.getLong(rowIdx);
        }

        void put(long value, int rowIdx) {
            if (list.size() == rowIdx) {
                list.add(value);
            } else {
                list.set(rowIdx, value);
            }
        }
    }

    class PsFilterContext implements FilterContext {
        private DetachedEntityCache cache;

        public PsFilterContext(DetachedEntityCache cache) {
            this.cache = cache;
        }

        @Override
        public CollectionFilter createContextFilter(CollectionFilter original) {
            return new PsFilterContextFilter((PropertyPathFilter) original);
        }

        class PsFilterContextFilter implements CollectionFilter {
            private String p1;

            private PdOperator pd;

            private PropertyPathFilter suffixFilter;

            private PropertyFilter valueFilter;

            public PsFilterContextFilter(PropertyPathFilter original) {
                String[] paths = original.getAccessor().getPaths();
                this.p1 = paths[0];
                this.pd = getDescriptor(p1);
                if (paths.length > 1) {
                    String suffix = Arrays.asList(paths)
                            .subList(1, paths.length).stream()
                            .collect(Collectors.joining("."));
                    this.suffixFilter = new PropertyPathFilter(suffix,
                            original.getTargetValue(),
                            original.getFilterOperator());
                } else {
                    this.valueFilter = new PropertyFilter(null,
                            original.getTargetValue(),
                            original.getFilterOperator());
                }
            }

            @Override
            public boolean allow(Object o) {
                Object value = getValue(pd, (Long) o);
                if (valueFilter != null) {
                    return valueFilter.matchesValue(value);
                } else {
                    Object hili = cache.get(pd.mappedClass, (Long) value);
                    return suffixFilter.allow(hili);
                }
            }
        }
    }

    abstract static class StringStore extends FieldStore<String> {
        public StringStore(int size) {
            super(size);
        }

        abstract String get(int rowIdx);

        abstract void put(String string, int rowIdx);
    }
}
