package cc.alcina.framework.entity.impl;

import org.apache.xerces.dom.CoreDocumentImpl;
import org.w3c.dom.Document;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomDocument.PerDocumentSupplier;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Registration.Singleton(
	value = PerDocumentSupplier.class,
	priority = Registration.Priority.APP)
public class PerDocumentSupplierCoreDocument implements PerDocumentSupplier {
	public static final transient String CORE_DOCUMENT_DOM_DOC_KEY = PerDocumentSupplierCoreDocument.class
			.getName() + ".CORE_DOCUMENT_DOM_DOC_KEY";

	public static PerDocumentSupplierCoreDocument cast() {
		return (PerDocumentSupplierCoreDocument) Registry
				.impl(PerDocumentSupplier.class);
	}

	// called by a Lifecycle service subtype in the servlet layer
	public void onAppStartup() {
		DomDocument.topicDocumentCreated.add(this::register);
	}

	public PerDocumentSupplierCoreDocument() {
	}

	void register(DomDocument createdDoc) {
		Document doc = (Document) createdDoc.w3cNode();
		if (doc instanceof CoreDocumentImpl) {
			CoreDocumentImpl impl = (CoreDocumentImpl) doc;
			impl.setUserData(impl, CORE_DOCUMENT_DOM_DOC_KEY, createdDoc, null);
		}
	}

	@Override
	public DomDocument get(Document document) {
		synchronized (document) {
			if (document instanceof com.google.gwt.dom.client.Document) {
				return ((com.google.gwt.dom.client.Document) document).domDocument;
			} else {
				CoreDocumentImpl impl = (CoreDocumentImpl) document;
				DomDocument domDocument = (DomDocument) impl.getUserData(impl,
						CORE_DOCUMENT_DOM_DOC_KEY);
				if (domDocument == null) {
					domDocument = DomDocument.from(document, true);
					impl.setUserData(impl, CORE_DOCUMENT_DOM_DOC_KEY,
							domDocument, null);
				}
				return domDocument;
			}
		}
	}
}
