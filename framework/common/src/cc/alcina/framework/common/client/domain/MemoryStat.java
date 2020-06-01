package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
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

	public String toString(Predicate<Class> classFilter) {
		FormatBuilder builder = new FormatBuilder();
		builder.indent(depth() * 4);
		builder.line(root.toString());
		// builder.indent(depth() * 4 + 2);
		// builder.line(counter.toString());
		Counter deep = new Counter();
		deep.accumulate(this);
		builder.line(deep.toString());
		if (classFilter != null && deep.perClassCount.keySet().stream()
				.anyMatch(classFilter::test)) {
			builder.line("Per-class");
			builder.line("========");
			deep.perClassCount.entrySet().stream()
					.filter(e -> classFilter.test(e.getKey()))
					.forEach(e -> builder.line("%s   %s   %s",
							CommonUtils.padStringRight(
									String.valueOf(e.getValue()), 10, ' '),
							CommonUtils.padStringRight(
									String.valueOf(
											deep.perClassSize.get(e.getKey())),
									10, ' '),
							e.getKey().getSimpleName()));
		}
		builder.indent(0);
		for (MemoryStat stat : children) {
			builder.appendIfNotBlank(stat.toString(classFilter));
		}
		return builder.toString();
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

	@RegistryLocation(registryPoint = ObjectMemory.class)
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