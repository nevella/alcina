package cc.alcina.framework.common.client.flight.replay;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.ZipUtil;

public class ReplayEventDispatcher {
	String eventZipPath;

	DispatchFilter filter;

	Timing timing;

	ReplayStream replayStream;

	String outputFolder;

	ReplayEventProcessor processor;

	Logger logger = LoggerFactory.getLogger(getClass());

	public ReplayEventDispatcher(String eventZipPath,
			ReplayEventProcessor processor, DispatchFilter filter,
			Timing timing) {
		this.eventZipPath = eventZipPath;
		this.processor = processor;
		this.filter = filter;
		this.timing = timing;
		try {
			init();
		} catch (Exception e) {
			throw WrappedRuntimeException.wrap(e);
		}
	}

	void init() throws FileNotFoundException, Exception {
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
					.sorted().filter(filter::isReplayableType)
					.collect(Collectors.toList());
			replayStream = new ReplayStream(events);
		}
	}

	public void replay() {
		while (replayStream.itr.hasNext()) {
			FlightEvent next = replayStream.itr.next();
			logger.info("[replay] {}", next);
			if (!filter.isReplayable(next)) {
				continue;
			}
			timing.await(next);
			processor.replay(next);
		}
	}

	public interface DispatchFilter {
		/*
		 * An initial filter, to remove events that are not of a
		 * replayable/awaitable type
		 */
		boolean isReplayableType(FlightEvent event);

		/*
		 * During event replay, skip this particular event. It may be used as an
		 * 'await' (replay should wait until the Processor has emitted a
		 * congruent event)
		 */
		boolean isReplayable(FlightEvent event);
	}

	public interface Timing {
		void await(FlightEvent next);
	}
}
