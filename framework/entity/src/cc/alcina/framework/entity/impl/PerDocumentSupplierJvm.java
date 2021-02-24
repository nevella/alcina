package cc.alcina.framework.entity.impl;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;

import org.w3c.dom.Document;

import cc.alcina.framework.common.client.dom.DomDoc;
import cc.alcina.framework.common.client.dom.DomDoc.PerDocumentSupplier;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = PerDocumentSupplier.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.MANUAL_PRIORITY)
public class PerDocumentSupplierJvm extends PerDocumentSupplier {
	private Map<Document, WeakReference<DomDoc>> perDocumentWeak;

	public PerDocumentSupplierJvm() {
		perDocumentWeak = new WeakHashMap<>();
	}

	@Override
	public DomDoc get(Document document) {
		synchronized (perDocumentWeak) {
			return perDocumentWeak
					.computeIfAbsent(document,
							d -> new WeakReference<>(new DomDoc(document)))
					.get();
		}
	}
}
