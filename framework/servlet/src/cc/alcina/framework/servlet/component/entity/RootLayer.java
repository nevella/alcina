package cc.alcina.framework.servlet.component.entity;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.servlet.component.entity.EntityBrowser.Ui.EntityPeer;
import cc.alcina.framework.servlet.component.entity.RootLayer.DomainGraphSelection;
import cc.alcina.framework.servlet.component.traversal.TraversalObserver.RootLayerNamer;

public class RootLayer extends Layer<DomainGraphSelection> {
	public RootLayer() {
		addChild(new EntityTypesLayer());
		int depth = EntityBrowser.peer().queryDepth();
		for (int idx = 1; idx < depth; idx++) {
			addChild(new EntityTraversalQueryLayer());
		}
	}

	@Registration({ RootLayerNamer.class, RootLayer.class })
	public static class NamerImpl extends RootLayerNamer<RootLayer> {
		@Override
		public String rootLayerName(RootLayer layer) {
			return "Entity graph";
		}
	}

	public static class DomainGraphSelection extends AbstractSelection {
		public DomainGraphSelection(Node parentNode, EntityPeer peer) {
			super(parentNode, peer, "domain");
		}
	}
}