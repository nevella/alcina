package cc.alcina.framework.gwt.client.dirndl.model.fragment;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.HasParentNodeAccess;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode.Transformer;

/**
 * <p>
 * This class acts as the main base for bi-directional model-tree dom-tree
 * transformations
 *
 *
 *
 */
@Transformer(NodeTransformer.DirectedTransformer.class)
@Directed
public abstract class FragmentNode extends Model
		implements HasParentNodeAccess {
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

	protected FragmentModel fragmentModel;

	public Ancestors ancestors() {
		return new Ancestors();
	}

	public void append(FragmentNode child) {
		provideNode().appendFragmentChild(child);
	}

	public FragmentModel fragmentModel() {
		if (fragmentModel == null) {
			FragmentNode parent = getParent();
			if (parent == null) {
				Node parentNode = provideParentNode();
				if (parentNode == null) {
				} else {
					Object parentModel = parentNode.getModel();
					if (parentModel instanceof FragmentModel.Has) {
						fragmentModel = ((FragmentModel.Has) parentModel)
								.provideFragmentModel();
					}
				}
			} else {
				fragmentModel = parent.fragmentModel();
			}
		}
		return fragmentModel;
	}

	public FragmentNode getParent() {
		Node parentNode = provideParentNode();
		if (parentNode == null) {
			return null;
		}
		Object parentModel = parentNode.getModel();
		if (parentModel instanceof FragmentNode) {
			return (FragmentNode) parentModel;
		} else {
			return null;
		}
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder().separator(" - ");
		format.append(getClass().getSimpleName());
		format.append(provideNode().getRendered().asDomNode());
		return format.toString();
	}

	/**
	 * Includes self by default
	 *
	 *
	 */
	public class Ancestors {
		boolean excludeSelf;

		public Ancestors excludeSelf() {
			this.excludeSelf = true;
			return this;
		}

		public boolean has(Class<? extends FragmentNode> test) {
			return stream().anyMatch(
					n -> Reflections.isAssignableFrom(test, n.getClass()));
		}

		public Stream<FragmentNode> stream() {
			return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
					new Itr(), Spliterator.ORDERED), false);
		}

		class Itr implements Iterator<FragmentNode> {
			FragmentNode cursor;

			Itr() {
				cursor = FragmentNode.this;
				if (excludeSelf) {
					cursor = cursor.getParent();
				}
			}

			@Override
			public boolean hasNext() {
				return cursor != null;
			}

			@Override
			public FragmentNode next() {
				if (cursor == null) {
					throw new NoSuchElementException();
				}
				FragmentNode result = cursor;
				cursor = cursor.getParent();
				return result;
			}
		}
	}

	/*
	 * Reverse transformation falls back on this model if no other matches exist
	 */
	@Transformer(NodeTransformer.Generic.class)
	public static class Generic extends FragmentNode {
	}

	/*
	 * Models a w3c Text node
	 */
	@Transformer(NodeTransformer.Text.class)
	@Directed(renderer = LeafRenderer.TextNode.class)
	public static class Text extends FragmentNode {
		private String value;

		public Text() {
		}

		public Text(String value) {
			this.value = value;
		}

		@Binding(type = Type.INNER_TEXT)
		public String getValue() {
			return this.value;
		}

		public void setValue(String value) {
			set("value", this.value, value, () -> this.value = value);
		}
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
