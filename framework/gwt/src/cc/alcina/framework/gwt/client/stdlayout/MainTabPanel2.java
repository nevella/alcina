package cc.alcina.framework.gwt.client.stdlayout;

import java.util.List;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

// 14 years and change on, reimplement with 'modern' markup/css
/*
 * This class does not inherit from TabPanel, but does implement the necessary
 * interfaces
 * 
 * No, it doesn't use dirndl...but very tempting
 */
public class MainTabPanel2 extends Composite implements IMainTabPanel {
	FlowPanel panel = new FlowPanel();

	class Left extends Composite {
		FlowPanel childPanel = new FlowPanel();

		Left() {
			initWidget(childPanel);
			setStyleName("left");
		}
	}

	class Center extends Composite {
		FlowPanel childPanel = new FlowPanel();

		Center() {
			initWidget(childPanel);
			setStyleName("center");
		}
	}

	class Right extends Composite {
		FlowPanel childPanel = new FlowPanel();

		Right() {
			initWidget(childPanel);
			setStyleName("right");
			nonTabButtons.forEach(childPanel::add);
		}
	}

	Left left;

	Center center;

	Right right;

	List<IsWidget> nonTabButtons;

	public MainTabPanel2(List<IsWidget> nonTabButtons) {
		this.nonTabButtons = nonTabButtons;
		initWidget(panel);
		left = new Left();
		center = new Center();
		right = new Right();
		panel.add(left);
		panel.add(center);
		panel.add(right);
		setStyleName("alcina-MainMenu2");
	}

	@Override
	public Widget getDeckPanel() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getDeckPanel'");
	}

	@Override
	public Widget getWidget(int tabIndex) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getWidget'");
	}

	@Override
	public int getWidgetIndex(Widget widget) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getWidgetIndex'");
	}

	@Override
	public void selectTab(int index) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'selectTab'");
	}

	@Override
	public int getSelectedTab() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getSelectedTab'");
	}

	@Override
	public int adjustClientSize(int clientWidth, int i) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'adjustClientSize'");
	}

	@Override
	public SimplePanel getNoTabContentHolder() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getNoTabContentHolder'");
	}

	@Override
	public int getAdjustHeight() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getAdjustHeight'");
	}

	@Override
	public FlowPanel getToolbarHolder() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getToolbarHolder'");
	}

	@Override
	public void add(Widget w, Widget tabWidget) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("Unimplemented method 'add'");
	}

	@Override
	public int getWidgetIndex(IsWidget w) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getWidgetIndex'");
	}

	@Override
	public void setNotabContent(Widget w) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'setNotabContent'");
	}

	@Override
	public int getTabBarOffsetHeight() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getTabBarOffsetHeight'");
	}

	@Override
	public int getTabBarHeight() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getTabBarHeight'");
	}

	@Override
	public void appendBar(Widget bar) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'appendBar'");
	}

	@Override
	public int getWidgetCount() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException(
				"Unimplemented method 'getWidgetCount'");
	}
}
