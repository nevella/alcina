package com.google.gwt.dom.client.mutations;

import com.google.gwt.dom.client.LocalDom;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomDocument.MutableDocument;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.util.Topic;

@Registration(DomDocument.MutableDocumentDecorator.class)
public class MutableDocumentDecoratorImpl
		implements DomDocument.MutableDocumentDecorator {
	@Override
	public MutableDocument asMutableDocument(DomDocument document) {
		return new MutableDocumentImpl(document);
	}

	static class MutableDocumentImpl implements MutableDocument {
		DomDocument document;

		Topic<Void> mutationOcurred = Topic.create();

		public MutableDocumentImpl(DomDocument document) {
			this.document = document;
			// since document is a singleton, can just piggyback of
			// RemoteMutations
			LocalDom.getRemoteMutations().topicMutationOccurred
					.add(() -> mutationOcurred.signal());
		}

		@Override
		public Topic<Void> topicMutationOccurred() {
			return mutationOcurred;
		}
	}
}