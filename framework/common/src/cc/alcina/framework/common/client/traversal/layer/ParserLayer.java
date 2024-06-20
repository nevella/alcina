package cc.alcina.framework.common.client.traversal.layer;

import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.layer.LayerParser.ParserResults;

/**
 * This class requires that the generated selection(s) derive all needed info
 * from the DOM on construction, since the DOM references will be removed
 * 
 * Use this class when traversing a website, for single-document multiple-layer
 * parsing generally don't since measures would be reused after dom detach
 * 
 */
public abstract class ParserLayer<S extends MeasureSelection> extends Layer<S> {
	public boolean detachMeasures = true;

	protected abstract LayerParserPeer createParserPeer(S selection);

	@Override
	public void process(S selection) throws Exception {
		LayerParser layerParser = new LayerParser(selection,
				createParserPeer(selection));
		layerParser.parse();
		processParserResults(selection, layerParser.getParserResults());
		if (detachMeasures) {
			layerParser.detachMeasures();
		}
	}

	protected abstract void processParserResults(S selection,
			ParserResults parserResults);
}
