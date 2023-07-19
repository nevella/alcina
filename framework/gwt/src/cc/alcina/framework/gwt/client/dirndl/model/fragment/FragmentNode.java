package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.RendererInput;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public interface FragmentNode {
	static boolean provideIsModelFor(org.w3c.dom.Node w3cNode,
			Class<? extends FragmentNode> fragmentNodeType) {
		return provideTransformerFor(fragmentNodeType).appliesTo(w3cNode);
	}

	static NodeTransformer provideTransformerFor(
			Class<? extends FragmentNode> fragmentNodeType) {
		Class<? extends NodeTransformer> transformerClass = Reflections
				.at(fragmentNodeType).annotation(Transformer.class).value();
		NodeTransformer transformer = Reflections.newInstance(transformerClass);
		transformer.setFragmentNodeType(fragmentNodeType);
		return transformer;
	}

	@Transformer(NodeTransformer.DirectedTransformer.class)
	@Directed(renderer = AbstractNode.Renderer.class)
	public static abstract class AbstractNode extends Model
			implements FragmentNode {
		public static class Renderer extends DirectedRenderer {
			@Override
			protected void render(RendererInput input) {
				throw new UnsupportedOperationException(
						"Not (currently) intended for rendering, rather for reverse (doc -> model) transformation/parsing");
			}
		}
	}

	/*
	 * Reverse transformation falls back on this model if no other matches exist
	 */
	@Transformer(NodeTransformer.Generic.class)
	public static class Generic extends AbstractNode {
	}

	/*
	 * Models a w3c Text node
	 */
	@Transformer(NodeTransformer.Text.class)
	public static class Text extends AbstractNode {
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Inherited
	@Target({ ElementType.TYPE })
	public @interface Transformer {
		Class<? extends NodeTransformer> value();
	}
}
