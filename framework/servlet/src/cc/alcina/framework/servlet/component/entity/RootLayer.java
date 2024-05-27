package cc.alcina.framework.servlet.component.entity;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.servlet.component.entity.EntityGraphView.Ui.EntityPeer;
import cc.alcina.framework.servlet.component.entity.RootLayer.DomainGraphSelection;
import cc.alcina.framework.servlet.component.traversal.TraversalHistories.RootLayerNamer;

public class RootLayer extends Layer<DomainGraphSelection> {
	public RootLayer() {
		addChild(new EntityTypesLayer());
		addChild(new EntityTypeLayer());
		int depth = EntityGraphView.peer().queryDepth();
		for (int idx = 2; idx < depth; idx++) {
			addChild(new QueryLayer());
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