package cc.alcina.framework.entity.impl;

import org.apache.xerces.dom.CoreDocumentImpl;
import org.w3c.dom.Document;
import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomDoc.PerDocumentSupplier;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.Registration;

@RegistryLocation(registryPoint = PerDocumentSupplier.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.MANUAL_PRIORITY)
@Registration.Singleton(value = PerDocumentSupplier.class, priority = Registration.Priority.MANUAL)
public class PerDocumentSupplierCoreDocument extends PerDocumentSupplier {

    public static final transient String CORE_DOCUMENT_DOM_DOC_KEY = PerDocumentSupplierCoreDocument.class.getName() + ".CORE_DOCUMENT_DOM_DOC_KEY";

    public PerDocumentSupplierCoreDocument() {
    }

    @Override
    public DomDoc get(Document document) {
        synchronized (document) {
            CoreDocumentImpl impl = (CoreDocumentImpl) document;
            DomDoc userData = (DomDoc) impl.getUserData(impl, CORE_DOCUMENT_DOM_DOC_KEY);
            if (userData == null) {
                userData = new DomDoc(document);
                impl.setUserData(impl, CORE_DOCUMENT_DOM_DOC_KEY, userData, null);
            }
            return userData;
        }
    }
}
