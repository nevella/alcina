package cc.alcina.framework.gwt.client.tour;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RootPanel;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.tour.StepPopupView.Action;
import cc.alcina.framework.gwt.client.tour.Tour.RelativeTo;
import cc.alcina.framework.gwt.client.tour.Tour.Step;
import cc.alcina.framework.gwt.client.tour.TourManager.UIRenderer;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.OtherPositioningStrategy;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.RelativePopupPositioningParams;
import cc.alcina.framework.gwt.client.util.WidgetUtils;
import cc.alcina.framework.gwt.client.widget.dialog.DecoratedRelativePopupPanel;

public class UIRendererClient extends UIRenderer
		implements NativePreviewHandler {
	protected List<DecoratedRelativePopupPanel> popups = new ArrayList<DecoratedRelativePopupPanel>();

	public HandlerRegistration nativePreviewHandlerRegistration;

	@Override
	public void onPreviewNativeEvent(NativePreviewEvent npe) {
		Event event = Event.as(npe.getNativeEvent());
		EventTarget target = event.getEventTarget();
		if (event.getType().equals(BrowserEvents.KEYDOWN)) {
			char c = (char) event.getKeyCode();
			if (c == KeyCodes.KEY_ESCAPE) {
				tourManager.stepListener.topicPublished(null, Action.CLOSE);
			}
		}
	}

	@Override
	protected void afterStepListenerAction() {
		WidgetUtils.squelchCurrentEvent();
	}

	@Override
	protected void clearPopups(int delay) {
		if (delay != 0) {
			new Timer() {
				@Override
				public void run() {
					clearPopups(0);
				}
			}.schedule(delay);
			return;
		}
		for (DecoratedRelativePopupPanel popupPanel : popups) {
			StepPopupView stepView = (StepPopupView) popupPanel.getWidget();
			stepView.topicPublisher.removeTopicListener(null,
					tourManager.stepListener);
			popupPanel.hide();
		}
		popups.clear();
		if (nativePreviewHandlerRegistration != null) {
			nativePreviewHandlerRegistration.removeHandler();
			nativePreviewHandlerRegistration = null;
		}
	}

	@Override
	protected void exitTour(String message) {
		Registry.impl(ClientNotifications.class).showMessage(message);
	}

	@Override
	protected boolean hasElement(List<String> selectors) {
		return getElement(selectors) != null;
	}

	@Override
	protected boolean performAction(Step step) {
		Tour.Condition targetCondition = step.provideTarget();
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
			Scheduler.get().scheduleDeferred(() -> {
				setPopupsModal(false);
				WidgetUtils.click(target);
				setPopupsModal(true);
			});
			break;
		}
		return true;
	}

	@Override
	protected void publishNext() {
		new Timer() {
			@Override
			public void run() {
				tourManager.stepListener.topicPublished(null, Action.NEXT);
			}
		}.schedule(1000);
	}

	@Override
	protected void render(Step step) {
		clearPopups(step.getDelay());
		int idx = 0;
		for (Tour.PopupInfo popupInfo : step.providePopups()) {
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
					tourManager.currentTour, idx++ == 0);
			stepPopupView.topicPublisher.addTopicListener(null,
					tourManager.stepListener);
			popup.setWidget(stepPopupView);
			int pointerRightMargin = popupInfo.getRelativeTo()
					.getPointerRightMargin();
			if (pointerRightMargin != 0) {
				WidgetUtils
						.getElementForSelector(popup.getElement(),
								".popupBottomCenterInner")
						.getStyle().setMarginRight(pointerRightMargin, Unit.PX);
				WidgetUtils
						.getElementForSelector(popup.getElement(),
								".popupTopCenterInner")
						.getStyle().setMarginRight(pointerRightMargin, Unit.PX);
			}
			popups.add(popup);
		}
	}

	protected void setPopupsModal(boolean modal) {
		for (DecoratedRelativePopupPanel popupPanel : popups) {
			popupPanel.setModal(modal);
		}
	}

	@Override
	protected boolean showStepPopups() {
		for (DecoratedRelativePopupPanel popupPanel : popups) {
			StepPopupView view = (StepPopupView) popupPanel.getWidget();
			Tour.PopupInfo popupInfo = view.popupInfo;
			RelativePopupPositioningParams params = new RelativePopupPositioningParams();
			RelativeTo relativeTo = popupInfo.getRelativeTo();
			params.relativeToElement = WidgetUtils.getElementForSelector(
					Document.get().getDocumentElement(),
					relativeTo.getElement());
			if (params.relativeToElement != null) {
				params.boundingWidget = RootPanel.get();
				params.relativeContainer = RootPanel.get();
				params.shiftX = relativeTo.getOffsetHorizontal();
				params.shiftY = relativeTo.getOffsetVertical();
				params.widgetToShow = view;
				params.addRelativeWidgetHeight = true;
				OtherPositioningStrategy strategy = OtherPositioningStrategy.BELOW_WITH_PREFERRED_LEFT;
				switch (relativeTo.getDirection()) {
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
						.addNativePreviewHandler(this);
			}
		}
		return true;
	}

	@Override
	protected void startTour(TourManager tourManager) {
		this.tourManager = tourManager;
		TourResources res = (TourResources) GWT.create(TourResources.class);
		StyleInjector.inject(res.tourCss().getText());
	}

	Element getElement(List<String> selectors) {
		Element selected = null;
		for (String selector : selectors) {
			selected = WidgetUtils.getElementForSelector(
					Document.get().getDocumentElement(), selector);
			if (selected != null
					&& WidgetUtils.isVisibleAncestorChain(selected)) {
				return selected;
			}
		}
		return null;
	}
}