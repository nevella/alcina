package cc.alcina.framework.common.client.traversal.layer;

import cc.alcina.framework.common.client.process.TreeProcess.HasReleaseableResources;
import cc.alcina.framework.common.client.traversal.AbstractUrlSelection;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.util.HasUrl;

public abstract class AbstractUrlMeasureSelection extends AbstractUrlSelection
		implements HasUrl, HasReleaseableResources {
	private Measure measure;

	public AbstractUrlMeasureSelection(Selection parent, Measure measure,
			String url) {
		super(parent, url);
		this.measure = measure;
	}

	public Measure getMeasure() {
		return this.measure;
	}

	@Override
	public void releaseResources() {
		super.releaseResources();
		this.measure.detach();
	}
}
