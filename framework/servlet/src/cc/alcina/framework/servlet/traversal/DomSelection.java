package cc.alcina.framework.servlet.traversal;

import cc.alcina.framework.common.client.dom.DomDocument;
import cc.alcina.framework.common.client.dom.DomNode;

public abstract class DomSelection<D extends DomNode>
		extends AbstractSelection<D> {
	public DomSelection(Selection parent, D value, String pathSegment) {
		super(parent, value, pathSegment);
	}

	@Override
	public boolean referencesParentResources() {
		return true;
	}

	public static class Document<DC extends DomDocument>
			extends DomSelection<DC> {
		public Document(Selection parent, DC document, String pathSegment) {
			super(parent, document, pathSegment);
		}

		@Override
		public boolean referencesParentResources() {
			return false;
		}
	}
}