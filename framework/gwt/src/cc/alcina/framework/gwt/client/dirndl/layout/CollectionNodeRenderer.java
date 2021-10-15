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
	@Override
	public Widget render(Node node) {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Widget> renderWithDefaults(Node node) {
		List<Widget> result = new ArrayList<>();
		CollectionNodeRendererArgs args = node
				.annotation(CollectionNodeRendererArgs.class);
		Collection collection = (Collection) node.model;
		int idx = 0;
		// FIXME - dirndl1.0 - this prevents some sort of caching issue - fix
		// annotationlocation to allow custom res paths (2021.10.09 - may
		// already be fixed
		// with CustomReflectorResolver fix)
		//
		// FIXME - dirndl1.2 - reuse previously created widgets (or elements)
		node.directed.bindings();
		for (Object object : collection) {
			Node child = node.addChild(object, null, null);
			/*
			 * FIXME - dirndl.context - remove (since resolveModel will go)
			 */
			Class<? extends Object> modelClass = node.getResolver()
					.resolveModel(object).getClass();
			if (args != null) {
				child.directed = CustomReflectorResolver.forParentAndValue(
						CollectionNodeRendererArgs.class, node, modelClass,
						args.value());
			} else {
				child.directed = CustomReflectorResolver.forParentAndValue(
						CollectionNodeRendererArgs.class, node, modelClass,
						Directed.Default.INSTANCE);
			}
			result.addAll(child.render().widgets);
		}
		return result;
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface CollectionNodeRendererArgs {
		public Directed value();
	}
}
