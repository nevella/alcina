package cc.alcina.framework.servlet.component.test.client;

import com.google.gwt.dom.client.Document;

import cc.alcina.framework.common.client.dom.DomNode;
import cc.alcina.framework.common.client.util.Ax;

class TestLocationMutation {
	void run() {
		AlcinaGwtTestClient.Utils.clearRootPanel();
		DomNode bodyNode = Document.get().getBody().asDomNode();
		DomNode t1 = bodyNode.builder().tag("t1").append();
		DomNode t1_1 = t1.builder().text("bruce").append();
		DomNode t1_2 = t1.builder().tag("brusque").text("brunch").append();
		Ax.out("t1_1 treeIndex pre-previousSiblingInsert: %s",
				t1_1.asLocation().getTreeIndex());
		t1_1.builder().tag("bruce-pre").text("pre").insertBeforeThis();
		Ax.out("t1_1 treeIndex post-previousSiblingInsert: %s",
				t1_1.asLocation().getTreeIndex());
	}
}
