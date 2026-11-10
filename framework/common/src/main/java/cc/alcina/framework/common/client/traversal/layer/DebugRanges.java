package cc.alcina.framework.common.client.traversal.layer;

import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.IntPair;

/* Restrict a parser to (possibly multiple) ranges */
public class DebugRanges {
	public static DebugRanges of(String configString) {
		return new DebugRanges(configString);
	}

	List<IntPair> ranges = null;

	DebugRanges(String configString) {
		if (Ax.isBlank(configString)) {
			return;
		}
		ranges = Arrays.stream(configString.split(";"))
				.map(IntPair::parseIntPair).map(IntPair::toMinimalNonPoint)
				.toList();
	}

	public boolean isFiltered(IntPair range) {
		if (ranges == null) {
			return false;
		}
		return !isDebug(range);
	}

	public boolean isDebug(IntPair range) {
		if (ranges == null) {
			return false;
		}
		return IntPair.intersection(ranges, range.toMinimalNonPoint()) != null;
	}
}
