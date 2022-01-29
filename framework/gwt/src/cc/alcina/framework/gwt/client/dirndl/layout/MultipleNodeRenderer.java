package cc.alcina.framework.gwt.client.dirndl.layout;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.ClientVisible;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

/**
 * FIXME - dirndl 1.1 - this: @MultipleNodeRendererLeaf(@Directed) - shouldn't
 * be needed
 * 
 * @author nick@alcina.cc
 *
 */
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
			/*
			 * No @Directed resolution - these are just tag/className tuples
			 */
			Directed tagClassName = new IntermediateDirected(idx, args);
			result.add(tagClassName);
		}
		MultipleNodeRendererLeaf leaf = node
				.annotation(MultipleNodeRendererLeaf.class);
		/*
		 * the leaf annotation carries the @Directed for the innermost widget
		 * rendered. It resolves normally
		 */
		if (leaf != null) {
			/*
			 * if the leaf is from a class annotation (more accurately, *NOT*
			 * from a property annotation) , ascend from the model superclass
			 * rather than the class - otherwise will loop indefinitely
			 */
			Class ascendFrom = node.property != null
					&& node.property
							.has(MultipleNodeRendererLeaf.class)
									? node.model.getClass()
									: node.model.getClass().getSuperclass();
			Directed leafValue = leaf.value();
			Directed directed = CustomReflectorResolver.forParentAndValue(
					MultipleNodeRendererArgs.class, node, ascendFrom,
					leafValue);
			result.add(directed);
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
		String[] cssClasses() default {};

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
	private final class IntermediateDirected extends Directed.Default {
		private final int idx;

		private final MultipleNodeRendererArgs args;

		private IntermediateDirected(int idx, MultipleNodeRendererArgs args) {
			this.idx = idx;
			this.args = args;
		}

		@Override
		public String cssClass() {
			return this.args.cssClasses().length == 0 ? ""
					: this.args.cssClasses()[this.idx];
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
