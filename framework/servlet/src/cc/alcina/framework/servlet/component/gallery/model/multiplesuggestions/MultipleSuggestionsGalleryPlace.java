package cc.alcina.framework.servlet.component.gallery.model.multiplesuggestions;

import cc.alcina.framework.servlet.component.gallery.GalleryPlace;

public class MultipleSuggestionsGalleryPlace extends GalleryPlace {
	@Override
	public MultipleSuggestionsGalleryPlace copy() {
		return (MultipleSuggestionsGalleryPlace) super.copy();
	}

	public static class Tokenizer
			extends GalleryPlace.Tokenizer<MultipleSuggestionsGalleryPlace> {
		;
	}

	@Override
	public String getDescription() {
		return "Models multiple suggestions as an editable area";
	}
}
