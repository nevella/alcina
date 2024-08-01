package cc.alcina.framework.servlet.component.romcom.client.test;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LocalDom;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.util.Ax;

public class TestSyncMutations2 {
	public void onModuleLoad() {
		LocalDom.invokeExternal(() -> mutate());
		DomNode mutated = Document.get().getBody().asDomNode().children
				.byTag("mutated").stream().findFirst().get();
		Ax.out(mutated.textContent());
	}

	native void mutate() /*-{
		var m=$doc.createElement("mutated");
		m.innerText="korg";
		$doc.body.append(m);
	}-*/;
}
