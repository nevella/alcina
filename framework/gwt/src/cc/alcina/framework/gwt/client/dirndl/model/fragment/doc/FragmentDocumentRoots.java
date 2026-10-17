package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.MarkupSchema.SchemaOracle;

class FragmentDocumentRoots<N extends FragmentDocumentNode> {
	// FIXME - durndl - this can be removed (do a traversal from the
	// document root node to determine what node classes are possible)
	Class<N> nodeType;

	DomDocument document;

	MarkupSchema schema;

	N instance;

	FragmentDocumentRoots(Class<N> nodeType, DomDocument document) {
		this.nodeType = nodeType;
		this.document = document;
		schema = new SchemaOracle().getSchema(nodeType, document);
		create();
	}

	FragmentDocumentRoots(Class<N> nodeType, MarkupSchema schema) {
		this.nodeType = nodeType;
		this.schema = schema;
		create();
	}

	void create() {
		instance = Reflections.newInstance(nodeType);
	}
}