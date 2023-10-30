package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedNameProvider;

public class SelectionFilter extends Bindable.Fields
		implements TreeSerializable {
	public int allLayersLimit = 0;

	public int maxExceptions = 0;

	public List<SelectionFilter.SelectionClassEntry> filters = new ArrayList<>();

	private transient Map<Class<? extends Selection>, SelectionFilter.SelectionClassEntry> filtersByClass;

	public SelectionClassEntry addLayerFilter(
			Class<? extends Selection> selectionClass,
			String pathSegmentRegex) {
		SelectionFilter.SelectionClassEntry entry = new SelectionClassEntry(
				selectionClass, pathSegmentRegex);
		filters.add(entry);
		return entry;
	}

	public void copyFrom(SelectionFilter other) {
		allLayersLimit = other.allLayersLimit;
		filters = other.filters;
		maxExceptions = other.maxExceptions;
		filtersByClass = null;
	}

	public boolean
			hasSelectionFilter(Class<? extends Selection> selectionClass) {
		return filtersByClass.containsKey(selectionClass);
	}

	public boolean matchesSelectionTypeFilter(
			Class<? extends Selection> selectionClass, List<String> list) {
		return filtersByClass.get(selectionClass).matches(list);
	}

	public void prepareToFilter() {
		filters.forEach(SelectionFilter.SelectionClassEntry::prepareToFilter);
		filtersByClass = filters.stream().collect(AlcinaCollectors.toKeyMap(
				SelectionFilter.SelectionClassEntry::_selectionClass));
	}

	public boolean provideNotEmpty() {
		return allLayersLimit != 0 || filters.size() != 0 || maxExceptions != 0;
	}

	public static class SelectionClassEntry extends Bindable.Fields
			implements TreeSerializable {
		public Class<? extends Selection> _selectionClass() {
			return selectionClass;
		}

		public String filterRegex;

		private transient RegExp regexp;

		private transient Logger logger = LoggerFactory.getLogger(getClass());

		public int logCount;

		Class<? extends Selection> selectionClass;

		public SelectionClassEntry() {
		}

		public SelectionClassEntry(Class<? extends Selection> selectionClass,
				String filterRegex) {
			this.selectionClass = selectionClass;
			this.filterRegex = filterRegex;
		}

		public boolean matches(List<String> list) {
			boolean result = false;
			for (String string : list) {
				if (regexp.test(string)) {
					result = true;
					break;
				}
			}
			synchronized (this) {
				if (logCount != 0) {
					logCount--;
					logger.info("Selection class filter: {} : {} : {}",
							NestedNameProvider.get(selectionClass), list,
							result);
				}
			}
			return result;
		}

		@Override
		public String toString() {
			return Ax.format("Filter entry :: %s :: %s",
					NestedNameProvider.get(selectionClass), filterRegex);
		}

		void prepareToFilter() {
			this.regexp = RegExp.compile(filterRegex);
		}
	}
}