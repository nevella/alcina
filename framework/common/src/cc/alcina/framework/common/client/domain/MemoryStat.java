package cc.alcina.framework.common.client.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;

public class MemoryStat {
	List<MemoryStat> children = new ArrayList<>();

	private Object root;

	MemoryStat parent;

	public ObjectMemory objectMemory;

	public Counter counter = new Counter();

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

	@Override
	public String toString() {
		FormatBuilder builder = new FormatBuilder();
		builder.indent(depth() * 4);
		builder.line(root.toString());
		builder.indent(depth() * 4 + 2);
		builder.line(counter.toString());
		Counter deep = new Counter();
		deep.accumulate(this);
		builder.line(deep.toString());
		builder.indent(0);
		for (MemoryStat stat : children) {
			builder.appendIfNotBlank(stat.toString());
		}
		return builder.toString();
	}

	int depth() {
		return parent == null ? 0 : parent.depth() + 1;
	}

	public static class Counter {
		public long count = 0;

		public long size = 0;

		public Map<Class, Long> perClass = new LinkedHashMap<>();

		public void accumulate(MemoryStat stat) {
			count += stat.counter.count;
			size += stat.counter.size;
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
		MemoryStat addMemoryStats(MemoryStat parent, StatType type);
	}

	@RegistryLocation(registryPoint = ObjectMemory.class)
	public static abstract class ObjectMemory {
		public abstract void walkStats(Object o, Counter counter);
	}

	public enum StatType {
		MIN, SAMPLE, EXACT
	}
}