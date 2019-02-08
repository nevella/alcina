package cc.alcina.extras.dev.console.remote.client.module.index;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

import cc.alcina.extras.dev.console.remote.client.common.widget.nav.NavComponent;

public class IndexSignedIn extends Composite {
	private FlowPanel fp;

	public IndexSignedIn() {
		this.fp = new FlowPanel();
		IndexModule.ensure();
		initWidget(fp);
		IndexStyles.MAIN_PANEL.set(this);
		render();
	}

	private void render() {
		fp.add(new NavComponent());
	}
}
