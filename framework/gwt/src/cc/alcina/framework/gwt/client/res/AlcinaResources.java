package cc.alcina.framework.gwt.client.res;

import cc.alcina.framework.gwt.client.gen.SimpleCssResource;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;

public interface AlcinaResources extends ClientBundle {
	public static final AlcinaResources INSTANCE = GWT
			.create(AlcinaResources.class);

	@Source("Alcina.css")
	public SimpleCssResource css();

	@Source("Alcina-iepre9.css")
	public TextResource cssIePre9();

	@Source("app-properties.txt")
	public TextResource appProperties();

	@Source("gwt-standard.css")
	public SimpleCssResource gwtStandardCss();
}