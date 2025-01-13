package cc.alcina.framework.servlet.component.gallery;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;

@ReflectiveSerializer.Checks(ignore = false)
public class GallerySettings extends Bindable.Fields {
	public static GallerySettings get() {
		return GalleryBrowser.Ui.get().settings;
	}
}
