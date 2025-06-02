package cc.alcina.framework.servlet.component.gallery.model.treetable;

import cc.alcina.framework.servlet.component.gallery.GalleryPlace;

public class TreeTableGalleryPlace extends GalleryPlace {
	@Override
	public TreeTableGalleryPlace copy() {
		return (TreeTableGalleryPlace) super.copy();
	}

	public static class Tokenizer
			extends GalleryPlace.Tokenizer<TreeTableGalleryPlace> {
		;
	}

	@Override
	public String getDescription() {
		return "Demo tree table (tree with nodes row-renderable)";
	}
}
