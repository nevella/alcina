package cc.alcina.framework.servlet.component.gallery;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/* TODO: delegate to the place */
@TypedProperties
@Directed(tag = "gallery-area")
class GalleryArea extends Model.Fields {
	static PackageProperties._GalleryArea properties = PackageProperties.galleryArea;

	@Directed(className = "gallery")
	GalleryContents contents;

	GalleryPage page;

	GalleryArea(GalleryPage page) {
		this.page = page;
		bindings().from(page.ui).on(GalleryBrowser.Ui.properties.place)
				.map(GalleryContents::forPlace).to(this).on(properties.contents)
				.oneWay();
	}
}
