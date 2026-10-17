package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.HasAnnotations;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.W3CResolver;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.FragmentModel;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.NodeTransformer;

class FragmentDocumentResolver extends W3CResolver.Linking
		implements NodeTransformer.Provider {
	MarkupSchema schema;

	Class<? extends FragmentDocumentNode> nodeType;

	FragmentDocumentRoot root;

	FragmentDocumentResolver(MarkupSchema schema,
			Class<? extends FragmentDocumentNode> nodeType,
			DomDocument document, FragmentDocumentNode root, boolean asGwtDoc) {
		this.schema = schema;
		this.nodeType = nodeType;
		this.document = document;
		this.root = (FragmentDocumentRoot) root;
		this.asGwtDoc = asGwtDoc;
		if (document != null) {
			this.linkModel = root;
			this.linkNode = document.getDocumentElementNode();
		}
	}

	static Set<Class> reflected = new LinkedHashSet<>();

	@Override
	public <A extends Annotation> A contextAnnotation(HasAnnotations reflector,
			Class<A> clazz, ResolutionContext resolutionContext) {
		if (clazz == Directed.class
				&& resolutionContext == ResolutionContext.Strategy
				&& reflector instanceof ClassReflector) {
			Class reflectedClass = ((ClassReflector) reflector)
					.getReflectedClass();
			if (Reflections.isAssignableFrom(HasMapping.class, reflectedClass)
					&& ((ClassReflector) reflector).hasNoArgsConstructor()) {
				return (A) schema.schemaMappings.getMapping(
						root.document.permittedSpecialisedDescendants(),
						reflectedClass).getDirected();
			}
		}
		return super.contextAnnotation(reflector, clazz, resolutionContext);
	}

	@Override
	public NodeTransformer createNodeTransformer(DomNode node) {
		return fragmentModel().createNodeTransformer(node);
	}

	FragmentModel fragmentModel() {
		return root.fragmentModel();
	}
}
