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

public class SelectionFilter extends Bindable.Fields
		implements TreeSerializable {
	public int allLayersLimit = 0;

	public int maxExceptions = 0;

	public List<SelectionFilter.LayerEntry> layers = new ArrayList<>();

	private transient Map<String, SelectionFilter.LayerEntry> entriesByLayer;

	public LayerEntry addLayerFilter(Class<? extends Layer> layer,
			String pathSegmentRegex) {
		SelectionFilter.LayerEntry entry = new LayerEntry(layer,
				pathSegmentRegex);
		layers.add(entry);
		return entry;
	}

	public void copyFrom(SelectionFilter other) {
		allLayersLimit = other.allLayersLimit;
		layers = other.layers;
		maxExceptions = other.maxExceptions;
		entriesByLayer = null;
	}

	public boolean hasLayerFilter(String layer) {
		return entriesByLayer.containsKey(layer);
	}

	public boolean matchesLayerFilter(String layer, List<String> list) {
		return entriesByLayer.get(layer).matches(list);
	}

	public void prepareToFilter() {
		layers.forEach(SelectionFilter.LayerEntry::prepareToFilter);
		entriesByLayer = layers.stream().collect(
				AlcinaCollectors.toKeyMap(SelectionFilter.LayerEntry::_layer));
	}

	public boolean provideNotEmpty() {
		return allLayersLimit != 0 || layers.size() != 0 || maxExceptions != 0;
	}

	public static class LayerEntry extends Bindable.Fields
			implements TreeSerializable {
		public String layer;

		public String _layer() {
			return layer;
		}

		public String filterRegex;

		private transient RegExp regexp;

		private transient Logger logger = LoggerFactory.getLogger(getClass());

		public int logCount;

		public LayerEntry() {
		}

		public LayerEntry(Class<? extends Layer> layer, String filterRegex) {
			this.layer = layer.toString();
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
					logger.info("Layer filter: {} : {} : {}", layer, list,
							result);
				}
			}
			return result;
		}

		@Override
		public String toString() {
			return Ax.format("Filter entry :: %s :: %s", layer, filterRegex);
		}

		void prepareToFilter() {
			this.regexp = RegExp.compile(filterRegex);
		}
	}
}