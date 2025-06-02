package cc.alcina.framework.servlet.component.gallery.model.tree;

import cc.alcina.framework.servlet.component.gallery.GalleryPlace;

public class TreeGalleryPlace extends GalleryPlace {
	@Override
	public TreeGalleryPlace copy() {
		return (TreeGalleryPlace) super.copy();
	}

	public static class Tokenizer
			extends GalleryPlace.Tokenizer<TreeGalleryPlace> {
		;
	}

	@Override
	public String getDescription() {
		return "Demo tree (and tree model)";
	}
}
