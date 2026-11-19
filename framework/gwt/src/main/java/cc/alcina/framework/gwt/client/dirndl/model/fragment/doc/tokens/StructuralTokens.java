package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.tokens;

import cc.alcina.framework.gwt.client.dirndl.model.fragment.doc.Block;

public class StructuralTokens {
	public static class OmitInvalidChild
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final OmitInvalidChild TYPE = new OmitInvalidChild();

		private OmitInvalidChild() {
		}
	}

	public static class CitedLegislationBlock
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final CitedLegislationBlock TYPE = new CitedLegislationBlock();

		private CitedLegislationBlock() {
		}
	}

	public static class CitedLegislationRun
			extends FragmentDocumentOverlayToken.AbstractToken {
		public static final CitedLegislationRun TYPE = new CitedLegislationRun();

		private CitedLegislationRun() {
		}

		public static class Transformer implements
				FragmentDocumentOverlayToken.Transformer<CitedLegislationRun, Block.CitedLegislationRun> {
		}
	}
}
