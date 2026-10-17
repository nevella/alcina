package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CollectionCreators;
import cc.alcina.framework.common.client.util.HasEquivalence.HasEquivalenceHelper;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocument.SpecialisedDescendant;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.MappingResolver.ResolvedMapping;

/*
 * Models the mappings for a particular schema. These are sorted most->least
 * specific, with initialisation checks for ambiguity
 */
class SchemaMappings {
	private MarkupSchema markupSchema;

	public SchemaMappings(MarkupSchema markupSchema) {
		this.markupSchema = markupSchema;
	}

	Map<Class<? extends SpecialisedDescendant>, SpecialisedMappings> specialisedMappings = (Map) CollectionCreators.Bootstrap
			.createConcurrentClassMap();

	/*
	 * Models the set of mappings corresponging to FragmentDocumentNode subtypes
	 * which are possible descendants of a particular document type
	 * (PracticeNote, LegislativeInstrument etc) - either SpecialisedDescendant
	 * subtypes (document-specific) or not (all docs).
	 * 
	 * There must be no duplicates of tag/class mappings - since that will make
	 * simple bidirectional mapping impossible (context-based mapping would be
	 * possible, but harder and not necessary at this stage)
	 * 
	 * TL;DR - all node types corresponding to a particular documenttype should
	 * implement a SpecialisedDescendant subtype
	 */
	class SpecialisedMappings {
		Class<? extends SpecialisedDescendant> specialisedDescendant;

		Map<Class<? extends FragmentDocumentNode>, ResolvedMapping> mappings;

		SpecialisedMappings(
				Class<? extends SpecialisedDescendant> specialisedDescendant) {
			this.specialisedDescendant = specialisedDescendant;
			mappings = Registry.types(FragmentDocumentNode.class)
					.filter(type -> !(Reflections
							.isAssignableFrom(FragmentDocument.class, type)))
					.map(node -> new MappingResolver(markupSchema,
							specialisedDescendant, node).getResolvedMapping())
					.filter(m -> {
						Class<? extends FragmentDocumentNode> nodeType = m
								.nodeType();
						if (Reflections.at(nodeType).isAbstract()) {
							return false;
						}
						if (Reflections.isAssignableFrom(
								SpecialisedDescendant.class, nodeType)
								&& !Reflections.isAssignableFrom(
										specialisedDescendant, nodeType)) {
							return false;
						} else {
							return true;
						}
					}).sorted().collect(AlcinaCollectors
							.toKeyMap(ResolvedMapping::nodeType));
			Multimap<ResolvedMapping, List<ResolvedMapping>> mapDuplicates = HasEquivalenceHelper
					.mapDuplicates(mappings.values());
			mapDuplicates.entrySet().removeIf(e -> e.getValue().size() == 1);
			if (mapDuplicates.size() > 0) {
				throw new IllegalArgumentException(
						"Duplicate mappings:\n" + mapDuplicates);
			}
		}

		void dump() {
			Ax.out(toString());
		}

		@Override
		public String toString() {
			return Ax.format("%s::%s\n====================\n%s",
					NestedName.get(markupSchema),
					NestedName.get(specialisedDescendant),
					mappings.values().stream().map(Object::toString)
							.collect(Collectors.joining("\n")));
		}

		Optional<ResolvedMapping> getMapping(DomNode domNode) {
			Optional<ResolvedMapping> oMapping = mappings.values().stream()
					.filter(m -> m.matches(domNode)).findFirst();
			return oMapping;
		}
	}

	SpecialisedMappings
			getMappings(Class<? extends SpecialisedDescendant> key) {
		return specialisedMappings.computeIfAbsent(key,
				SpecialisedMappings::new);
	}

	/**
	 * Return the mapping (and thus FragmentDocumentNode subtype) corresponding
	 * to a particular DomNode
	 * 
	 * @param domNode
	 *            The dom node to match
	 * @param permittedSpecialisedDescendants
	 *            The owner document type descendant node filter
	 * @return the matching FragmentDocument FragmentDocumentNode type's mapping
	 * 
	 */
	Optional<ResolvedMapping> getMapping(
			Class<? extends SpecialisedDescendant> permittedSpecialisedDescendants,
			DomNode domNode) {
		return getMappings(permittedSpecialisedDescendants).getMapping(domNode);
	}

	public ResolvedMapping getMapping(
			Class<? extends SpecialisedDescendant> permittedSpecialisedDescendants,
			Class<? extends FragmentDocumentNode> nodeClass) {
		return getMappings(permittedSpecialisedDescendants).mappings
				.get(nodeClass);
	}
}
