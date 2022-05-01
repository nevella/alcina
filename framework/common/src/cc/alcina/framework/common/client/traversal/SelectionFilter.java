package cc.alcina.framework.common.client.traversal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gwt.regexp.shared.RegExp;

import cc.alcina.framework.common.client.serializer.PropertySerialization;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class SelectionFilter extends Model implements TreeSerializable {
	private int allGenerationsLimit = 0;

	private int maxExceptions = 0;

	private List<SelectionFilter.GenerationEntry> generations = new ArrayList<>();

	private transient Map<String, RegExp> entriesByGeneration;

	public void addGenerationFilter(Object generation,
			String pathSegmentRegex) {
		SelectionFilter.GenerationEntry entry = new GenerationEntry(generation,
				pathSegmentRegex);
		generations.add(entry);
	}

	public int getAllGenerationsLimit() {
		return this.allGenerationsLimit;
	}

	@PropertySerialization(types = SelectionFilter.GenerationEntry.class)
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
		return list.stream().anyMatch(filterable -> entriesByGeneration
				.get(generation).exec(filterable) != null);
	}

	public void prepareToFilter() {
		entriesByGeneration = generations.stream()
				.collect(Collectors.toMap(GenerationEntry::getGeneration,
						e -> RegExp.compile(e.getPathSegmentRegex())));
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

		private String pathSegmentRegex;

		public GenerationEntry() {
		}

		public GenerationEntry(Object generation, String pathSegmentRegex) {
			this.generation = generation.toString();
			this.pathSegmentRegex = pathSegmentRegex;
		}

		public String getGeneration() {
			return this.generation;
		}

		public String getPathSegmentRegex() {
			return this.pathSegmentRegex;
		}

		public void setGeneration(String generation) {
			this.generation = generation;
		}

		public void setPathSegmentRegex(String pathSegmentRegex) {
			this.pathSegmentRegex = pathSegmentRegex;
		}
	}
}