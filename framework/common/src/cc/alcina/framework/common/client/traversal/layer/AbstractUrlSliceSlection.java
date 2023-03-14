package cc.alcina.framework.common.client.traversal.layer;

import cc.alcina.framework.common.client.traversal.AbstractUrlSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.HasUrl;

public abstract class AbstractUrlSliceSlection extends AbstractUrlSelection
		implements HasUrl {
	private final Slice slice;

	public AbstractUrlSliceSlection(Selection parent, Slice slice, String url) {
		super(parent, url, url);
		this.slice = slice;
	}

	public Slice getSlice() {
		return this.slice;
	}
}
