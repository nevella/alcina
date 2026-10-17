package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.FragmentDocument.SpecialisedDescendant;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.MappingResolver.ResolvedMapping;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.FragmentDocumentOverlayToken;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens.FragmentDocumentOverlayToken.Transformer;

/**
 * <p>
 * Marker class that identifies input/output schema for a FragmentDocument.
 *
 * <p>
 * It's used by the @Mapping annotation, and is a property of the document.
 *
 *
 *
 */
public abstract class MarkupSchema {
	private static final transient Map<Class<? extends MarkupSchema>, MarkupSchema> typeInstances = new LinkedHashMap<>();

	public static Class<? extends MarkupSchema> defaultSchema() {
		return FragmentDocumentClass.class;
	}

	public static MarkupSchema schemaOrDefault(MarkupSchema schema) {
		return schema != null ? schema : type(defaultSchema());
	}

	static <N extends FragmentDocumentNode> FragmentDocumentRoots<N>
			createDocument(Class<N> nodeType, DomDocument document) {
		return new FragmentDocumentRoots<>(nodeType, document);
	}

	static MarkupSchema type(Class<? extends MarkupSchema> schema) {
		if (typeInstances.isEmpty()) {
			synchronized (MarkupSchema.class) {
				if (typeInstances.isEmpty()) {
					// ensure fields constructed
					FragmentDocumentClass.TYPE.register();
					FragmentDocumentTags.TYPE.register();
				}
			}
		}
		return typeInstances.get(schema);
	}

	SchemaMappings schemaMappings = new SchemaMappings(this);

	Map<Class<? extends FragmentDocumentNode>, FragmentDocumentOverlayToken> nodeTokens;

	private MarkupSchema() {
		nodeTokens = new LinkedHashMap<>();
		Registry.types(Transformer.class)
				.filter(clazz -> clazz != Transformer.class).forEach(clazz -> {
					Class<Transformer<?, ?>> t = (Class<Transformer<?, ?>>) (Class<?>) clazz;
					FragmentDocumentOverlayToken to = nodeTokens
							.put(Transformer.to(t), FragmentDocumentOverlayToken
									.typeInstance(Transformer.from(t)));
					Preconditions.checkState(to == null,
							"Duplicate transformer 'to' type");
				});
	}

	public FragmentDocumentNode create(FragmentDocumentNode parent,
			DomNode domNode) {
		Optional<ResolvedMapping> resolvedMapping = schemaMappings.getMapping(
				parent.document.permittedSpecialisedDescendants(), domNode);
		return resolvedMapping.isPresent()
				? Reflections.newInstance(resolvedMapping.get().nodeType())
				: new FragmentDocumentNode.GenericNode();
	}

	public FragmentDocumentOverlayToken getToken(DomNode domNode,
			Class<? extends SpecialisedDescendant> permittedSpecialisedDescendants) {
		Optional<ResolvedMapping> oResolvedMapping = schemaMappings
				.getMapping(permittedSpecialisedDescendants, domNode);
		return oResolvedMapping.isPresent()
				? getToken(oResolvedMapping.get().nodeType())
				: null;
	}

	<N extends FragmentDocumentNode> FragmentDocumentRoots<N>
			createDocument(Class<N> nodeType) {
		return new FragmentDocumentRoots<>(nodeType, this);
	}

	FragmentDocumentOverlayToken
			getToken(Class<? extends FragmentDocumentNode> nodeType) {
		return nodeTokens.get(nodeType);
	}

	void register() {
		typeInstances.put(getClass(), this);
	}

	/*
	 *
	 * 
	 * Relates fragmentnodes to tag-class paris
	 */
	public static class FragmentDocumentClass extends MarkupSchema {
		public static final FragmentDocumentClass TYPE = new FragmentDocumentClass();
	}

	/*
	 * Relates fragmentnodes to tag-only paris
	 */
	public static class FragmentDocumentTags extends MarkupSchema {
		public static final FragmentDocumentTags TYPE = new FragmentDocumentTags();
	}

	static class SchemaOracle {
		MarkupSchema getSchema(Class<? extends FragmentDocumentNode> nodeType,
				DomDocument document) {
			return typeInstances.values().stream()
					.filter(schema -> schema.schemaMappings
							.getMapping(null, document.getDocumentElementNode())
							.isPresent())
					.findFirst().get();
		}
	}
}
