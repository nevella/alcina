package cc.alcina.framework.servlet.transform.fragment.generic;

import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerPeer;

public class DocumentTransformerPeer_Generic extends DocumentTransformerPeer {
	RootLayer rootLayer;

	public DocumentTransformerPeer_Generic() {
	}

	@Override
	protected void initialiseTraversal() {
		rootLayer = new RootLayer(true);
		traversal.layers().setRoot(rootLayer);
	}
}
