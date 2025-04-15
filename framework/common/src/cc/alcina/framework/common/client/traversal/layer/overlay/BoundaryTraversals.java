package cc.alcina.framework.common.client.traversal.layer.overlay;

import cc.alcina.framework.common.client.util.CountingMap;

/**
 * A simple combined document traversal measurement - tracking various boundary
 * traversals
 */
public class BoundaryTraversals {
	public enum Unit {
		character, word, sentence, block, segment;
	}

	public CountingMap<Unit> counts = new CountingMap<>();

	@Override
	public String toString() {
		return counts.toString();
	}

	public BoundaryTraversals withCount(Unit unit, int count) {
		counts.add(unit, count);
		return this;
	}

	/**
	 * 
	 * @param unit
	 *            the unit to decrement
	 * @return false if the unit exists, and its count after decrement is zero
	 */
	public boolean decrement(Unit unit) {
		if (!counts.containsKey(unit)) {
			return true;
		}
		return counts.add(unit, -1) != 0;
	}

	public BoundaryTraversals copy() {
		BoundaryTraversals result = new BoundaryTraversals();
		result.counts = counts.clone();
		return result;
	}

	public boolean isExhausted() {
		return counts.values().stream().anyMatch(i -> i == 0);
	}

	public boolean isZero(Unit unit) {
		return counts.containsKey(unit) && counts.get(unit) == 0;
	}

	public int get(Unit unit) {
		return counts.getOrDefault(unit, 0);
	}
}
