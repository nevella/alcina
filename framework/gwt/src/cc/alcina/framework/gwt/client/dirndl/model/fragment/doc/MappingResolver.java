package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.dom.DomNodeType;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasEquivalenceString;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed.Impl;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocument.SpecialisedDescendant;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.MarkupSchema.FragmentDocumentClass;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocumentNode.Mapping;

/**
 * A MappingResolver encapsulates {@code @Mappings/@Mapping} resolution (which
 * mapping should the transformer/parser use, based on schema and possibly
 * specialisedDescendant)
 *
 */
public class MappingResolver {
	MarkupSchema schema;

	ClassReflector reflector;

	Class<? extends FragmentDocumentNode> nodeType;

	/*
	 * (future) use to say customise latex output of docproponent (to 'court')
	 */
	Class<? extends SpecialisedDescendant> specialisedDescendant;

	/**
	 * Tests incoming DomNodes to see if they match the encapsulated mapping,
	 * and if they do, the DomNode corresponds to the MappingResolver.nodeType
	 * type. Note there's only one mapping per MappingResolver
	 */
	class ResolvedMapping implements Comparable<ResolvedMapping>,
			HasEquivalenceString<ResolvedMapping> {
		Mapping mapping;

		Directed directed;

		ResolvedMapping(Mapping mapping) {
			this.mapping = mapping;
			directed = mapping.directed();
		}

		boolean matches(DomNode node) {
			return node.tagAndClassIs(directed.tag(), directed.className());
		}

		Class<? extends FragmentDocumentNode> nodeType() {
			return nodeType;
		}

		/**
		 * Ordered most-specific (tag, class) to least
		 */
		@Override
		public int compareTo(ResolvedMapping o) {
			{
				int cmp = specicifity() - o.specicifity();
				if (cmp != 0) {
					return cmp;
				}
			}
			return nodeType().getSimpleName()
					.compareTo(o.nodeType().getSimpleName());
		}

		int specicifity() {
			return directed.className().isEmpty() ? 1 : 0;
		}

		@Override
		public String equivalenceString() {
			DomNodeType domNodeType = Reflections.at(directed.renderer())
					.templateInstance().rendersAsType();
			if (!domNodeType.hasVariantName()) {
				return domNodeType.w3cNodeTypeString();
			}
			String tagName = directed.tag();
			String className = Ax.blankTo(directed.className(), "*");
			return Ax.format("%s.%s", tagName, className);
		}

		@Override
		public String toString() {
			return Ax.format("%s - %s", NestedName.get(nodeType),
					equivalenceString());
		}

		public Directed getDirected() {
			return directed;
		}
	}

	MappingResolver(MarkupSchema markupSchema,
			Class<? extends SpecialisedDescendant> specialisedDescendant,
			Class<? extends FragmentDocumentNode> nodeType) {
		this.schema = markupSchema;
		this.specialisedDescendant = specialisedDescendant;
		this.nodeType = nodeType;
	}

	Mapping asFragmentDocumentClass(Mapping fragmentDocumentTags) {
		Mapping.Impl impl = new Mapping.Impl(fragmentDocumentTags);
		Impl directed = new Directed.Impl(impl.directed);
		impl.directed = directed;
		String tag = Ax.blankTo(directed.tag(),
				DirectedRenderer.tagName(nodeType));
		String tagPart = tag;
		String classPart = directed.className();
		// akomoa-ntoso compatibility
		if (tag.contains("-")) {
			if (tag.matches("(block|inline)-.+")) {
				tagPart = tag.replaceFirst("(.+?)-(.+)", "$1");
				classPart = tag.replaceFirst("(.+?)-(.+)", "$2");
			} else {
				tagPart = tag.replace("-", "");
			}
		}
		directed.setTag(tagPart);
		directed.setClassName(classPart);
		return impl;
	}

	ResolvedMapping getResolvedMapping() {
		List<ResolvedMapping> candidateMapings = getCandidateMappings();
		Optional<ResolvedMapping> exact = candidateMapings.stream()
				.filter(m -> m.mapping.schema() == schema.getClass())
				.findFirst();
		if (exact.isPresent()) {
			return exact.get();
		}
		/*
		 * fragmentDocumenttags, default --internal--; this maps directly to
		 * other internal types (tex) or with a bidi transform to
		 * fragmentDocumentclass
		 */
		Optional<ResolvedMapping> fragmentDocumentTags = candidateMapings
				.stream()
				.filter(m -> m.mapping
						.schema() == MarkupSchema.FragmentDocumentTags.class)
				.findFirst();
		if (fragmentDocumentTags.isPresent()) {
			if (schema == FragmentDocumentClass.TYPE) {
				return new ResolvedMapping(asFragmentDocumentClass(
						fragmentDocumentTags.get().mapping));
			}
			return fragmentDocumentTags.get();
		}
		return new ResolvedMapping(
				new FragmentDocumentNode.Mapping.Impl(nodeType));
	}

	List<ResolvedMapping> getCandidateMappings() {
		List<Mapping> mappings = Reflections.at(nodeType)
				.annotations(Mapping.class);
		return mappings.stream().map(ResolvedMapping::new)
				.collect(Collectors.toList());
	}
}
