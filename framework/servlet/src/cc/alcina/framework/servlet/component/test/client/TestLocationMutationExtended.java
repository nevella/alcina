package cc.alcina.framework.servlet.component.test.client;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.Document;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.gwt.client.util.ClientUtils;

class TestLocationMutationExtended {
	void run() {
		AlcinaGwtTestClient.Utils.clearRootPanel();
		DomNode bodyNode = Document.get().getBody().asDomNode();
		DomNode t1 = bodyNode.builder().tag("t1").append();
		DomNode t1_1 = t1.builder().text("bruce").append();
		bodyNode.document.locations().validateLocations();
		int t1_1_attachId = t1_1.gwtNode().getAttachId();
		t1.strip();
		int t1_1_attachId_postStrip = t1_1.gwtNode().getAttachId();
		Preconditions.checkState(t1_1_attachId == t1_1_attachId_postStrip);
		bodyNode.document.locations().validateLocations();
		DomNode t2 = t1_1.builder().tag("t2").wrap();
		int t1_1_attachId_postWrap = t1_1.gwtNode().getAttachId();
		Preconditions.checkState(t1_1_attachId == t1_1_attachId_postWrap);
		bodyNode.document.locations().validateLocations();
		ClientUtils.consoleInfo("   [TestLocationMutationExtended] Passed");
	}
}
