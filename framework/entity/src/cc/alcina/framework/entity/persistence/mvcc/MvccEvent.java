package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Map;

import cc.alcina.framework.common.client.domain.TransactionId;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.projection.GraphProjection;

public class MvccEvent {
	public MvccObject domainIdentity;

	public MvccEvent.Type type;

	public boolean writeable;

	public EntityLocator locator;

	public TransactionId fromTransaction;

	public TransactionId currentTransaction;

	public TransactionId toTransaction;

	public Map<String, String> primitiveFieldValues;

	public String threadName;

	public MvccEvent() {
	}

	MvccEvent(MvccObject domainIdentity, EntityLocator locator,
			TransactionId fromTransaction, TransactionId currentTransaction,
			TransactionId toTransaction,
			Map<String, String> primitiveFieldValues, MvccEvent.Type type,
			boolean writeable) {
		this.domainIdentity = domainIdentity;
		this.locator = locator;
		this.fromTransaction = fromTransaction;
		this.currentTransaction = currentTransaction;
		this.toTransaction = toTransaction;
		this.primitiveFieldValues = primitiveFieldValues;
		this.type = type;
		this.writeable = writeable;
		this.threadName = Thread.currentThread().getName();
	}

	@Override
	public String toString() {
		return GraphProjection.fieldwiseToStringOneLine(this);
	}

	public String toMultilineString() {
		FormatBuilder format = new FormatBuilder();
		format.separator("\n");
		format.appendIfNotBlankKv("threadName", threadName);
		format.appendIfNotBlankKv("type", type);
		format.appendIfNotBlankKv("writeable", writeable);
		format.appendIfNotBlankKv("currentTransaction", currentTransaction);
		format.appendIfNotBlankKv("fromTransaction", fromTransaction);
		format.appendIfNotBlankKv("toTransaction", toTransaction);
		format.separator("");
		format.indent(1);
		primitiveFieldValues.keySet().stream().sorted().forEach(k -> format
				.line("[kv] %s: %s", k, primitiveFieldValues.get(k)));
		return format.toString();
	}

	public enum Type {
		VERSIONS_CREATION, VERSION_CREATION, VERSION_REMOVAL, VERSIONS_REMOVAL,
		END
	}
}