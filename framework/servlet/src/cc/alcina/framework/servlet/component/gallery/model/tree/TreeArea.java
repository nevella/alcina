package cc.alcina.framework.servlet.component.gallery.model.tree;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Tree;
import cc.alcina.framework.gwt.client.dirndl.model.Tree.TreeNode.BasicNode;
import cc.alcina.framework.servlet.component.gallery.GalleryContents;

/*
@formatter:off

 * @formatter:on
 */
@Registration({ GalleryContents.class, TreeGalleryPlace.class })
@TypedProperties
@Directed(tag = "tree-area")
class TreeArea extends GalleryContents<TreeGalleryPlace> {
	Tree<BasicNode> tree;

	TreeArea() {
		BasicNode root = new BasicNode(null, "");
		BasicNode eukaryotes = new BasicNode(root, "eukaryotes");
		BasicNode plants = new BasicNode(eukaryotes, "plants");
		BasicNode clover = new BasicNode(plants, "clover");
		BasicNode animals = new BasicNode(eukaryotes, "animals");
		BasicNode celaphopods = new BasicNode(animals, "celaphopods");
		BasicNode octopus = new BasicNode(celaphopods, "octopus");
		tree = new Tree<>();
		tree.setRoot(root);
		root.stream(true).forEach(n -> n.setOpen(true));
	}
}
