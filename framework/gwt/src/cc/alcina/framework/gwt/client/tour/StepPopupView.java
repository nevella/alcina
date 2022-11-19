package cc.alcina.framework.gwt.client.tour;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;

import cc.alcina.framework.common.client.actions.InlineButtonHandler;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.ide.widget.Toolbar.ToolbarButton;

public class StepPopupView extends Composite {
	private FlowPanel fp;

	Topic<Action> topicAction = Topic.create();

	Tour.PopupInfo popupInfo;

	public StepPopupView(Tour.PopupInfo popupInfo, TourState currentTour,
			boolean withButtons) {
		this.popupInfo = popupInfo;
		this.fp = new FlowPanel();
		initWidget(fp);
		setStyleName("step");
		FlowPanel caption = new FlowPanel();
		caption.setStyleName("caption");
		caption.add(new Label(popupInfo.getCaption()));
		fp.add(caption);
		HTML contents = new HTML();
		contents.setStyleName("contents");
		contents.setHTML(popupInfo.getDescription());
		fp.add(contents);
		if (withButtons) {
			FlowPanel buttons = new FlowPanel();
			buttons.setStyleName("buttons");
			if (currentTour.hasPrevious()) {
				buttons.add(new ToolbarButton(new Back()));
			}
			if (currentTour.hasNext()) {
				buttons.add(new ToolbarButton(new Next()));
			} else {
				buttons.add(new ToolbarButton(new Close()));
			}
			fp.add(buttons);
		}
		ToolbarButton w = new ToolbarButton(new CloseX());
		w.setStyleName("close-x link-no-underline");
		fp.add(w);
	}

	enum Action {
		NEXT, CLOSE, BACK;
	}

	class Back extends InlineButtonHandler {
		@Override
		public String getActionName() {
			return "Back";
		}

		@Override
		public void onClick(ClickEvent event) {
			topicAction.publish(Action.BACK);
		}
	}

	class Close extends InlineButtonHandler {
		@Override
		public String getActionName() {
			return "Close";
		}

		@Override
		public void onClick(ClickEvent event) {
			topicAction.publish(Action.CLOSE);
		}
	}

	class CloseX extends InlineButtonHandler {
		@Override
		public String getActionName() {
			return "X";
		}

		@Override
		public void onClick(ClickEvent event) {
			topicAction.publish(Action.CLOSE);
		}
	}

	class Next extends InlineButtonHandler {
		@Override
		public String getActionName() {
			return "Next";
		}

		@Override
		public void onClick(ClickEvent event) {
			topicAction.publish(Action.NEXT);
		}
	}
}
