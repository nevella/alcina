package cc.alcina.framework.gwt.client.cell.tree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public abstract class FilterPeer<T> {
	public String text = "";

	private Map<T, Boolean> cachedFilter = new HashMap<T, Boolean>();

	protected Predicate<T> additionalFilter;

	public FilterPeer() {
	}

	public FilterPeer(Predicate<T> additionalFilter) {
		this.additionalFilter = additionalFilter;
	}

	public abstract List<T> children(T t);

	public List<T> filter(List<T> elements) {
		cachedFilter.clear();
		return filter0(elements);
	}

	public abstract boolean satisfiesFilter(T t);

	private List<T> filter0(List<T> elements) {
		List<T> result = new ArrayList<T>();
		for (T t : elements) {
			if (cachedFilter.containsKey(t)) {
				if (cachedFilter.get(t)) {
					result.add(t);
				}
			} else {
				if (satisfiesFilter(t)) {
					cachedFilter.put(t, true);
					result.add(t);
				} else {
					boolean matched = filter0(children(t)).size() > 0;
					cachedFilter.put(t, matched);
					if (matched) {
						result.add(t);
					}
				}
			}
		}
		return result;
	}

	public abstract static class BasicFilterPeer<T> extends FilterPeer<T> {
		public BasicFilterPeer(Predicate<T> additionalFilter) {
			super(additionalFilter);
		}

		@Override
		public boolean satisfiesFilter(T t) {
			return additionalFilter.test(t);
		}
	}
}