package cc.alcina.framework.gwt.client.entity.view;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;

public class TwoPanelLayout extends Composite {
	private FlowPanel fp;

	public SimplePanel right;

	public SimplePanel left;

	public TwoPanelLayout() {
		this.fp = new FlowPanel();
		this.left = new SimplePanel();
		this.left.setStyleName("leftPanel");
		this.right = new SimplePanel();
		this.right.setStyleName("rightPanel");
		initWidget(fp);
		fp.add(left);
		fp.add(right);
		setStyleName("alcina-TwoPaneLayout");
	}

	public void setRightVisible(boolean visible) {
		right.setVisible(visible);
		setStyleName("onePanelHidden", !visible);
	}
}