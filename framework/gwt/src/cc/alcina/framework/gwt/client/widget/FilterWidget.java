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

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

/**
 * TODO - the "wrappedenterhandler" ClickHandler should be changed to
 * SelectedValueHandler<String>
 * 
 * @author nick@alcina.cc
 * 
 */
public class FilterWidget extends Composite
		implements KeyUpHandler, KeyDownHandler, BlurHandler, ClickHandler {
	private static final String ALCINA_FILTER_HINT = "alcina-FilterHint";

	private static final int OTHER_KEY_DOWN = 63233;

	public static int defaultFilterDelayMs = 500;

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

	private Timer queueingFinishedTimer;

	private VisualFilterable vf;

	private boolean hintWasCleared;

	private boolean focusOnAttach;

	private FlowPanel holder;

	private ClickHandler enterHandler;

	private String lastFilteredText = "";

	private String lastQueuedText = "";

	private String hint;

	private int filterDelayMs;

	private long lastQueueAddMillis;

	private Timer changeListenerTimer = new Timer() {
		@Override
		public void run() {
			maybeCommit();
		}
	};

	private int initialCursorPos;

	List<HandlerRegistration> registrations = new ArrayList<>();

	private String lastText = "";

	public FilterWidget() {
		this(null);
	}

	public FilterWidget(String hint) {
		this.holder = new FlowPanel();
		this.textBox = new TextBox();
		textBox.setStyleName("alcina-Filter");
		textBox.getElement().setAttribute("autocomplete", "off");
		holder.setStyleName("alcina-FilterHolder");
		FlowPanel holder2 = new FlowPanel();
		holder2.add(textBox);
		holder2.setStyleName("alcina-FilterHolder-pad");
		holder.add(holder2);
		initWidget(holder);
		setHint(hint);
		filterDelayMs = defaultFilterDelayMs;
	}

	public void clear() {
		getTextBox().setText("");
		maybeCommit();
	}

	public void filter() {
		vf.filter(textBox.getText());
		lastFilteredText = textBox.getText();
	}

	public ClickHandler getEnterHandler() {
		return this.enterHandler;
	}

	public int getFilterDelayMs() {
		return this.filterDelayMs;
	}

	public String getHint() {
		return this.hint;
	}

	public FlowPanel getHolder() {
		return this.holder;
	}

	public int getInitialCursorPos() {
		return this.initialCursorPos;
	}

	public String getLastText() {
		return this.lastText;
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

	public boolean isHinted() {
		return hint != null && textBox.getText().equals(hint);
	}

	public boolean isHintWasCleared() {
		return this.hintWasCleared;
	}

	public boolean isQueueing() {
		return queueingFinishedTimer != null;
	}

	@Override
	public void onBlur(BlurEvent event) {
		changeListenerTimer.cancel();
	}

	@Override
	public void onClick(ClickEvent event) {
		clearHint();
		onFocus(null);
		textBox.setFocus(true);
	}

	/*
	 * Note - deliberately don't listen to (non-user-initiated) focus events -
	 * capture ones we're into via click, keyup
	 */
	public void onFocus(FocusEvent event) {
		String filterText = getTextBox().getText();
		if (!isFilterCurrent()
				&& !getTextBox().getStyleName().contains(ALCINA_FILTER_HINT)) {
			commit();
		}
		changeListenerTimer.scheduleRepeating(100);
	}

	@Override
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

	@Override
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

	public void registerFilterable(VisualFilterable vf) {
		this.vf = vf;
	}

	public void saveLastText() {
		lastText = getTextBox().getText();
	}

	public void setEnterHandler(ClickHandler enterListener) {
		this.enterHandler = enterListener;
	}

	public void setFilterDelayMs(int filterDelayMs) {
		this.filterDelayMs = filterDelayMs;
	}

	public void setFocusOnAttach(boolean focusOnAttach) {
		this.focusOnAttach = focusOnAttach;
	}

	public void setHint(String _hint) {
		if (_hint != null && CommonUtils.isNullOrEmpty(textBox.getText())) {
			textBox.addStyleName(ALCINA_FILTER_HINT);
			textBox.setText(_hint);
			hintWasCleared = false;
			if (hint == null) {
				HintHandler handler = new HintHandler();
				textBox.addFocusHandler(handler);
				textBox.addKeyDownHandler(handler);
				textBox.addMouseDownHandler(handler);
				textBox.addChangeHandler(handler);
			}
		}
		this.hint = _hint;
	}

	public void setInitialCursorPos(int initialCursorPos) {
		this.initialCursorPos = initialCursorPos;
	}

	public void setValue(String value) {
		textBox.setText(value);
		clearHint();
		if (WidgetUtils.isVisibleAncestorChain(textBox)) {
			textBox.setCursorPos(initialCursorPos);
		} else {
			OneOffHandler oneOff = new OneOffHandler(
					() -> textBox.setCursorPos(initialCursorPos));
			oneOff.register(addAttachHandler(e -> oneOff.run()));
		}
	}

	private void clearHint() {
		if (!hintWasCleared) {
			hintWasCleared = true;
			if (isHinted()) {
				textBox.setText("");
				lastFilteredText = textBox.getText();
			}
			textBox.removeStyleName(ALCINA_FILTER_HINT);
		}
	}

	private void commit() {
		if (queueingFinishedTimer != null) {
			queueingFinishedTimer.cancel();
		}
		queueingFinishedTimer = null;
		filter();
	}

	protected void maybeCommit() {
		String currentText = getTextBox().getText();
		if (CommonUtils.isNotNullOrEmpty(currentText)
				&& !currentText.equals(lastQueuedText)
				&& !getTextBox().getStyleName().contains(ALCINA_FILTER_HINT)) {
			queueCommit();
		}
	}

	@Override
	protected void onAttach() {
		super.onAttach();
		// FIXME - localdom - attach/detach issue with popups meant was
		// attaching to wrong element
		registrations.add(textBox.addKeyUpHandler(this));
		registrations.add(textBox.addKeyDownHandler(this));
		registrations.add(textBox.addBlurHandler(this));
		registrations.add(textBox.addClickHandler(this));
		if (isFocusOnAttach()
				&& WidgetUtils.getParentWidget(this, "GridForm") == null) {
			// just in case this widget is inside a popup panel e.g.
			Scheduler.get().scheduleDeferred(new ScheduledCommand() {
				@Override
				public void execute() {
					String text = textBox.getText();
					if (CommonUtils.isNotNullOrEmpty(text) && !isHinted()) {
						textBox.setCursorPos(text.length());
					}
					textBox.setFocus(true);
					if (!isHinted()) {
						commit();
					}
				}
			});
		}
	}

	@Override
	protected void onDetach() {
		if (queueingFinishedTimer != null) {
			queueingFinishedTimer.cancel();
			queueingFinishedTimer = null;
		}
		changeListenerTimer.cancel();
		registrations.forEach(HandlerRegistration::removeHandler);
		super.onDetach();
	}

	protected void queueCommit() {
		String filterText = getTextBox().getText();
		if (CommonUtils.isNullOrEmpty(lastQueuedText)
				&& CommonUtils.isNullOrEmpty(filterText)) {
			return;
		}
		lastQueueAddMillis = System.currentTimeMillis();
		lastQueuedText = filterText;
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

	private class HintHandler implements FocusHandler, KeyDownHandler,
			MouseDownHandler, ChangeHandler {
		boolean wasFocussed = false;

		@Override
		public void onChange(ChangeEvent event) {
			clearHint();
		}

		@Override
		public void onFocus(FocusEvent event) {
			if (focusOnAttach && !wasFocussed) {
				wasFocussed = true;
				textBox.setCursorPos(0);
				return;
			}
			clearHint();
		}

		@Override
		public void onKeyDown(KeyDownEvent event) {
			clearHint();
		}

		@Override
		public void onMouseDown(MouseDownEvent event) {
			clearHint();
		}
	}
}
