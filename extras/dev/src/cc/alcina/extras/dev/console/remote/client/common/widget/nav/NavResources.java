package cc.alcina.extras.dev.console.remote.client.common.widget.nav;

import com.google.gwt.resources.client.ClientBundle;

import cc.alcina.framework.gwt.client.gen.SimpleCssResource;

public interface NavResources extends ClientBundle {
	@Source("res/nav-popup.css")
	public SimpleCssResource navPopup();

	@Source("res/nav.css")
	public SimpleCssResource navStyles();

	@Source("res/nav-center.css")
	public SimpleCssResource navStylesCenter();

	@Source("res/nav-center-action-button.css")
	public SimpleCssResource navStylesCenterActionButton();

	@Source("res/nav-center-menu-button.css")
	public SimpleCssResource navStylesCenterMenuButton();

	@Source("res/nav-center-search.css")
	public SimpleCssResource navStylesCenterSearch();
}
