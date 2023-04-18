package cc.alcina.framework.common.client.traversal.layer;

import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.layer.Measure.Token;

public class MeasureSelection extends AbstractSelection<Measure>
		implements Comparable<MeasureSelection> {
	private boolean omit;

	public MeasureSelection(Selection parent, Measure measure) {
		super(parent, measure, measure.toString());
	}

	public MeasureSelection(Selection parent, Measure measure,
			String pathSegment) {
		super(parent, measure, pathSegment);
	}

	@Override
	public int compareTo(MeasureSelection o) {
		return get().compareTo(o.get());
	}

	public boolean contains(MeasureSelection o, Token.Order order) {
		return get().contains(o.get(), order);
	}

	/*
	 * Omit from output (and traversal)
	 */
	public boolean isOmit() {
		return this.omit;
	}

	public void setOmit(boolean omit) {
		this.omit = omit;
	}
}