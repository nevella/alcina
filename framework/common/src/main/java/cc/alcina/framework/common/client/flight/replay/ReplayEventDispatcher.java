package cc.alcina.framework.common.client.flight.replay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.flight.FlightEvent;

public class ReplayEventDispatcher {
	DispatchFilter filter;

	Timing timing;

	ReplayStream replayStream;

	public ReplayEventProcessor processor;

	Logger logger = LoggerFactory.getLogger(getClass());

	public ReplayEventDispatcher(ReplayStream replayStream,
			ReplayEventProcessor processor, DispatchFilter filter,
			Timing timing) {
		this.replayStream = replayStream;
		this.processor = processor;
		this.filter = filter;
		this.timing = timing;
	}

	public void replay() {
		while (replayStream.itr.hasNext()) {
			FlightEvent next = replayStream.itr.next();
			if (!filter.isReplayable(next)) {
				continue;
			}
			logger.info("[replay] {}", next);
			timing.await(next);
			processor.replay(next);
		}
	}

	public interface DispatchFilter {
		/*
		 * During event replay, skip this particular event (if false). It may be
		 * used as an 'await' (replay should wait until the Processor has
		 * emitted a congruent event)
		 */
		boolean isReplayable(FlightEvent event);
	}

	public interface Timing {
		void await(FlightEvent next);
	}
}
