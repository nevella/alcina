package cc.alcina.framework.servlet.component.gallery.model.sequencearea;

import java.io.File;
import java.net.URL;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.servlet.component.gallery.GalleryPlace;
import cc.alcina.framework.servlet.component.sequence.adapter.FlightEventSequence;

public class GallerySequenceAreasReportPlace extends GalleryPlace {
	public SequencePlace sequencePlace = new SequencePlace();

	@Override
	public GallerySequenceAreasReportPlace copy() {
		return (GallerySequenceAreasReportPlace) super.copy();
	}

	public GallerySequenceAreasReportPlace() {
		if (Ax.isTest()) {
			try {
				sequencePlace = new SequencePlace();
				URL resource = getClass().getResource("flight-events.zip");
				String path = new File(resource.toURI()).getPath();
				sequencePlace.instanceQuery = FlightEventSequence
						.createInstanceQuery(path);
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}
	}

	public GallerySequenceAreasReportPlace(SequencePlace sequencePlace) {
		this.sequencePlace = sequencePlace;
	}

	public static class Tokenizer
			extends GalleryPlace.Tokenizer<GallerySequenceAreasReportPlace> {
	}

	@Override
	public String getDescription() {
		return "List + view sequence data";
	}
}
