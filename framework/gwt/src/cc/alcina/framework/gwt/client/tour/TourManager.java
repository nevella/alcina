package cc.alcina.framework.gwt.client.tour;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.RootPanel;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.state.AllStatesConsort;
import cc.alcina.framework.common.client.state.Consort;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.tour.StepPopupView.Action;
import cc.alcina.framework.gwt.client.tour.Tour.Condition;
import cc.alcina.framework.gwt.client.tour.Tour.Operator;
import cc.alcina.framework.gwt.client.tour.Tour.PopupInfo;
import cc.alcina.framework.gwt.client.tour.Tour.RelativeTo;
import cc.alcina.framework.gwt.client.tour.Tour.Step;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.OtherPositioningStrategy;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.RelativePopupPositioningParams;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.dialog.DecoratedRelativePopupPanel;

public abstract class TourManager implements NativePreviewHandler {
	protected List<DecoratedRelativePopupPanel> popups = new ArrayList<DecoratedRelativePopupPanel>();

	protected TourModel currentTour;

	String tourJson = "";

	private DisplayStepConsort consort;

	protected boolean autoplay;

	protected Step step;

	public TopicListener<StepPopupView.Action> stepListener = new TopicListener<StepPopupView.Action>() {
		@Override
		public void topicPublished(String key, Action message) {
			switch (message) {
			case CLOSE:
				if (consort != null) {
					consort.cancel();
				}
				clearPopups();
				break;
			case NEXT:
				currentTour.gotoStep(currentTour.getCurrentStepIndex() + 1);
				refreshTourView();
				break;
			case BACK:
				currentTour.gotoStep(currentTour.getCurrentStepIndex() - 1);
				refreshTourView();
				break;
			}
			WidgetUtils.squelchCurrentEvent();
		}
	};

	public HandlerRegistration nativePreviewHandlerRegistration;

	private AsyncCallback completionCallback;

	protected TourManager() {
		super();
		TourResources res = (TourResources) GWT.create(TourResources.class);
		StyleInjector.inject(res.tourCss().getText());
	}

	@Override
	public void onPreviewNativeEvent(NativePreviewEvent npe) {
		Event event = Event.as(npe.getNativeEvent());
		EventTarget target = event.getEventTarget();
		if (event.getType().equals(BrowserEvents.KEYDOWN)) {
			char c = (char) DOM.eventGetKeyCode(event);
			if (c == KeyCodes.KEY_ESCAPE) {
				stepListener.topicPublished(null, Action.CLOSE);
			}
		}
	}

	public void startTourWithJson(String tourJson, boolean autoplay,
			AsyncCallback completionCallback) {
		this.completionCallback = completionCallback;
		int idx1 = 0;
		this.autoplay = autoplay;
		while (true) {
			idx1 = tourJson.indexOf("/*");
			if (idx1 == -1) {
				break;
			}
			int idx2 = tourJson.indexOf("*/");
			tourJson = tourJson.substring(0, idx1)
					+ tourJson.substring(idx2 + 2);
		}
		this.tourJson = tourJson.replaceFirst("var sample = ", "");
		currentTour = TourModel.fromJson(this.tourJson);
		refreshTourView();
	}

	protected void clearPopups() {
		for (DecoratedRelativePopupPanel popupPanel : popups) {
			StepPopupView stepView = (StepPopupView) popupPanel.getWidget();
			stepView.topicPublisher.removeTopicListener(null, stepListener);
			popupPanel.hide();
		}
		popups.clear();
		if (nativePreviewHandlerRegistration != null) {
			nativePreviewHandlerRegistration.removeHandler();
			nativePreviewHandlerRegistration = null;
		}
	}

	protected void exitTour(String string) {
		Registry.impl(ClientNotifications.class).showMessage(string);
	}

	protected void refreshTourView() {
		if (consort != null) {
			consort.cancel();
		}
		consort = new DisplayStepConsort(null);
		consort.start();
	}

	protected void setPopupsModal(boolean modal) {
		for (DecoratedRelativePopupPanel popupPanel : popups) {
			popupPanel.setModal(modal);
		}
	}

	public static enum DisplayStepPhase {
		SETUP, WAIT_FOR, IGNORE_IF, PERFORM_ACTION, SHOW_POPUP
	}

	class DisplayStepConsort extends AllStatesConsort<DisplayStepPhase> {
		private TopicListener exitListener = new TopicListener() {
			@Override
			public void topicPublished(String key, Object message) {
				if (!Consort.FINISHED.equals(key)) {
					clearPopups();
				}
			}
		};

		public DisplayStepConsort(AsyncCallback callback) {
			super(DisplayStepPhase.class, callback);
			this.timeout = 20000;
			exitListenerDelta(exitListener, false, true);
		}

		@Override
		public void finished() {
			super.finished();
			if (autoplay && currentTour.hasNext()) {
				new Timer() {
					@Override
					public void run() {
						stepListener.topicPublished(null, Action.NEXT);
					}
				}.schedule(1000);
			}
			if (completionCallback != null && autoplay
					&& !currentTour.hasNext()) {
				completionCallback.onSuccess(null);
			}
		}

		@Override
		public void onFailure(Throwable throwable) {
			super.onFailure(throwable);
			finished();
			if (completionCallback != null) {
				completionCallback.onFailure(throwable);
			}
		}

		@Override
		public void runPlayer(AllStatesPlayer player, DisplayStepPhase next) {
			if (!isRunning()) {
				return;
			}
			switch (next) {
			case SETUP:
				render();
				wasPlayed(player);
				break;
			case WAIT_FOR:
				if (waitFor()) {
					wasPlayed(player);
				} else {
					retry(player, next, 200);
				}
				break;
			case IGNORE_IF:
				if (checkIgnore()) {
					finished();
					if (currentTour.hasNext()) {
						stepListener.topicPublished(null, Action.NEXT);
					}
				} else {
					wasPlayed(player);
				}
				break;
			case PERFORM_ACTION:
				if (checkIgnoreAction() || performAction()) {
					wasPlayed(player);
				} else {
					retry(player, next, 200);
				}
				break;
			case SHOW_POPUP:
				if (showStepPopups()) {
					wasPlayed(player);
				} else {
					retry(player, next, 200);
				}
				break;
			}
		}

		/*
		 * return true if we should ignore this step
		 */
		private boolean checkIgnore() {
			Condition ignoreIf = step.getIgnoreIf();
			if (ignoreIf != null) {
				return evaluateCondition(ignoreIf);
			}
			return false;
		}

		private boolean checkIgnoreAction() {
			Condition ignoreActionIf = step.getIgnoreActionIf();
			if (ignoreActionIf != null) {
				return evaluateCondition(ignoreActionIf);
			}
			return false;
		}

		private boolean evaluateCondition(Condition condition) {
			Operator operator = condition.getOperator();
			int conditionCount = 0;
			int passCount = 0;
			for (String selector : condition.getSelectors()) {
				conditionCount++;
				passCount += getElement(
						Collections.singletonList(selector)) != null ? 1 : 0;
			}
			for (Condition child : condition.getConditions()) {
				conditionCount++;
				passCount += evaluateCondition(child) ? 1 : 0;
			}
			switch (operator) {
			case AND:
				return conditionCount == passCount;
			case OR:
				return passCount > 0;
			case NOT:
				return passCount == 0;
			default:
				throw new UnsupportedOperationException();
			}
		}

		private boolean performAction() {
			Condition targetCondition = step.getTarget();
			if (targetCondition == null) {
				return true;
			}
			final Element target = getElement(targetCondition.getSelectors());
			if (target == null) {
				return false;
			}
			switch (step.getAction()) {
			case NONE:
				break;
			case CLICK:
				setPopupsModal(false);
				WidgetUtils.click(target);
				setPopupsModal(true);
				break;
			case SET_TEXT:
				setPopupsModal(false);
				WidgetUtils.click(target);
				target.setPropertyString("value", step.getActionValue());
				setPopupsModal(false);
				WidgetUtils.click(target);
				setPopupsModal(true);
				break;
			}
			return true;
		}

		private void render() {
			clearPopups();
			step = currentTour.getCurrentStep();
			int idx = 0;
			for (PopupInfo popupInfo : step.getPopups()) {
				DecoratedRelativePopupPanel popup = new DecoratedRelativePopupPanel(
						false, idx == 0);
				popup.setStyleName("dropdown-popup tour");
				switch (popupInfo.getRelativeTo().getPointer()) {
				case CENTER_UP:
					popup.arrowCenterUp();
					break;
				case LEFT_UP:
					popup.arrowLeftUp();
					break;
				case RIGHT_UP:
					popup.arrowRightUp();
					break;
				case RIGHT_DOWN:
					popup.arrowRightDown();
					break;
				case CENTER_DOWN:
					popup.arrowCenterDown();
					break;
				}
				StepPopupView stepPopupView = new StepPopupView(popupInfo,
						currentTour, idx++ == 0);
				stepPopupView.topicPublisher.addTopicListener(null,
						stepListener);
				popup.setWidget(stepPopupView);
				int pointerRightMargin = popupInfo.getRelativeTo()
						.getPointerRightMargin();
				if (pointerRightMargin != 0) {
					WidgetUtils
							.getElementForSelector(popup.getElement(),
									".popupBottomCenterInner")
							.getStyle()
							.setMarginRight(pointerRightMargin, Unit.PX);
					WidgetUtils
							.getElementForSelector(popup.getElement(),
									".popupTopCenterInner")
							.getStyle()
							.setMarginRight(pointerRightMargin, Unit.PX);
				}
				popups.add(popup);
			}
		}

		/*
		 * return false if we need to keep waiting
		 */
		private boolean waitFor() {
			Condition waitFor = step.getWaitFor();
			if (waitFor != null) {
				return evaluateCondition(waitFor);
			}
			return true;
		}

		protected boolean showStepPopups() {
			for (DecoratedRelativePopupPanel popupPanel : popups) {
				StepPopupView view = (StepPopupView) popupPanel.getWidget();
				PopupInfo popupInfo = view.popupInfo;
				RelativePopupPositioningParams params = new RelativePopupPositioningParams();
				RelativeTo relativeTo = popupInfo.getRelativeTo();
				params.relativeToElement = WidgetUtils
						.getElementForSelector(null, relativeTo.getElement());
				if (params.relativeToElement != null) {
					params.boundingWidget = RootPanel.get();
					params.relativeContainer = RootPanel.get();
					params.shiftX = relativeTo.getOffsetHorizontal();
					params.shiftY = relativeTo.getOffsetVertical();
					params.widgetToShow = view;
					params.addRelativeWidgetHeight = true;
					OtherPositioningStrategy strategy = OtherPositioningStrategy.BELOW_WITH_PREFERRED_LEFT;
					switch (relativeTo.getPositioningDirection()) {
					case LEFT_BOTTOM:
						break;
					case CENTER_TOP:
						strategy = OtherPositioningStrategy.ABOVE_CENTER;
						break;
					case RIGHT_BOTTOM:
						strategy = OtherPositioningStrategy.BELOW_RIGHT;
					case RIGHT_TOP:
						strategy = OtherPositioningStrategy.ABOVE_RIGHT;
						break;
					}
					params.positioningStrategy = strategy;
					RelativePopupPositioning.showPopup(params, popupPanel);
					int popupFromBottom = relativeTo.getPopupFromBottom();
					WidgetUtils.scrollIntoView(popupPanel.getElement(),
							popupFromBottom);
				}
				if (params.relativeToElement == null) {
					return false;
				}
			}
			if (popups.size() > 0) {
				if (nativePreviewHandlerRegistration == null) {
					nativePreviewHandlerRegistration = Event
							.addNativePreviewHandler(TourManager.this);
				}
			}
			return true;
		}

		@Override
		protected void timedOut(AllStatesPlayer allStatesPlayer,
				DisplayStepPhase state) {
			System.out.println(CommonUtils.formatJ("Timed out - %s - %s",
					currentTour.getCurrentStep(), state));
			super.timedOut(allStatesPlayer, state);
		}

		Element getElement(List<String> selectors) {
			Element selected = null;
			for (String selector : selectors) {
				selected = WidgetUtils.getElementForSelector(null, selector);
				if (selected != null
						&& WidgetUtils.isVisibleAncestorChain(selected)) {
					return selected;
				}
			}
			return null;
		}
	}
}