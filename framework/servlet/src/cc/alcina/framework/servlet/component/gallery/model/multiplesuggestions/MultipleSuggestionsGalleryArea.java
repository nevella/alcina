package cc.alcina.framework.servlet.component.gallery.model.multiplesuggestions;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.servlet.component.gallery.GalleryContents;

/*
@formatter:off
 * TODO 
 
 - create a model (a user list enum, and a model which encapsulates that) editable via a MultipleSuggestions
 - link to the place
 - have a submit button that modifies the place
 - have a current value list 
 - link to this file from the gallery

 * @formatter:on
 */
@Registration({ GalleryContents.class, MultipleSuggestionsGalleryPlace.class })
class MultipleSuggestionsGalleryArea extends GalleryContents {
	static PackageProperties._MultipleSuggestionsGalleryArea properties = PackageProperties.multipleSuggestionsGalleryArea;

	String self = "multiple suggers";

	MultipleSuggestionsGalleryArea() {
	}
}
