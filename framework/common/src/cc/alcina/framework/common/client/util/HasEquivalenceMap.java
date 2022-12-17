package cc.alcina.framework.common.client.util;

import java.util.List;
import java.util.stream.Collectors;

public class HasEquivalenceMap<T extends HasEquivalence>
		extends Multimap<Integer, List<T>> {
	public HasEquivalenceMap() {
		super();
	}

	public HasEquivalenceMap(List<T> values) {
		super();
		values.forEach(this::add);
	}

	public void add(T value) {
		add(value.equivalenceHash(), value);
	}

	public void addUnique(T value) {
		if (!containsHehValue(value)) {
			add(value);
		}
	}

	public boolean containsHehValue(T value) {
		int hash = value.equivalenceHash();
		List<T> list = get(hash);
		if (list == null) {
			return false;
		}
		return list.stream().anyMatch(t -> t.equivalentTo(value));
	}

	public List<T> getEquivalents(T value) {
		return get(value.equivalenceHash()).stream()
				.filter(v -> v.equivalentTo(value))
				.collect(Collectors.toList());
	}
}