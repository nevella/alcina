package cc.alcina.framework.gwt.client.tour;

import cc.alcina.framework.gwt.client.gen.SimpleCssResource;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

public interface TourResources extends ClientBundle {
	
	@Source("tour.css")
	public SimpleCssResource tourCss();
}
