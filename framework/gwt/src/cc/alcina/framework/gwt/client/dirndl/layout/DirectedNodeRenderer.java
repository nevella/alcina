package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

@ClientInstantiable
public abstract class DirectedNodeRenderer {
	public abstract Widget render(Node node);

	public List<Widget> renderWithDefaults(Node node) {
		Widget rendered = render(node);
		renderDefaults(node, rendered);
		return Collections.singletonList(rendered);
	}

	protected void renderDefaults(Node node, Widget widget) {
		if (node.directed != null && node.directed.cssClass().length() > 0) {
			widget.addStyleName(node.directed.cssClass());
		}
		TitleProvider titleProvider = node.annotation(TitleProvider.class);
		if (titleProvider != null) {
			widget.setTitle((String) Reflections
					.newInstance(titleProvider.value()).apply(node.model));
		}
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface TitleProvider {
		Class<? extends TitleFunction> value();
	}

	@ClientInstantiable
	public static abstract class TitleFunction<A>
			implements Function<A, String> {
	}
}
