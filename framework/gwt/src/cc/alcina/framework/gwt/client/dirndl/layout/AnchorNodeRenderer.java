package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class AnchorNodeRenderer extends ContainerNodeRenderer {
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface AnchorNodeRendererHref {
		String value();
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface AnchorNodeRendererHrefProvider {
		Class<? extends AnchorNodeRendererHrefFunction> value();
	}

	@ClientInstantiable
	public static abstract class AnchorNodeRendererHrefFunction<A>
			implements Function<A, String> {
	}

	@Override
	protected String getTag(Node node) {
		return "a";
	}

	@Override
	public Widget render(Node node) {
		AnchorNodeRendererHref hrefConstant = node
				.annotation(AnchorNodeRendererHref.class);
		AnchorNodeRendererHrefProvider hrefProvider = node
				.annotation(AnchorNodeRendererHrefProvider.class);
		Widget rendered = super.render(node);
		String href = hrefConstant != null ? hrefConstant.value()
				: (String) Reflections.newInstance(hrefProvider.value())
						.apply(node.model);
		rendered.getElement().setAttribute("href", href);
		return rendered;
	}
}
