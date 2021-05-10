package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.gwt.client.dirndl.annotation.Behaviour;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

public class MultipleNodeRenderer extends DirectedNodeRenderer
		implements HasWrappingDirecteds {
	@Override
	public List<Directed> getWrappingDirecteds(Node node) {
		List<Directed> result = new ArrayList<>();
		MultipleNodeRendererArgs args = node
				.annotation(MultipleNodeRendererArgs.class);
		FlowPanel parent = null;
		FlowPanel root = null;
		for (int idx = 0; idx < args.tags().length; idx++) {
			Directed tagClassName = new DirectedImplementation(idx, args);
			result.add(tagClassName);
		}
		MultipleNodeRendererLeaf leaf = node
				.annotation(MultipleNodeRendererLeaf.class);
		if (leaf != null) {
			Directed leafDirected = leaf.value();
			/*
			 * if the property has a simple @Directed annotation, and the class
			 * has a non-simple @Directed, use the class
			 */
			if (DirectedLayout.isDefault(leafDirected)) {
				Object model = node.getModel();
				Class clazz = model == null ? void.class : model.getClass();
				if (clazz != null) {
					Directed directed = Reflections.classLookup()
							.getAnnotationForClass(clazz, Directed.class);
					if (directed != null) {
						leafDirected = directed;
					}
				}
			}
			result.add(leafDirected);
		}
		return result;
	}

	@Override
	public Widget render(Node node) {
		throw new UnsupportedOperationException();
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface MultipleNodeRendererArgs {
		String[] cssClasses();

		String[] tags();
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface MultipleNodeRendererLeaf {
		Directed value();
	}

	/*
	 * Fabricates a 'directed' out of the supplied tag & css class
	 */
	private final class DirectedImplementation implements Directed {
		private final int idx;

		private final MultipleNodeRendererArgs args;

		private DirectedImplementation(int idx, MultipleNodeRendererArgs args) {
			this.idx = idx;
			this.args = args;
		}

		@Override
		public Class<? extends Annotation> annotationType() {
			return Directed.class;
		}

		@Override
		public Behaviour[] behaviours() {
			return new Behaviour[0];
		}

		@Override
		public Binding[] bindings() {
			return new Binding[0];
		}

		@Override
		public String cssClass() {
			return this.args.cssClasses()[this.idx];
		}

		@Override
		public boolean merge() {
			return false;
		}

		@Override
		public Class<? extends DirectedNodeRenderer> renderer() {
			return ContainerNodeRenderer.class;
		}

		@Override
		public String tag() {
			return this.args.tags()[this.idx];
		}
	}
}
