package cc.alcina.extras.webdriver.gallery;

import java.util.List;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;

class GallerySnapshot {
	List<GalleryPersister.Image> images;

	StringMap metadata;

	public GallerySnapshot() {
	}

	GallerySnapshot(List<GalleryPersister.Image> images, StringMap metadata) {
		this.images = images;
		this.metadata = metadata;
	}

	@Override
	public String toString() {
		return Ax.format("Build: %s -- Timestamp: %s", metadata.get("Build"),
				metadata.get("Timestamp (UTC)"));
	}
}