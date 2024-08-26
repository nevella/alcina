package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Date;
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

		public Date date = new Date();

		@Override
		public String toString() {
			FormatBuilder format = new FormatBuilder();
			format.line("type: %s", getClass().getSimpleName());
			format.indent(1);
			format.line("time: %s", DateStyle.TIMESTAMP_HUMAN.format(date));
			format.line(event.toMultilineString());
			return format.toString();
		}
	}

	public static class VersionsCreationEvent extends Observable {
		MvccObjectVersionsEntity<?> versions;

		VersionsCreationEvent(MvccObjectVersionsEntity<?> versions) {
			this.versions = versions;
			recordEvent(versions);
		}

		void recordEvent(MvccObjectVersionsEntity<?> versions) {
			Entity entity = versions.domainIdentity;
			EntityLocator locator = entity.toLocator();
			Entity versioned = versions.resolve(false);
			Map<String, String> primitiveFieldValues = Transactions
					.primitiveFieldValues(versioned);
			event = new MvccEvent((MvccObject) versions.domainIdentity, locator,
					null, Transaction.current().getId(), null,
					primitiveFieldValues, MvccEvent.Type.VERSIONS_CREATION,
					versions.initialWriteableTransaction != null);
		}
	}

	public static class VersionsRemovalEvent extends Observable {
		MvccObjectVersionsEntity<?> versions;

		VersionsRemovalEvent(MvccObjectVersionsEntity<?> versions) {
			this.versions = versions;
			recordEvent(versions);
		}

		void recordEvent(MvccObjectVersionsEntity<?> versions) {
			Entity entity = versions.domainIdentity;
			EntityLocator locator = entity.toLocator();
			Entity versioned = versions.domainIdentity;
			Map<String, String> primitiveFieldValues = Transactions
					.primitiveFieldValues(versioned);
			event = new MvccEvent((MvccObject) versions.domainIdentity, locator,
					null, Transaction.current().getId(), null,
					primitiveFieldValues, MvccEvent.Type.VERSIONS_REMOVAL,
					false);
		}
	}

	public static class MutationEvent extends Observable {
		MvccObjectVersionsEntity<?> versions;

		MutationEvent(MvccObjectVersionsEntity<?> versions) {
			this.versions = versions;
		}
	}
}
