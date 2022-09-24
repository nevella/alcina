package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;

public class MemoryStat {
	List<MemoryStat> children = new ArrayList<>();

	private Object root;

	MemoryStat parent;

	public ObjectMemory objectMemory;

	public Counter counter = new Counter();

	public StatType type;

	public MemoryStat(Object root) {
		this.root = root;
	}

	public void addChild(MemoryStat child) {
		children.add(child);
		child.parent = this;
		child.objectMemory = objectMemory;
	}

	public void setObjectMemory(ObjectMemory objectMemory) {
		this.objectMemory = objectMemory;
	}

	boolean hasChildren() {
		return children.size() > 0;
	}

	public Query query() {
		return new Query(this);
	}

	public enum Order {
		NAME, ENCOUNTER, SIZE_REVERSED
	}

	public static class Query {
		private Predicate<Class> classFilter;

		private boolean leafOnly;

		private Order order = Order.ENCOUNTER;

		private MemoryStat from;

		public Query(MemoryStat from) {
			this.from = from;
		}

		public Query withClassFilter(Predicate<Class> classFilter) {
			this.classFilter = classFilter;
			return this;
		}

		public Query withLeafOnly(boolean leafOnly) {
			this.leafOnly = leafOnly;
			return this;
		}

		public Query withOrder(Order order) {
			this.order = order;
			return this;
		}

		static class Cmp implements Comparator<MemoryStat> {
			private Order order;

			public Cmp(Order order) {
				this.order = order;
			}

			@Override
			public int compare(MemoryStat o1, MemoryStat o2) {
				switch (order) {
				case ENCOUNTER:
					return 0;
				case NAME:
					return o1.root.toString().compareTo(o2.root.toString());
				case SIZE_REVERSED:
					Counter c1 = new Counter();
					c1.accumulate(o1);
					Counter c2 = new Counter();
					c2.accumulate(o2);
					return -CommonUtils.compareLongs(c1.size,
							c2.size);
				default:
					throw new UnsupportedOperationException();
				}
			}
		}

		List<MemoryStat> stats = new ArrayList<>();

		public String execute() {
			LinkedList<MemoryStat> stack = new LinkedList<>();
			// push, pop from top to preserve encounter order
			stack.addFirst(from);
			Cmp cmp = new Cmp(order);
			while (stack.size() > 0) {
				MemoryStat stat = stack.removeFirst();
				// reverse, because adding to the front of the list - want the
				// 1st element of non-reversed sort added last
				stats.add(stat);
				stat.children.stream().sorted(cmp.reversed())
						.forEach(stack::addFirst);
			}
			if (leafOnly) {
				stats.removeIf(MemoryStat::hasChildren);
				Collections.sort(stats, cmp);
			}
			FormatBuilder builder = new FormatBuilder();
			builder.appendPadLeft(14, "Size");
			builder.appendPadLeft(10, "Count");
			builder.appendBlock("  Object");
			builder.fill(100, "=");
			for (MemoryStat stat : stats) {
				Counter deep = new Counter();
				deep.accumulate(stat);
				builder.appendPadLeft(14, deep.size);
				builder.appendPadLeft(10, deep.count);
				builder.appendPadLeft(stat.depth()*2+2, "");
				builder.appendBlock(stat.root.toString());
				if (classFilter != null && deep.perClassCount.keySet().stream()
						.anyMatch(classFilter::test)) {
					builder.indent(stat.depth() * 4 + 25);
					builder.line("Per-class");
					builder.line("========");
					deep.perClassCount.entrySet().stream()
							.filter(e -> classFilter.test(e.getKey()))
							.forEach(e -> builder.line("%s   %s   %s",
									CommonUtils.padStringRight(
											String.valueOf(e.getValue()), 10,
											' '),
									CommonUtils.padStringRight(String.valueOf(
											deep.perClassSize.get(e.getKey())),
											10, ' '),
									e.getKey().getSimpleName()));
				}
			}
			return builder.toString();
		}
	}

	int depth() {
		return parent == null ? 0 : parent.depth() + 1;
	}

	public static class Counter {
		public long count = 0;

		public long size = 0;

		public Map<Class, Long> perClassSize = new LinkedHashMap<>();

		public Map<Class, Long> perClassCount = new LinkedHashMap<>();

		public Counter() {
		}

		public void accumulate(MemoryStat stat) {
			count += stat.counter.count;
			size += stat.counter.size;
			stat.counter.perClassSize.forEach(
					(k, v) -> perClassSize.merge(k, v, (v1, v2) -> v1 + v2));
			stat.counter.perClassCount.forEach(
					(k, v) -> perClassCount.merge(k, v, (v1, v2) -> v1 + v2));
			for (MemoryStat child : stat.children) {
				accumulate(child);
			}
		}

		@Override
		public String toString() {
			return Ax.format("%s bytes; %s objects", size, count);
		}
	}

	public interface MemoryStatProvider {
		MemoryStat addMemoryStats(MemoryStat parent);
	}

	@Registration(ObjectMemory.class)
	public static abstract class ObjectMemory {
		public abstract void dumpStats();

		public abstract boolean
				isMemoryStatProvider(Class<? extends Object> clazz);

		public abstract void walkStats(Object o, Counter counter,
				Predicate<Object> filter);
	}

	public enum StatType {
		MIN, SAMPLE, EXACT
	}
}
