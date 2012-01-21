/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.gwt.client.widget;

import cc.alcina.framework.gwt.client.util.WidgetUtils;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;

/**
 * TODO - the "wrappedenterhandler" ClickHandler should be changed to
 * SelectedValueHandler<String>
 * 
 * @author nick@alcina.cc
 * 
 */
public class FilterWidget extends Composite implements KeyUpHandler,
		KeyDownHandler, FocusHandler, BlurHandler {
	private static boolean isArrowDown(int code) {
		switch (code) {
		case OTHER_KEY_DOWN:
		case KeyCodes.KEY_DOWN:
			return true;
		default:
			return false;
		}
	}

	private TextBox textBox;

	private static final int OTHER_KEY_DOWN = 63233;

	private Timer queueingFinishedTimer;

	private VisualFilterable vf;

	private boolean hintWasCleared;

	private boolean focusOnAttach;

	private FlowPanel holder;

	private ClickHandler enterHandler;

	private String lastFilteredText = "";

	private String lastQueuedText = "";
	public static int defaultFilterDelayMs=500;

	public FilterWidget() {
		this(null);
	}

	public FilterWidget(String hint) {
		this.holder = new FlowPanel();
		this.textBox = new TextBox();
		textBox.setStyleName("alcina-Filter");
		textBox.addKeyUpHandler(this);
		textBox.addKeyDownHandler(this);
		textBox.addFocusHandler(this);
		textBox.addBlurHandler(this);
		holder.setStyleName("alcina-FilterHolder");
		FlowPanel holder2 = new FlowPanel();
		holder2.add(textBox);
		holder2.setStyleName("alcina-FilterHolder-pad");
		holder.add(holder2);
		initWidget(holder);
		setHint(hint);
		filterDelayMs = defaultFilterDelayMs;
	}

	private String hint;

	private int filterDelayMs;

	public String getHint() {
		return this.hint;
	}

	public void setHint(String _hint) {
		if (_hint != null) {
			textBox.addStyleName("alcina-FilterHint");
			textBox.setText(_hint);
			if (hint == null) {
				textBox.addFocusHandler(new FocusHandler() {
					public void onFocus(FocusEvent event) {
						if (!hintWasCleared && textBox.getText().equals(hint)) {
							hintWasCleared = true;
							textBox.setText("");
							textBox.removeStyleName("alcina-FilterHint");
							lastFilteredText = textBox.getText();
						}
					}
				});
			}
		}
		this.hint = _hint;
	}

	public void filter() {
		vf.filter(textBox.getText());
		lastFilteredText = textBox.getText();
	}

	public ClickHandler getEnterHandler() {
		return this.enterHandler;
	}

	public FlowPanel getHolder() {
		return this.holder;
	}

	public TextBox getTextBox() {
		return this.textBox;
	}

	public boolean isFilterCurrent() {
		return textBox.getText().equals(lastFilteredText);
	}

	public boolean isFocusOnAttach() {
		return focusOnAttach;
	}

	public boolean isHintWasCleared() {
		return this.hintWasCleared;
	}

	public void onKeyDown(KeyDownEvent event) {
		int keyCode = event.getNativeKeyCode();
		if (keyCode == KeyCodes.KEY_ENTER) {
			Event.getCurrentEvent().preventDefault();
			if (enterHandler != null) {
				WidgetUtils.fireClickOnHandler(
						(HasClickHandlers) event.getSource(), enterHandler);
			}
			return;
		}
	}

	public void onKeyUp(KeyUpEvent event) {
		int keyCode = event.getNativeKeyCode();
		if (keyCode == KeyCodes.KEY_ENTER || keyCode == KeyCodes.KEY_UP
				|| keyCode == KeyCodes.KEY_DOWN) {
			// this should probably be handled by having the vfwf add a
			// listener...but
			if (isArrowDown((int) keyCode) && vf != null
					&& vf instanceof VisualFilterable.VisualFilterableWithFirst) {
				((VisualFilterable.VisualFilterableWithFirst) vf).moveToFirst();
			}
			Event.getCurrentEvent().preventDefault();
			return;
		}
		queueCommit();
	}

	protected void queueCommit() {
		lastQueueAddMillis = System.currentTimeMillis();
		lastQueuedText = getTextBox().getText();
		if (queueingFinishedTimer == null) {
			queueingFinishedTimer = new Timer() {
				long timerAddedMillis = lastQueueAddMillis;

				@Override
				public void run() {
					if (lastQueueAddMillis - timerAddedMillis == 0) {
						commit();
					}
					timerAddedMillis = lastQueueAddMillis;
				}
			};
			queueingFinishedTimer.scheduleRepeating(filterDelayMs);
		}
	}

	private long lastQueueAddMillis;

	public boolean isQueueing() {
		return queueingFinishedTimer != null;
	}

	private void commit() {
		if (queueingFinishedTimer != null) {
			queueingFinishedTimer.cancel();
		}
		queueingFinishedTimer = null;
		filter();
	}

	public void registerFilterable(VisualFilterable vf) {
		this.vf = vf;
	}

	public void setEnterHandler(ClickHandler enterListener) {
		this.enterHandler = enterListener;
	}

	public void setFocusOnAttach(boolean focusOnAttach) {
		this.focusOnAttach = focusOnAttach;
	}

	@Override
	protected void onAttach() {
		super.onAttach();
		if (isFocusOnAttach()
				&& WidgetUtils.getParentWidget(this, "GridForm") == null) {
			// just in case this widget is inside a popup panel e.g.
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				@Override
				public void execute() {
					textBox.setFocus(true);
				}
			});
		}
	}

	@Override
	protected void onDetach() {
		if (queueingFinishedTimer != null) {
			queueingFinishedTimer.cancel();
		}
		changeListenerTimer.cancel();
		super.onDetach();
	}

	public void clear() {
		getTextBox().setText("");
	}

	public int getFilterDelayMs() {
		return this.filterDelayMs;
	}

	public void setFilterDelayMs(int filterDelayMs) {
		this.filterDelayMs = filterDelayMs;
	}

	private Timer changeListenerTimer = new Timer() {
		@Override
		public void run() {
			if (!getTextBox().getText().equals(lastQueuedText)) {
				queueCommit();
			}
		}
	};

	@Override
	public void onFocus(FocusEvent event) {
		changeListenerTimer.scheduleRepeating(100);
	}

	@Override
	public void onBlur(BlurEvent event) {
		changeListenerTimer.cancel();
	}
}
