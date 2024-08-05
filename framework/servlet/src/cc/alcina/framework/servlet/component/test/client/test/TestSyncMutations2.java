package cc.alcina.framework.servlet.component.test.client.test;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LocalDom;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.gwt.client.util.ClientUtils;

public class TestSyncMutations2 {
	public void onModuleLoad() {
		LocalDom.invokeExternal(() -> mutate());
		DomNode mutated = Document.get().getBody().asDomNode().children
				.byTag("mutated").stream().findFirst().get();
		Preconditions.checkState(mutated.textMatches("korg"));
		ClientUtils.consoleInfo("Mutated node text content: '%s'",
				mutated.textContent());
		ClientUtils.consoleInfo("   [TestSyncMutations2] Passed");
	}

	native void mutate() /*-{
		var m=$doc.createElement("mutated");
		m.innerText="korg";
		$doc.body.append(m);
	}-*/;
}
