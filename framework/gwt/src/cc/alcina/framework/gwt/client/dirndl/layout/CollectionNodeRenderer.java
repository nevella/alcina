package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

@RegistryLocation(registryPoint = DirectedNodeRenderer.class, targetClass = AbstractCollection.class)
public class CollectionNodeRenderer extends DirectedNodeRenderer {
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface CollectionNodeRendererArgs {
		public Directed value();
	}

	@Override
	public List<Widget> renderWithDefaults(Node node) {
		List<Widget> result = new ArrayList<>();
		CollectionNodeRendererArgs args = node
				.annotation(CollectionNodeRendererArgs.class);
		Collection collection = (Collection) node.model;
		int idx = 0;
		for (Object object : collection) {
			Node child = node.addChild(object, null, null);
			if (args != null) {
				child.directed = args.value();
			}
			result.addAll(child.render().widgets);
		}
		return result;
	}

	@Override
	public Widget render(Node node) {
		throw new UnsupportedOperationException();
	}
}
