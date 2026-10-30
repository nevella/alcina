package cc.alcina.framework.servlet.transform.fragment.generic.test;

import cc.alcina.framework.entity.Io;
import cc.alcina.framework.servlet.transform.fragment.DocumentTransformerHost;
import cc.alcina.framework.servlet.transform.fragment.generic.DocumentTransformerPeer_Generic;

public class TestFragmentDocumentConversion {
	public void run() throws Exception {
		String wordMarkup = Io.read()
				.resource("Test FragmentDocument conversion source.html")
				.asString();
		String transformed = new DocumentTransformerHost().transform(wordMarkup,
				new DocumentTransformerPeer_Generic());
		Io.log().toFile(transformed);
	}
}
