# misc.ops.heap-tools

Domain store usage can be measured via the MemoryStat interface, which provides an accurate view of memory ownership:

```
MemoryStat memoryStats = DomainStore.writableStore()
		.getMemoryStats(StatType.EXACT);
String string = memoryStats.query().withLeafOnly(true)
		 .withClassFilter(
		 ObjectMemoryImpl.entityAndMapAndCollectionFilter)
		.withOrder(Order.SIZE_REVERSED).execute();
Ax.out(string);
```