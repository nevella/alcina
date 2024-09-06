package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Map;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.util.DateStyle;
import cc.alcina.framework.common.client.util.FormatBuilder;

public abstract class MvccObservable implements ProcessObservable {
	public MvccEvent event;

	public MvccEvent getEvent() {
		return event;
	}

	MvccObjectVersionsEntity<?> versions;

	boolean writeable;

	ObjectVersion version;

	Entity from;

	Entity to;

	MvccObservable(MvccObjectVersionsEntity<?> versions, boolean writeable) {
		this.versions = versions;
		this.writeable = writeable;
		recordEvent();
	}

	MvccObservable(MvccObjectVersionsEntity<?> versions,
			ObjectVersion version) {
		this.versions = versions;
		this.version = version;
		this.writeable = version != null && version.writeable;
		recordEvent();
	}

	// called for intra-version copy ops
	MvccObservable(MvccObjectVersionsEntity<?> versions, Object from,
			Object to) {
		this.versions = versions;
		this.to = (Entity) to;
		this.writeable = false;
		recordEvent();
		event.fromObjectIdentityHashCode = System.identityHashCode(from);
	}

	void recordEvent() {
		Entity entity = versions.domainIdentity;
		EntityLocator locator = entity.toLocator();
		Entity versioned = null;
		if (!versions.attached) {
			versioned = entity;
		} else if (to != null) {
			versioned = to;
		} else if (version != null) {
			versioned = (Entity) version.object;
		} else {
			versioned = versions.resolve(writeable);
		}
		Map<String, String> primitiveFieldValues = Transactions
				.primitiveFieldValues(versioned);
		int visibleAllTransactionsIdentityHashCode = versions.visibleAllTransactions != null
				? System.identityHashCode(versions.visibleAllTransactions)
				: 0;
		event = new MvccEvent((MvccObject) versions.domainIdentity, locator,
				null, Transaction.current(), null, primitiveFieldValues,
				getClass().getSimpleName(), writeable,
				System.identityHashCode(versioned),
				visibleAllTransactionsIdentityHashCode);
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder();
		format.line("type: %s", getClass().getSimpleName());
		format.indent(1);
		format.line("time: %s", DateStyle.TIMESTAMP_HUMAN.format(event.date));
		format.line(event.toMultilineString());
		return format.toString();
	}

	public static class VersionsCreationEvent extends MvccObservable {
		VersionsCreationEvent(MvccObjectVersionsEntity<?> versions,
				boolean writeable) {
			super(versions, writeable);
		}
	}

	public static class VersionCreationEvent extends MvccObservable {
		VersionCreationEvent(MvccObjectVersionsEntity<?> versions,
				ObjectVersion version, Object from) {
			super(versions, version);
			event.fromObjectIdentityHashCode = from != null
					? System.identityHashCode(from)
					: 0;
		}
	}

	public static class VersionsRemovalEvent extends MvccObservable {
		VersionsRemovalEvent(MvccObjectVersionsEntity<?> versions,
				boolean writeable) {
			super(versions, writeable);
		}
	}

	public static class VersionRemovalEvent extends MvccObservable {
		VersionRemovalEvent(MvccObjectVersionsEntity<?> versions,
				ObjectVersion version) {
			super(versions, version);
		}
	}

	public static class VisibleAllTransactionsUpdatedEvent
			extends MvccObservable {
		VisibleAllTransactionsUpdatedEvent(
				MvccObjectVersionsEntity<?> versions) {
			super(versions, null);
		}
	}

	public static class VersionCommittedEvent extends MvccObservable {
		VersionCommittedEvent(MvccObjectVersionsEntity<?> versions,
				boolean writeable) {
			super(versions, writeable);
		}
	}

	public static class VersionDbPersistedEvent extends MvccObservable {
		VersionDbPersistedEvent(MvccObjectVersionsEntity<?> versions,
				boolean writeable) {
			super(versions, writeable);
		}
	}

	public static class VersionCopiedToDomainIdentityEvent
			extends MvccObservable {
		VersionCopiedToDomainIdentityEvent(MvccObjectVersionsEntity<?> versions,
				Entity from, Entity to) {
			super(versions, from, to);
		}
	}

	public static class RevertDomainIdentityEvent extends MvccObservable {
		RevertDomainIdentityEvent(MvccObjectVersionsEntity<?> versions,
				Entity from, Entity to) {
			super(versions, from, to);
		}
	}
}