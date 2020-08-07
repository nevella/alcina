package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class AnchorNodeRenderer extends ContainerNodeRenderer {
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface AnchorNodeRendererArgs {
		String href();
	}

	@Override
	protected String getTag(Node node) {
		return "a";
	}

	@Override
	public Widget render(Node node) {
		AnchorNodeRendererArgs args = node
				.annotation(AnchorNodeRendererArgs.class);
		Widget rendered = super.render(node);
		rendered.getElement().setAttribute("href", args.href());
		return rendered;
	}
}
