package cc.alcina.extras.dev.console.alcina;

import cc.alcina.framework.servlet.component.sequence.adapter.FlightEventSequence;
import cc.alcina.framework.servlet.component.sequence.adapter.JobEventSequence;
import cc.alcina.framework.servlet.component.sequence.adapter.MvccEventSequence;
import cc.alcina.framework.servlet.process.observer.job.JobHistory;
import cc.alcina.framework.servlet.process.observer.mvcc.MvccHistory;

public class AlcinaSequenceLoaders {
	public static class MvccEvent extends FlightEventSequence.AbstractLoader {
		public MvccEvent() {
			super(MvccHistory.LOCAL_PATH, "mvcc-event-latest", s -> s,
					new MvccEventSequence());
		}

		@Override
		public boolean handlesSequenceLocation(String location) {
			return location.equals("mvcc");
		}
	}

	public static class JobEvent extends FlightEventSequence.AbstractLoader {
		public JobEvent() {
			super(JobHistory.LOCAL_PATH, "job-event-latest", s -> s,
					new JobEventSequence());
		}

		@Override
		public boolean handlesSequenceLocation(String location) {
			return location.equals("job");
		}
	}

	public static class JobEvent2 extends FlightEventSequence.AbstractLoader {
		public JobEvent2() {
			super("/tmp/sequence/job2", "job-event-2", s -> s,
					new JobEventSequence());
		}

		@Override
		public boolean handlesSequenceLocation(String location) {
			return location.equals("job2");
		}
	}

	public static class FlightExtract
			extends FlightEventSequence.AbstractLoader {
		public FlightExtract() {
			super("/tmp/flight-event/extract", "flight-extract", s -> s,
					new FlightEventSequence());
		}

		@Override
		public boolean handlesSequenceLocation(String location) {
			return location.equals("flight");
		}
	}
}