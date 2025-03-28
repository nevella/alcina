package cc.alcina.framework.servlet.component.gallery;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.IfNotEqual;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.component.gallery.GalleryBrowser.Ui;

@TypedProperties
public abstract class GalleryContents<RP extends GalleryPlace> extends Model.All
		implements Registration.AllSubtypes, IfNotEqual {
	static PackageProperties._GalleryContents properties = PackageProperties.galleryContents;

	@Directed.Exclude
	public RP place;

	@Override
	public int hashCode() {
		return 1;
	}

	public GalleryContents() {
		bindings().from(Ui.get()).on(Ui.properties.place).to(this)
				.on(properties.place).oneWay();
	}

	/*
	 * All place updates should be handled by the GalleryContents subtype, _if_
	 * the place type is unchanged
	 */
	@Override
	public boolean equals(Object obj) {
		return obj != null && obj.getClass() == getClass();
	}

	static <RP extends GalleryPlace> GalleryContents<RP> forPlace(RP place) {
		GalleryContents<RP> result = Registry.impl(GalleryContents.class,
				place.getClass());
		return result;
	}
}
