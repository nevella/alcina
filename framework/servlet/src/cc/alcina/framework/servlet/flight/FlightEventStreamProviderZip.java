package cc.alcina.framework.servlet.flight;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.flight.replay.ReplayStream;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.ZipUtil;

public class FlightEventStreamProviderZip {
	public List<FlightEvent> events;

	String eventZipPath;

	String outputFolder;

	public FlightEventStreamProviderZip(String eventZipPath) {
		this.eventZipPath = eventZipPath;
	}

	public ReplayStream getReplayStream() {
		try {
			{
				// unzip events
				Preconditions.checkState(eventZipPath.endsWith(".zip"));
				outputFolder = eventZipPath.replace(".zip", "");
				SEUtilities.deleteDirectory(new File(outputFolder), true);
				new ZipUtil().unzip(new File(outputFolder),
						new FileInputStream(eventZipPath));
			}
			{
				// compute event stream
				List<FlightEvent> events = (List) SEUtilities
						.listFilesRecursive(outputFolder, null).stream()
						.filter(f -> f.isFile())
						.map(f -> Io.read().file(f).asString())
						.<FlightEvent> map(ReflectiveSerializer::deserialize)
						.sorted().collect(Collectors.toList());
				return new ReplayStream(events);
			}
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}
}
