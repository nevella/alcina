package cc.alcina.framework.common.client.traversal.layer.overlay;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.util.CountingMap;

/**
 * <p>
 * A simple combined document traversal measurement - tracking various boundary
 * traversals
 * 
 * <p>
 * Note that instances are mutated during traversal, so to reuse a quota for fwd
 * and back, clone() it
 */
public class BoundaryTraversals implements Cloneable {
	public enum Unit {
		character, word, sentence, line, block, segment, document;
	}

	public BoundaryTraversals clone() {
		BoundaryTraversals result = new BoundaryTraversals();
		result.wordsPerLine = wordsPerLine;
		result.counts = new CountingMap<>();
		result.counts.putAll(counts);
		return result;
	}

	public int wordsPerLine;

	transient int wordsTraversedCurrentLine;

	public CountingMap<Unit> counts = new CountingMap<>();

	@Override
	public String toString() {
		return counts.toString();
	}

	public BoundaryTraversals withCount(Unit unit, int count) {
		counts.add(unit, count);
		return this;
	}

	public BoundaryTraversals withWordsPerLine(int wordsPerLine) {
		this.wordsPerLine = wordsPerLine;
		return this;
	}

	/**
	 * 
	 * @param unit
	 *            the unit to decrement
	 * @return false if the unit exists, and its count after decrement is zero
	 */
	public boolean decrement(Unit unit) {
		if (counts.containsKey(Unit.line)) {
			Preconditions.checkState(wordsPerLine != 0);
			// lines are computed
			if (unit == Unit.block) {
				decrement(Unit.line);
				decrement(Unit.line);
			} else if (unit == Unit.word) {
				if (++wordsTraversedCurrentLine == wordsPerLine) {
					wordsTraversedCurrentLine = 0;
					decrement(Unit.line);
				}
			}
			if (counts.get(Unit.line) <= 0) {
				return false;
			}
		}
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
