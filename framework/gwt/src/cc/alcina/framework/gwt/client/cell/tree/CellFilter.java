package cc.alcina.framework.gwt.client.cell.tree;

import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.util.AtEndOfEventSeriesTimer;

public abstract class CellFilter extends Composite implements KeyUpHandler {
	private FlowPanel fp;

	protected TextBox textBox;

	private AtEndOfEventSeriesTimer seriesTimer = new AtEndOfEventSeriesTimer(
			200, new Runnable() {
				@Override
				public void run() {
					afterSeries();
				}
			}).maxDelayFromFirstAction(3000);

	public CellFilter() {
		this.fp = new FlowPanel();
		initWidget(fp);
		this.textBox = new TextBox();
		fp.add(textBox);
		textBox.setWidth("400px");
		textBox.getElement().setPropertyString("placeholder", "Filter index");
		textBox.addStyleName("filter");
		textBox.addKeyUpHandler(this);
	}

	public void focus() {
		textBox.setFocus(true);
	}

	public boolean hasFilteringText() {
		return Ax.notBlank(textBox.getValue());
	}

	@Override
	public void onKeyUp(KeyUpEvent event) {
		seriesTimer.triggerEventOccurred();
	}

	protected abstract void afterSeries();

	@Override
	protected void onAttach() {
		super.onAttach();
		if (shouldFocusOnAttach()) {
			new Timer() {
				@Override
				public void run() {
					focus();
				}
			}.schedule(500);
		}
	}

	protected abstract boolean shouldFocusOnAttach();
}