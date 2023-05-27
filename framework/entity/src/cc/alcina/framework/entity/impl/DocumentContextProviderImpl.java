package cc.alcina.framework.entity.impl;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;

import cc.alcina.framework.common.client.context.ContextFrame;
import cc.alcina.framework.common.client.context.ContextProvider;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.impl.DocumentContextProviderImpl.DocumentFrame;

@Registration.Singleton(DocumentContextProviderImpl.class)
public class DocumentContextProviderImpl implements
		Document.DocumentContextProvider, ContextProvider<DocumentFrame> {
	public static DocumentContextProviderImpl get() {
		return Registry.impl(DocumentContextProviderImpl.class);
	}

	@Override
	public Document contextDocument() {
		return contextInstance().document;
	}

	@Override
	public DocumentFrame contextInstance() {
		DocumentFrame frame = LooseContext.get(contextKeyFrame());
		Preconditions.checkNotNull(frame, "Document frame not registered");
		return frame;
	}

	public void register() {
		Document.registerContextProvider(this);
	}

	public void registerContextFrame(DocumentFrame frame) {
		LooseContext.set(contextKeyFrame(), frame);
	}

	@Override
	public void registerCreatedDocument(Document document) {
		contextInstance().document = document;
	}

	public void registerNewContextFrame() {
		registerContextFrame(new DocumentFrame());
	}

	private String contextKeyFrame() {
		return DocumentFrame.class.getName();
	}

	public static class DocumentFrame implements ContextFrame {
		Document document;
	}
}
