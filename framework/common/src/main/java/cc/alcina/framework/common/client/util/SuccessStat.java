package cc.alcina.framework.common.client.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.serializer.TreeSerializable;

@Bean(PropertySource.FIELDS)
public class SuccessStat implements TreeSerializable {
	public SuccessStat() {
	}

	public SuccessStat(Class<?> type) {
		this.type = type;
	}

	public int pass;

	public int fail;

	public int exception;

	public Class<?> type;

	@Override
	public String toString() {
		return Ax.format("[%s] :: pass/fail/exception/total :: %a/%a/%s",
				NestedName.get(type), pass, fail, exception, total());
	}

	@Bean(PropertySource.FIELDS)
	public static class Stats implements TreeSerializable {
		public List<SuccessStat> stats = new ArrayList<>();

		public Stats add(Stats other) {
			Map<Class, SuccessStat> byType = new LinkedHashMap<>();
			stats.forEach(s -> byType.put(s.type, s));
			other.stats.forEach(s -> byType
					.computeIfAbsent(s.type, SuccessStat::new).add(s));
			Stats result = new Stats();
			byType.values().forEach(result.stats::add);
			return result;
		}

		public SuccessStat forClass(Class<?> clazz) {
			return stats.stream().filter(s -> s.type == clazz).findFirst()
					.orElseGet(() -> new SuccessStat(clazz));
		}
	}

	void add(SuccessStat other) {
		pass += other.pass;
		fail += other.fail;
		exception += other.exception;
	}

	public int total() {
		return pass + fail + exception;
	}
}
