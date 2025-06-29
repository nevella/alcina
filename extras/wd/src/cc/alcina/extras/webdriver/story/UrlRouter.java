package cc.alcina.extras.webdriver.story;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Url;
import cc.alcina.framework.common.client.util.UrlBuilder;
import cc.alcina.framework.gwt.client.story.Story.Action.Context;
import cc.alcina.framework.gwt.client.story.Story.Action.Context.PerformerResource;

public class UrlRouter implements PerformerResource {
	UrlRouterPart part;

	@Override
	public void initialise(Context context) {
		part = context.tellerContext().getPart(UrlRouterPart.class);
	}

	public String route(String to) {
		Url url = Url.parse(to);
		UrlBuilder builder = url.toBuilder();
		if (url.host == null) {
			builder.withProtocol(part.protocol);
			builder.withHost(part.host);
			builder.withPort(part.port);
		}
		if (to.equals("/") && Ax.notBlank(part.path)) {
			builder.withPath(part.path);
		}
		if (part.gwtDevMode) {
			builder.withQueryStringParam("gwt.l", null);
		}
		return builder.build();
	}
}