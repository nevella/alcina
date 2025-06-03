package cc.alcina.framework.servlet.component.gallery.model.treetable;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyOrder;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.impl.form.FmsContentCells;
import cc.alcina.framework.gwt.client.dirndl.model.Tree;
import cc.alcina.framework.gwt.client.dirndl.model.TreeTable;
import cc.alcina.framework.servlet.component.gallery.GalleryContents;

/*
@formatter:off

- TODO - implement, test
 * @formatter:on
 */
@Registration({ GalleryContents.class, TreeTableGalleryPlace.class })
@TypedProperties
@Directed(tag = "tree-table-area")
class TreeTableArea extends GalleryContents<TreeTableGalleryPlace> {
	@TypedProperties
	static class Attributes extends Bindable.Fields.All {
		Integer limbs;

		String description;

		String colour;
	}

	class TreeOfLifeNode extends Tree.TreeNode<TreeOfLifeNode> {
		TreeOfLifeNode(TreeOfLifeNode parent, String name) {
			super(parent, name);
			setContents(new Attributes());
		}

		@Override
		public Attributes getContents() {
			return super.getContents();
		}
	}

	@DirectedContextResolver(FmsContentCells.FmsCellsContextResolver.TreeTableResolver.class)
	TreeTable treeTable;

	TreeTableArea() {
		TreeOfLifeNode root = new TreeOfLifeNode(null, "");
		root.getContents().colour = "green";
		TreeOfLifeNode eukaryotes = new TreeOfLifeNode(root, "eukaryotes");
		eukaryotes.getContents().description = "complex structured life things";
		TreeOfLifeNode plants = new TreeOfLifeNode(eukaryotes, "plants");
		TreeOfLifeNode clover = new TreeOfLifeNode(plants, "clover");
		clover.getContents().colour = "green";
		TreeOfLifeNode animals = new TreeOfLifeNode(eukaryotes, "animals");
		TreeOfLifeNode celaphopods = new TreeOfLifeNode(animals, "celaphopods");
		TreeOfLifeNode octopus = new TreeOfLifeNode(celaphopods, "octopus");
		octopus.getContents().limbs = 8;
		Tree<TreeOfLifeNode> tree = new Tree<>();
		tree.setRoot(root);
		root.stream(true).forEach(n -> n.setOpen(true));
		treeTable = new TreeTable(tree, Attributes.class);
	}
}
