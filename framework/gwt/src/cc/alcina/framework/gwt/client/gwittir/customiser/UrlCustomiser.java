package cc.alcina.framework.gwt.client.gwittir.customiser;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundLink;

@ClientInstantiable
public class UrlCustomiser implements Customiser {
	public static final String TARGET = "target";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, Custom params) {
		NamedParameter parameter = NamedParameter.Support
				.getParameter(params.parameters(), TARGET);
		String target = parameter == null ? null : parameter.stringValue();
		return new UrlProvider(target);
	}

	public static class BoundValueLink extends BoundLink<String> {
		public BoundValueLink() {
		}

		public BoundValueLink(String target) {
			super.setTarget(target);
		}

		public void setValue(String url) {
			super.setValue(url);
			if (url != null) {
				super.setHref(url);
			}
		}
	}

	public static class UrlProvider implements BoundWidgetProvider {
		private final String target;

		public UrlProvider(String target) {
			this.target = target;
		}

		public BoundWidget get() {
			return new BoundValueLink(target);
		}
	}
}
