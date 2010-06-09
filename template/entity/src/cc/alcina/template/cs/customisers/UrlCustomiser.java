package cc.alcina.template.cs.customisers;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.CustomiserInfo;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.gwt.client.gwittir.customiser.Customiser;
import cc.alcina.framework.gwt.client.gwittir.widget.BoundLink;

import com.totsp.gwittir.client.ui.BoundWidget;
import com.totsp.gwittir.client.ui.util.BoundWidgetProvider;

@ClientInstantiable
public class UrlCustomiser implements Customiser {
	public static final String TARGET = "target";

	public BoundWidgetProvider getProvider(boolean editable, Class objectClass,
			boolean multiple, CustomiserInfo params) {
		NamedParameter parameter = NamedParameter.Support.getParameter(params
				.parameters(), TARGET);
		String target = parameter == null ? null : parameter.stringValue();
		return new UrlProvider(target);
	}

	public static class UrlProvider implements BoundWidgetProvider {
		private final String target;

		public UrlProvider(String target) {
			this.target = target;
		}

		public BoundWidget get() {
			return new BoundBookmarkLink(target);
		}
	}

	public static class BoundBookmarkLink extends BoundLink<String> {
		public BoundBookmarkLink() {
		}

		public BoundBookmarkLink(String target) {
			super.setTarget(target);
		}

		public void setValue(String url) {
			super.setValue(url);
			if (url != null) {
				super.setHref(url);
			}
		}
	}
}
