package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode.Mapping;

public interface FootnoteNode {
	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "block", className = "footnote-inpage"))
	public static class FootnoteReferenceTarget extends Block {
		private String number;

		@Binding(type = Type.PROPERTY)
		public String getNumber() {
			return number;
		}

		public void setNumber(String number) {
			set("number", this.number, number, () -> this.number = number);
			if (provideIsBound()) {
				updateNumberNode();
			}
		}

		void updateNumberNode() {
			children().filter(
					n -> n instanceof FootnoteReferenceTarget.NumberNode)
					.toList().forEach(FragmentNode::removeFromParent);
			nodes().insertAsFirstChild(new NumberNode(number));
		}

		@Override
		public void onFragmentRegistration() {
			updateNumberNode();
		}

		@Property.Not
		public FootnoteReferenceTargetContents getContents() {
			return soleChildOfType(FootnoteReferenceTargetContents.class);
		}

		@Mapping(
			schema = MarkupSchema.FragmentDocumentClass.class,
			directed = @Directed(
				tag = "block",
				className = "footnote-inpage-number"))
		public static class NumberNode extends FragmentDocumentNode {
			String number;

			public NumberNode() {
			}

			public NumberNode(String number) {
				this.number = number;
			}

			@Override
			public void onFragmentRegistration() {
				nodes().append(new Inline.Text(number));
			}
		}
	}

	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "inline", className = "footnote"))
	public static class FootnoteReference extends FragmentDocumentNode
			implements Inline, FragmentDocumentNode.NonOutputTreeChildren {
		private String number;

		@Binding(type = Type.PROPERTY)
		public String getNumber() {
			return number;
		}

		public static boolean is(DomNode node) {
			return node.tagAndClassIs("inline", "footnote");
		}

		public void setNumber(String number) {
			set("number", this.number, number, () -> this.number = number);
			if (provideIsBound()) {
				NumberNode numberNode = byTypeNode(NumberNode.class);
				if (numberNode == null) {
					nodes().removeAllChildren();
					onFragmentRegistration();
				} else {
					numberNode.setNumber(number);
				}
			}
		}

		@Override
		public void onFragmentRegistration() {
			nodes().append(new NumberNode(number));
		}

		@Mapping(
			schema = MarkupSchema.FragmentDocumentClass.class,
			directed = @Directed(tag = "inline", className = "footnote-number"))
		public static class NumberNode extends FragmentDocumentNode {
			String number;

			public NumberNode() {
			}

			public void setNumber(String number) {
				set("number", this.number, number, () -> this.number = number);
				if (provideIsBound()) {
					Inline.Text text = byTypeNode(Inline.Text.class);
					if (text == null || number == null) {
						nodes().removeAllChildren();
						onFragmentRegistration();
					} else {
						text.setValue(number);
					}
				}
			}

			public NumberNode(String number) {
				this.number = number;
			}

			@Override
			public void onFragmentRegistration() {
				nodes().append(new Inline.Text(number));
			}

			public static boolean is(DomNode node) {
				return node.tagAndClassIs("inline", "footnote-number");
			}
		}
	}

	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(tag = "block", className = "footnotes-container"))
	public static class FootnotesContainer extends Block {
		public List<FootnoteReferenceTarget> footnoteReferenceTargets = new ArrayList<>();

		@Override
		public void onFragmentRegistration() {
			footnoteReferenceTargets.forEach(nodes()::append);
		}
	}

	@Mapping(
		schema = MarkupSchema.FragmentDocumentClass.class,
		directed = @Directed(
			tag = "block",
			className = "footnote-inpage-contents"))
	public static class FootnoteReferenceTargetContents extends Block {
	}
}