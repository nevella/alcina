package cc.alcina.framework.entity.registry;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.util.Ax;

public class ClassMetadataCache<T extends ClassMetadata>
		implements Serializable {
	public Map<String, T> classData = new LinkedHashMap<String, T>();

	public List<String> ignorePackageSegments = new ArrayList<String>();

	public String dump() {
		return toString() + "\n"
				+ classData.keySet().stream().collect(Collectors.joining("\n"));
	}

	public void insert(T item) {
		for (String segment : ignorePackageSegments) {
			if (item.className.startsWith(segment)) {
				return;
			}
		}
		classData.put(item.className, item);
	}

	public void merge(ClassMetadataCache other) {
		classData.putAll(other.classData);
	}

	@Override
	public String toString() {
		return Ax.format("%s classes", classData.size());
	}
}