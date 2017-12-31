package cc.alcina.framework.gwt.client.ide.widget;

import java.util.Iterator;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.gwt.client.widget.DivStackPanel;
import cc.alcina.framework.gwt.client.widget.layout.HasLayoutInfo;
import cc.alcina.framework.gwt.client.widget.layout.LayoutEvents;

public class StackPanel100pcHeight extends DivStackPanel
		implements HasLayoutInfo {
	public LayoutInfo getLayoutInfo() {
		return new LayoutInfo() {
			@Override
			public int getClientAdjustHeight() {
				// int captionHeight = getElement().getFirstChildElement()
				// .getFirstChildElement().getOffsetHeight();
				// divstack impl below - above is table
				int captionHeight = getElement().getFirstChildElement()
						.getOffsetHeight();
				return getWidgetCount() * captionHeight + 1;// 1==bottom-border
			}

			@Override
			public Iterator<Widget> getLayoutWidgets() {
				return iterator();
			}

			@Override
			public boolean to100percentOfAvailableHeight() {
				return true;
			}

			@Override
			public boolean useBestOffsetForParentHeight() {
				return false;
			}
		};
	}

	@Override
	public void showStack(int index) {
		int oldIndex = getSelectedIndex();
		super.showStack(index);
		if (oldIndex != index) {
			LayoutEvents.get().fireDeferredGlobalRelayoutIfNoneQueued();
		}
	}
}