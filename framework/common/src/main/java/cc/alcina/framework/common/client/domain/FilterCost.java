package cc.alcina.framework.common.client.domain;

public interface FilterCost {
	public static FilterCost evaluatorProjectionCost() {
		return new NaiveCost(NaiveOrdering.Stream_evaluator);
	}

	public static FilterCost lookupProjectionCost() {
		return new NaiveCost(NaiveOrdering.Indexed_lookup);
	}

	public static FilterCost multikeyProjectionCost() {
		return new NaiveCost(NaiveOrdering.Complex_projection);
	}

	public static FilterCost trieProjectionCost() {
		return new NaiveCost(NaiveOrdering.Complex_projection);
	}

	/*
	 * See StringTrieProjection; DomainLookup - for stream_evaluator you'd need
	 * a stats system
	 */
	public double estimatedMatchFraction();

	public NaiveOrdering naiveOrdering();

	/*
	 * 0 for projection/lookup, 1 for evaluator
	 */
	public double perIncomingEntityCost();

	/*
	 * 1 for trie projections, fractional for multikey projections, 0 for
	 * evaluators
	 */
	public double perOutgoingEntityCost();

	public interface HasFilterCost {
		public FilterCost estimateFilterCost(int entityCount,
				DomainFilter... filters);
	}

	public static class NaiveCost implements FilterCost {
		private NaiveOrdering ordering;

		public NaiveCost(NaiveOrdering ordering) {
			this.ordering = ordering;
		}

		@Override
		public double estimatedMatchFraction() {
			return 0;
		}

		@Override
		public NaiveOrdering naiveOrdering() {
			return ordering;
		}

		@Override
		public double perIncomingEntityCost() {
			return 0;
		}

		@Override
		public double perOutgoingEntityCost() {
			return 0;
		}
	}

	/*
	 * Naive planner just orders by ordinal
	 */
	public enum NaiveOrdering {
		/*
		 * multi-key or trie
		 */
		Complex_projection,
		/*
		 * single-field lookup, e.g. user.email
		 */
		Indexed_lookup,
		/*
		 * directly evaluate predicate against object stream
		 */
		Stream_evaluator
	}
}