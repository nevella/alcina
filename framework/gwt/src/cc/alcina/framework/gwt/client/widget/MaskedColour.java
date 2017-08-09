package cc.alcina.framework.gwt.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.stdlayout.image.StandardDataImages;

public class MaskedColour extends Composite {
	public MaskedColour(String cssColorOrRule, Image maskImage,
			OuterPixels margin, int colorPixelWidth) {
		FlowPanel holder = new FlowPanel();
		Style holderStyle = holder.getElement().getStyle();
		margin.marginalise(holder);
		holderStyle.setDisplay(Display.BLOCK);
		holderStyle.setProperty("lineHeight", maskImage.getHeight()
				+ margin.top + margin.bottom, Unit.PX);
		if (colorPixelWidth != 0) {
			holder.add(maskImage);
			FlowPanel cp = new FlowPanel();
			Style cStyle = cp.getElement().getStyle();
			if (cssColorOrRule.startsWith("#")) {
				cStyle.setBackgroundColor(cssColorOrRule);
			} else {
				cp.setStyleName(cssColorOrRule);
			}
			cStyle.setWidth(colorPixelWidth, Unit.PX);
			cStyle.setHeight(maskImage.getHeight(), Unit.PX);
			cStyle.setProperty("lineHeight", maskImage.getHeight(), Unit.PX);
			Image spacer = AbstractImagePrototype.create(
					((StandardDataImages) GWT.create(StandardDataImages.class))
							.transparent()).createImage();
			Style spacerStyle = spacer.getElement().getStyle();
			spacerStyle.setWidth(colorPixelWidth, Unit.PX);
			spacerStyle.setHeight(maskImage.getHeight(), Unit.PX);
			cp.add(spacer);
			holder.add(cp);
			holderStyle.setPosition(Position.RELATIVE);
			Style maskStyle = maskImage.getElement().getStyle();
			maskStyle.setPosition(Position.ABSOLUTE);
			maskStyle.setLeft(0, Unit.PX);
			maskStyle.setTop(0, Unit.PX);
			// maskStyle.setZIndex(1);
		} else {
			holder.add(maskImage);
			if (cssColorOrRule.startsWith("#")) {
				holderStyle.setBackgroundColor(cssColorOrRule);
			} else {
				holder.setStyleName(cssColorOrRule);
			}
		}
		initWidget(holder);
	}

	public static class OuterPixels {
		public static final OuterPixels ZERO = new OuterPixels(0, 0, 0, 0);

		public final int top;

		public final int right;

		public final int bottom;

		public final int left;

		public OuterPixels(int top, int right, int bottom, int left) {
			this.top = top;
			this.right = right;
			this.bottom = bottom;
			this.left = left;
		}

		public void marginalise(Widget widget) {
			Style style = widget.getElement().getStyle();
			style.setMarginTop(top, Unit.PX);
			style.setMarginRight(right, Unit.PX);
			style.setMarginBottom(bottom, Unit.PX);
			style.setMarginLeft(left, Unit.PX);
		}

		public void pad(Widget widget) {
			Style style = widget.getElement().getStyle();
			style.setPaddingTop(top, Unit.PX);
			style.setPaddingRight(right, Unit.PX);
			style.setPaddingBottom(bottom, Unit.PX);
			style.setPaddingLeft(left, Unit.PX);
		}
	}
}
