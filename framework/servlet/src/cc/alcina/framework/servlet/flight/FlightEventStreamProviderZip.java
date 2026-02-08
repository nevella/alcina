package cc.alcina.framework.servlet.flight;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.function.Function;
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

	Function<String, String> serializationRefactoringHandler;

	public FlightEventStreamProviderZip(String eventZipPath,
			Function<String, String> serializationRefactoringHandler) {
		this.eventZipPath = eventZipPath;
		this.serializationRefactoringHandler = serializationRefactoringHandler;
	}

	public ReplayStream getReplayStream() {
		File outputFolder = null;
		try {
			{
				// unzip events
				Preconditions.checkState(eventZipPath.endsWith(".zip"));
				outputFolder = File.createTempFile("unzip", "zip");
				SEUtilities.deleteDirectory(outputFolder, false);
				outputFolder.mkdirs();
				ZipUtil.unzip(outputFolder, new FileInputStream(eventZipPath));
			}
			{
				// compute event stream
				events = (List) SEUtilities
						.listFilesRecursive(outputFolder.getPath(), null)
						.stream().filter(f -> f.isFile())
						.map(f -> Io.read().file(f).asString())
						.map(this::trackRefactoring)
						.<FlightEvent> map(ReflectiveSerializer::deserialize)
						.sorted().collect(Collectors.toList());
				return new ReplayStream(events);
			}
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		} finally {
			SEUtilities.deleteDirectory(outputFolder, false);
		}
	}

	String trackRefactoring(String serialized) {
		return serializationRefactoringHandler.apply(serialized);
	}
}
