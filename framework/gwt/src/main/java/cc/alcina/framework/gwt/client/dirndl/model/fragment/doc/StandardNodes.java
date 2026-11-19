package cc.alcina.framework.gwt.client.dirndl.model.fragment.doc;

public class StandardNodes {
	// FIXME - although a block (in AKN), just treat this (for the moment)
	// as a single PCDATA container
	public static class LongTitle extends FragmentDocumentNode
			implements Block.Simple {
	}

	public static class ShortTitle extends FragmentDocumentNode
			implements Block.Simple {
	}

	public static class DocProponent extends FragmentDocumentNode
			implements Block.Simple {
	}

	public static class DocTitle extends FragmentDocumentNode
			implements Block.Simple {
	}
}
