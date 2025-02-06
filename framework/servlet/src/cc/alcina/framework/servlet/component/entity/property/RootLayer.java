package cc.alcina.framework.servlet.component.entity.property;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.servlet.component.entity.property.PropertyFilterParser.Query;
import cc.alcina.framework.servlet.component.traversal.TraversalObserver.RootLayerNamer;

/*
 * Passes the input selection to the child sequence, on completion converts
 * parsed clauses to output
 */
public class RootLayer extends Layer<Query> {
	public RootLayer() {
		addChild(new DocumentLayer());
		addChild(new QueryPartLayer());
	}

	@Registration({ RootLayerNamer.class, RootLayer.class })
	public static class NamerImpl extends RootLayerNamer<RootLayer> {
		@Override
		public String rootLayerName(RootLayer layer) {
			return "Recipe parser";
		}
	}
}