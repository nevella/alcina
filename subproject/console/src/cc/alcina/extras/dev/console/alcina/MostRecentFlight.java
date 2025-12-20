package cc.alcina.extras.dev.console.alcina;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.servlet.component.sequence.adapter.FlightEventSequence;

@TypeSerialization("mostrecentflight")
public class MostRecentFlight extends Sequence.AbstractLoader {
	public MostRecentFlight() {
		super(mostRecentFlightFolder(), "flight-extract", s -> s,
				new FlightEventSequence());
	}

	static String mostRecentFlightFolder() {
		File eventFolder = new File("/tmp/flight-event");
		if (!eventFolder.exists()) {
			return null;
		}
		File file = Arrays.stream(eventFolder.listFiles())
				.filter(f -> f.isDirectory()
						&& !TimeConstants.within(f.lastModified(),
								5 * TimeConstants.ONE_SECOND_MS))
				.sorted(Comparator.comparing(File::lastModified).reversed())
				.findFirst().orElse(null);
		if (file == null) {
			return null;
		}
		String path = file.getPath();
		Ax.out("Loaded most recent path [modified: %s] - %s",
				Ax.timestampYmd(new Date(new File(path).lastModified())), path);
		return path;
	}

	@Override
	public boolean handlesSequenceLocation(String location) {
		return false;
	}
}