package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasFilterableString;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentNode;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode.AttributeMappingType;

@Registration(FragmentDocumentNode.class)
@AttributeMappingType.Ref(AttributeMappingType.NONE)
public abstract class FragmentDocumentNode extends FragmentNode implements
		HasMapping, Registration.AllSubtypesClient, HasFilterableString {
	public FragmentDocumentNode splitReplicatedFrom;

	boolean split;

	@Binding(type = Type.PROPERTY)
	public boolean isSplit() {
		return split;
	}

	public void setSplit(boolean split) {
		set("split", this.split, split, () -> this.split = split);
	}

	public static class GenericNode extends FragmentDocumentNode {
	}

	/**
	 * Whether a node has 0, 1 or greater than 1 attributes (this type models
	 * how node attributes are mapped to a non-map structure such as tex []
	 * environment parameters)
	 */
	public enum AttributeMappingType {
		NONE, SINGLE, MULTIPLE;

		@ClientVisible
		@Retention(RetentionPolicy.RUNTIME)
		@Documented
		@Target(ElementType.TYPE)
		public @interface Ref {
			/**
			 * The placeholder
			 */
			AttributeMappingType value();
		}
	}

	/*
	 * Marker for a node that should not have children attached
	 */
	public interface Leaf {
	}

	// WIP - @Directed should define the tag mapping, @Validation should define
	// any validation rules
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Repeatable(Mappings.class)
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface Mapping {
		static class Impl implements Mapping {
			boolean descend = true;

			Directed directed = Directed.Impl.DEFAULT_INSTANCE;

			Class<? extends FragmentDocumentNode> implementationType = VoidNode.class;

			boolean optional = false;

			Class<? extends MarkupSchema> schema = MarkupSchema.FragmentDocumentTags.class;

			Type type = Type.ELEMENT;

			Impl(Mapping from) {
				descend = from.descend();
				directed = from.directed();
				implementationType = from.implementationType();
				optional = from.optional();
				schema = from.schema();
				type = from.type();
			}

			Impl(Class<? extends FragmentDocumentNode> nodeType) {
				directed = new Directed.Impl()
						.withTag(nodeType.getSimpleName().toLowerCase());
			}

			@Override
			public Class<? extends Annotation> annotationType() {
				return Mapping.class;
			}

			@Override
			public boolean descend() {
				return descend;
			}

			@Override
			public Directed directed() {
				return directed;
			}

			@Override
			public Class<? extends FragmentDocumentNode> implementationType() {
				return implementationType;
			}

			@Override
			public boolean optional() {
				return optional;
			}

			@Override
			public Class<? extends MarkupSchema> schema() {
				return schema;
			}

			@Override
			public Type type() {
				return type;
			}
		}

		@Reflected
		public enum Type {
			ELEMENT, ATTRIBUTE, TEXT
		}

		boolean descend() default true;

		Directed directed();

		/*
		 * Only required for collection mappings
		 */
		Class<? extends FragmentDocumentNode> implementationType() default VoidNode.class;

		boolean optional() default false;

		Class<? extends MarkupSchema> schema() default MarkupSchema.FragmentDocumentTags.class;

		Type type() default Type.ELEMENT;
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Documented
	@Target({ ElementType.TYPE, ElementType.METHOD })
	public @interface Mappings {
		public Mapping[] value();
	}

	/*
	 * Marker for fragmentDocument node property ordering types
	 */
	public interface NodeProperty extends PropertyEnum {
	}

	/*
	 * Marker for a node that not be included in an output document
	 */
	public interface NonOutput {
	}

	/*
	 * Marker for a node that should not be included in an output document
	 * *including children*
	 */
	public interface NonOutputTree extends NonOutput {
	}

	/*
	 * Marker for a node that should be included in an output document *but not
	 * its children*
	 */
	public interface NonOutputTreeChildren {
	}

	/*
	 * Marker for a node that should be included in an output document *but only
	 * some of its children*
	 */
	public interface FilteringOutputTreeChildren {
		boolean allowTreeChild(
				Class<? extends FragmentDocumentNode> outputType);
	}

	/*
	 * Only subtypes can contain text DOM nodes
	 */
	public interface TextContainer {
	}

	public static class VoidNode extends FragmentDocumentNode
			implements NonOutput {
	}

	public static class VoidTreeNode extends FragmentDocumentNode
			implements NonOutputTree {
	}

	public static String mappingToString(Mapping mapping) {
		throw new UnsupportedOperationException();
		// return mapping == null ? null
		// : Ax.isBlank(mapping.className()) ? mapping.name()
		// : Ax.format("%s.%s", mapping.name(),
		// mapping.className());
	}

	/*
	 * may or not be null - document() is safer
	 */
	public FragmentDocument document;

	public FragmentDocument document() {
		return document != null ? document
				: ((TransformingFragmentModel) fragmentModel()).document;
	}

	public String asText() {
		if (this instanceof TextContainer) {
			return domNode().textContent();
		} else {
			throw new UnsupportedOperationException();
		}
	}

	protected String toStringContents() {
		DirectedLayout.Node layoutNode = provideNode();
		if (layoutNode != null) {
			return Ax.trimForLogging(
					layoutNode.getRendered().asDomNode().toMarkup());
		} else {
			return "[not rendered]";
		}
	}

	@Override
	public String toString() {
		return Ax.format("%s :: %s", NestedName.get(this), toStringContents());
	}

	@Override
	public String toFilterableString() {
		FormatBuilder format = new FormatBuilder();
		format.line(getClass().getSimpleName());
		DirectedLayout.Node layoutNode = provideNode();
		if (layoutNode != null) {
			format.line(layoutNode.getRendered().asDomNode().toString());
		}
		return format.toString();
	}

	protected <N extends FragmentDocumentNode> N
			get(FragmentDocumentNode.NodeProperty property) {
		throw new UnsupportedOperationException();
	}
}
