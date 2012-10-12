package cc.alcina.framework.gwt.client.widget.dialog;

import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImageProvider;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.OtherPositioningStrategy;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.RelativePopupAxis;
import cc.alcina.framework.gwt.client.util.RelativePopupPositioning.RelativePopupPositioningParams;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class BubbleInfoPopupDisplayer {
	private Widget toShow;

	private Widget relativeTo;

	private int preFade = 2000;

	private int fade = 1000;
	
	private double initialOpacity=1.0;

	private RelativePopupPanel popup;

	public BubbleInfoPopupDisplayer() {
	}

	public BubbleInfoPopupDisplayer widgetToShow(Widget toShow) {
		this.toShow = toShow;
		return this;
	}

	public BubbleInfoPopupDisplayer relativeTo(Widget relativeTo) {
		this.relativeTo = relativeTo;
		return this;
	}
	public BubbleInfoPopupDisplayer initialOpacity(double initialOpacity) {
		this.initialOpacity=initialOpacity;
		return this;
	}
	public BubbleInfoPopupDisplayer preFade(int preFade) {
		this.preFade=preFade;
		return this;
	}

	public void show() {
		popup = new RelativePopupPanel(false);
		popup.addStyleName("bubble");
		RelativePopupPositioningParams params = new RelativePopupPositioningParams();
		RelativePopupPositioning
				.showPopup(
						relativeTo,
						toShow,
						RootPanel.get(),
						new RelativePopupAxis[] { RelativePopupPositioning.BOTTOM_LTR },
						null, popup, -50, 0);
		AbstractImagePrototype aip = AbstractImagePrototype
				.create(StandardDataImageProvider.get().getDataImages()
						.bubbleArrow());
		DivElement div = Document.get().createDivElement();
		div.setClassName("bubble-up");
		div.setInnerHTML(aip.getHTML());
		popup.getElement().appendChild(div);
		Style style = div.getStyle();
		int absoluteLeft = relativeTo.getAbsoluteLeft();
		style.setLeft(
				absoluteLeft - popup.getAbsoluteLeft()
						+ (relativeTo.getOffsetWidth() / 2) - 7, Unit.PX);
		WidgetUtils.setOpacity(popup, (int) (100 *initialOpacity));
		Animation fadeAnimation = new Animation() {
			@Override
			protected void onComplete() {
				super.onComplete();
				popup.hide();
			}

			@Override
			protected void onUpdate(double progress) {
				double fadeQ = fade == 0 ? 0
						: ((preFade + fade) * progress - preFade)
								/ ((double) fade);
				if (fadeQ > 0) {
					WidgetUtils.setOpacity(popup, (int) (100*initialOpacity*(1.0-fadeQ)));
				}
			}
		};
		fadeAnimation.run(preFade + fade);
	}
}
