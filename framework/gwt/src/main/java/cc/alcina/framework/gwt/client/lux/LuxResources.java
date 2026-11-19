package cc.alcina.framework.gwt.client.lux;

import com.google.gwt.resources.client.ClientBundle;

import cc.alcina.framework.gwt.client.gen.SimpleCssResource;

public interface LuxResources extends ClientBundle {
	@Source("res/lux.css")
	public SimpleCssResource luxStyles();
}
