package cc.alcina.framework.servlet.transform.fragment.html;

import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerSelection;
import cc.alcina.framework.common.client.traversal.Layer;

/*
 * Any changes made here should emit a cleanup selection (for mutation tracking)
 * as well as performing the FragmentDocumentNode transformation
 */
public class OutputCleanupLayer extends Layer<DocumentTransformerSelection> {
	/**
	 * Customises all sublayers (they're fairly simple)
	 */
	public static class Peer {
		public boolean isNormaliseWhitespace() {
			return true;
		}

		public interface Has {
			Peer getOutputCleanupLayerPeer();
		}
	}

	public OutputCleanupLayer() {
		addChild(new OutputCleanupLiLabels());
		addChild(new OutputCleanupTocHeading());
		addChild(new OutputCleanupParas());
		addChild(new OutputCleanupNestedParas());
		addChild(new OutputCleanupBlockAnonParas());
		addChild(new OutputCleanupHeadingParas());
		addChild(new OutputCleanupInlines());
		addChild(new OutputCleanupEmptyBlockquotes());
		addChild(new OutputCleanupEmptyLis());
		addChild(new OutputCleanupEmptyTrs());
		addChild(new OutputCleanupEmptyParas());
		addChild(new OutputCleanupEmptyPositioningBlocks());
		addChild(new OutputNormaliseTextNodeWhitespace());
		addChild(new OutputCleanupRemovePagebreaksBeforeSignificantText());
		addChild(new OutputEnsureLiLabels());
		addChild(new OutputMergeAndNormaliseContiguousStyledNode());
		addChild(new OutputMergeAndNormaliseContiguousTextNode());
		/*
		 * This (normalisation) is OK multiple times
		 */
		addChild(new OutputNormaliseTextNodeWhitespace());
		addChild(new OutputCleanupTopLevelInlines.CleanupStyled());
		addChild(new OutputCleanupTopLevelInlines.CleanupText());
	}
}