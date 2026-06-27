package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.serializer.TreeSerializable;

@Bean(PropertySource.FIELDS)
public class ModificationStat implements TreeSerializable {
	public ModificationStat() {
	}

	public ModificationStat(Class<?> type) {
		this.type = type;
	}

	public int added;

	public int removed;

	public int modified;

	public Class<?> type;

	@Override
	public String toString() {
		return Ax.format("[%s] :: added - %s; removed - %s; modified: %s",
				NestedName.get(type), added, removed, modified);
	}

	@Bean(PropertySource.FIELDS)
	public static class Stats implements TreeSerializable {
		public List<ModificationStat> stats = new ArrayList<>();

		public Stats add(Stats other) {
			Map<Class, ModificationStat> byType = new LinkedHashMap<>();
			stats.forEach(s -> byType.put(s.type, s));
			other.stats.forEach(s -> byType
					.computeIfAbsent(s.type, ModificationStat::new).add(s));
			Stats result = new Stats();
			byType.values().forEach(result.stats::add);
			return result;
		}

		public ModificationStat forClass(Class<?> clazz) {
			return stats.stream().filter(s -> s.type == clazz).findFirst()
					.orElseGet(() -> new ModificationStat(clazz));
		}
	}

	void add(ModificationStat other) {
		added += other.added;
		modified += other.modified;
		removed += other.removed;
	}

	public int delta() {
		return added + modified + removed;
	}
}
