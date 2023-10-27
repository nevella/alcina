package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class SelectionFilter extends Model implements TreeSerializable {
	private int allGenerationsLimit = 0;

	private int maxExceptions = 0;

	private List<SelectionFilter.GenerationEntry> generations = new ArrayList<>();

	private transient Map<String, SelectionFilter.GenerationEntry> entriesByGeneration;

	// TODO - change to 'LayerFilter' generation to Class<? extends Layer>
	public GenerationEntry addGenerationFilter(Object generation,
			String pathSegmentRegex) {
		SelectionFilter.GenerationEntry entry = new GenerationEntry(generation,
				pathSegmentRegex);
		generations.add(entry);
		return entry;
	}

	public void copyFrom(SelectionFilter other) {
		allGenerationsLimit = other.allGenerationsLimit;
		generations = other.generations;
		maxExceptions = other.maxExceptions;
		entriesByGeneration = null;
	}

	public int getAllGenerationsLimit() {
		return this.allGenerationsLimit;
	}

	public List<SelectionFilter.GenerationEntry> getGenerations() {
		return this.generations;
	}

	public int getMaxExceptions() {
		return this.maxExceptions;
	}

	public boolean hasGenerationFilter(String generation) {
		return entriesByGeneration.containsKey(generation);
	}

	public boolean matchesGenerationFilter(String generation,
			List<String> list) {
		return entriesByGeneration.get(generation).matches(list);
	}

	public void prepareToFilter() {
		generations.forEach(SelectionFilter.GenerationEntry::prepareToFilter);
		entriesByGeneration = generations.stream().collect(AlcinaCollectors
				.toKeyMap(SelectionFilter.GenerationEntry::getGeneration));
	}

	public boolean provideNotEmpty() {
		return allGenerationsLimit != 0 || generations.size() != 0
				|| maxExceptions != 0;
	}

	public void setAllGenerationsLimit(int allGenerationsLimit) {
		this.allGenerationsLimit = allGenerationsLimit;
	}

	public void
			setGenerations(List<SelectionFilter.GenerationEntry> generations) {
		this.generations = generations;
	}

	public void setMaxExceptions(int maxExceptions) {
		this.maxExceptions = maxExceptions;
	}

	public static class GenerationEntry extends Model
			implements TreeSerializable {
		private String generation;

		private String filterRegex;

		private transient RegExp regexp;

		private transient Logger logger = LoggerFactory.getLogger(getClass());

		private int logCount;

		public GenerationEntry() {
		}

		public GenerationEntry(Object generation, String filterRegex) {
			this.generation = generation.toString();
			this.filterRegex = filterRegex;
		}

		public String getFilterRegex() {
			return this.filterRegex;
		}

		public String getGeneration() {
			return this.generation;
		}

		public int getLogCount() {
			return this.logCount;
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
					logger.info("Generation filter: {} : {} : {}", generation,
							list, result);
				}
			}
			return result;
		}

		public void setFilterRegex(String filterRegex) {
			this.filterRegex = filterRegex;
		}

		public void setGeneration(String generation) {
			this.generation = generation;
		}

		public void setLogCount(int logCount) {
			this.logCount = logCount;
		}

		@Override
		public String toString() {
			return Ax.format("Filter entry :: %s :: %s", generation,
					filterRegex);
		}

		void prepareToFilter() {
			this.regexp = RegExp.compile(filterRegex);
		}
	}
}