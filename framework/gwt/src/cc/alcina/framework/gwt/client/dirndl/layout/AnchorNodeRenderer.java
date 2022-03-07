package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Function;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class AnchorNodeRenderer extends ContainerNodeRenderer {
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface Href {
		String value() default "";
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface Id {
		String value() default "";
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface AnchorNodeRendererHrefProvider {
		Class<? extends AnchorNodeRendererHrefFunction> value();
	}

	@Reflected
	public static abstract class AnchorNodeRendererHrefFunction<A>
			implements Function<A, String> {
	}

	@Override
	protected String getTag(Node node) {
		return "a";
	}

	@Override
	public Widget render(Node node) {
		Widget rendered = super.render(node);
		String href = node.optional(Href.class).map(Href::value).orElse(null);
		href = href != null ? href
				: node.optional(AnchorNodeRendererHrefProvider.class)
						.map(p -> (String) Reflections.newInstance(p.value())
								.apply(node.model))
						.orElse(null);
		if (href != null) {
			rendered.getElement().setAttribute("href", href);
		}
		if (node.has(Id.class)) {
			String id = node.annotation(Id.class).value();
			rendered.getElement().setAttribute("id",
					id.isEmpty() ? node.model.toString() : id);
		}
		return rendered;
	}
}
