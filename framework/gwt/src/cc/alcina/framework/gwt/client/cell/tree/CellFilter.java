package cc.alcina.framework.gwt.client.cell.tree;

import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.util.EventCollator;

public abstract class CellFilter extends Composite implements KeyUpHandler {
	private FlowPanel fp;

	protected TextBox textBox;

	private EventCollator seriesTimer = new EventCollator(200, new Runnable() {
		@Override
		public void run() {
			afterSeries();
		}
	}).withMaxDelayFromFirstEvent(3000);

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

	protected abstract void afterSeries();

	public void focus() {
		textBox.setFocus(true);
	}

	public boolean hasFilteringText() {
		return Ax.notBlank(textBox.getValue());
	}

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

	@Override
	public void onKeyUp(KeyUpEvent event) {
		seriesTimer.eventOccurred();
	}

	protected abstract boolean shouldFocusOnAttach();
}