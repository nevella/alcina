package cc.alcina.framework.servlet.component.gallery.home;

import cc.alcina.framework.servlet.component.gallery.GalleryPlace;

/**
 * 
 * 
 */
public class GalleryHomePlace extends GalleryPlace {
	@Override
	public GalleryHomePlace copy() {
		return (GalleryHomePlace) super.copy();
	}

	public static class Tokenizer
			extends GalleryPlace.Tokenizer<GalleryHomePlace> {
	}

	@Override
	public String getDescription() {
		return "Gallery Home";
	}
}
