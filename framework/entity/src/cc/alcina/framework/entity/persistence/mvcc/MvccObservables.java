package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Map;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.util.DateStyle;
import cc.alcina.framework.common.client.util.FormatBuilder;

/**
 * A container for observables emitted by the Mvcc system (for deep production
 * debugging)
 */
public class MvccObservables {
	public static abstract class Observable implements ProcessObservable {
		public MvccEvent event;

		public MvccEvent getEvent() {
			return event;
		}

		MvccObjectVersionsEntity<?> versions;

		boolean writeable;

		ObjectVersion version;

		Observable(MvccObjectVersionsEntity<?> versions, boolean writeable) {
			this.versions = versions;
			this.writeable = writeable;
			recordEvent();
		}

		Observable(MvccObjectVersionsEntity<?> versions,
				ObjectVersion version) {
			this.versions = versions;
			this.version = version;
			this.writeable = version.writeable;
			recordEvent();
		}

		void recordEvent() {
			Entity entity = versions.domainIdentity;
			EntityLocator locator = entity.toLocator();
			Entity versioned = version != null ? (Entity) version.object
					: versions.resolve(writeable);
			Map<String, String> primitiveFieldValues = Transactions
					.primitiveFieldValues(versioned);
			event = new MvccEvent((MvccObject) versions.domainIdentity, locator,
					null, Transaction.current(), null, primitiveFieldValues,
					getClass().getSimpleName(), writeable,
					System.identityHashCode(versioned));
		}

		@Override
		public String toString() {
			FormatBuilder format = new FormatBuilder();
			format.line("type: %s", getClass().getSimpleName());
			format.indent(1);
			format.line("time: %s",
					DateStyle.TIMESTAMP_HUMAN.format(event.date));
			format.line(event.toMultilineString());
			return format.toString();
		}
	}

	public static class VersionsCreationEvent extends Observable {
		VersionsCreationEvent(MvccObjectVersionsEntity<?> versions,
				boolean writeable) {
			super(versions, writeable);
		}
	}

	public static class VersionCreationEvent extends Observable {
		VersionCreationEvent(MvccObjectVersionsEntity<?> versions,
				ObjectVersion version) {
			super(versions, version);
			if (Transaction.current()
					.getPhase() == TransactionPhase.VACUUM_BEGIN) {
				int debug = 3;
			}
		}
	}

	public static class VersionsRemovalEvent extends Observable {
		VersionsRemovalEvent(MvccObjectVersionsEntity<?> versions,
				boolean writeable) {
			super(versions, writeable);
		}
	}

	public static class VersionRemovalEvent extends Observable {
		VersionRemovalEvent(MvccObjectVersionsEntity<?> versions,
				ObjectVersion version) {
			super(versions, version);
		}
	}

	public static class VersionCommittedEvent extends Observable {
		VersionCommittedEvent(MvccObjectVersionsEntity<?> versions,
				boolean writeable) {
			super(versions, writeable);
		}
	}

	public static class VersionDbPersistedEvent extends Observable {
		VersionDbPersistedEvent(MvccObjectVersionsEntity<?> versions,
				boolean writeable) {
			super(versions, writeable);
		}
	}
	// public static class MutationEvent extends Observable {
	// MvccObjectVersionsEntity<?> versions;
	// MutationEvent(MvccObjectVersionsEntity<?> versions) {
	// this.versions = versions;
	// }
	// }
}
