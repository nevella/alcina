package cc.alcina.framework.servlet.component.gallery;

import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/* TODO: delegate to the place */
@TypedProperties
@Directed(tag = "gallery-area")
class GalleryArea extends Model.Fields {
	PackageProperties._GalleryArea.InstanceProperties properties() {
		return PackageProperties.galleryArea.instance(this);
	}

	@Directed(className = "gallery")
	GalleryContents contents;

	GalleryPage page;

	GalleryArea(GalleryPage page) {
		this.page = page;
		from(page.ui.subtypeProperties().place()).map(GalleryContents::forPlace)
				.to(properties().contents()).oneWay();
	}
}
