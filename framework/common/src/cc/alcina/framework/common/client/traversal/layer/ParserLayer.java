package cc.alcina.framework.common.client.traversal.layer;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;

public abstract class ParserLayer<S extends Selection> extends Layer<S> {
	public ParserLayer(Class<S> input, Class<? extends Selection>... outputs) {
		super(input, outputs);
	}

	protected void onBeforeDetach(LayerParser layerParser) {
	}

	protected void parse(MeasureSelection selection, LayerParserPeer peer) {
		LayerParser layerParser = new LayerParser(selection, peer);
		layerParser.parse();
		layerParser.selectMatches();
		onBeforeDetach(layerParser);
		// remove dom document refs (allow gc)
		layerParser.detachMeasures();
	}
}
