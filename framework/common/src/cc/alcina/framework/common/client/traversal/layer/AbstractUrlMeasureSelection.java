package cc.alcina.framework.common.client.traversal.layer;

import cc.alcina.framework.common.client.traversal.AbstractUrlSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.HasUrl;

public abstract class AbstractUrlMeasureSelection extends AbstractUrlSelection
		implements HasUrl {
	private final Measure measure;

	public AbstractUrlMeasureSelection(Selection parent, Measure measure,
			String url) {
		super(parent, url, url);
		this.measure = measure;
	}

	public Measure getMeasure() {
		return this.measure;
	}
}
