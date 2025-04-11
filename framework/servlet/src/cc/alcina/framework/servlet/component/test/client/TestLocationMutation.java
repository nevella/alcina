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
		int t1_1_index_1 = t1_1.asLocation().getTreeIndex();
		Ax.out("t1_1 treeIndex pre-previousSiblingInsert: %s", t1_1_index_1);
		int t1_2_index_1 = t1_2.asLocation().getTreeIndex();
		Ax.out("t1_2 treeIndex pre-previousSiblingInsert: %s", t1_2_index_1);
		t1_1.builder().tag("bruce-pre").text("pre").insertBeforeThis();
		int t1_1_index_2 = t1_1.asLocation().getTreeIndex();
		Ax.out("t1_1 treeIndex post-previousSiblingInsert: %s", t1_1_index_2);
		int t1_2_index_2 = t1_2.asLocation().getTreeIndex();
		Ax.out("t1_2 treeIndex post-previousSiblingInsert: %s", t1_2_index_2);
	}
}
