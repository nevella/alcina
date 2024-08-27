package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Map;

import cc.alcina.framework.common.client.domain.TransactionId;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.projection.GraphProjection;

public class MvccEvent {
	public MvccObject domainIdentity;

	public MvccEvent.Type type;

	public boolean writeable;

	public EntityLocator locator;

	public TransactionId fromTransaction;

	public TransactionId currentTransactionId;

	public TransactionPhase currentTransactionPhase;

	public TransactionId toTransaction;

	public Map<String, String> primitiveFieldValues;

	public String threadName;

	public int versionIdentityHashCode;

	public MvccEvent() {
	}

	MvccEvent(MvccObject domainIdentity, EntityLocator locator,
			TransactionId fromTransaction, Transaction currentTransaction,
			TransactionId toTransaction,
			Map<String, String> primitiveFieldValues, MvccEvent.Type type,
			boolean writeable, int versionIdentityHashCode) {
		this.domainIdentity = domainIdentity;
		this.locator = locator;
		this.fromTransaction = fromTransaction;
		this.currentTransactionId = currentTransaction.getId();
		this.currentTransactionPhase = currentTransaction.phase;
		this.toTransaction = toTransaction;
		this.primitiveFieldValues = primitiveFieldValues;
		this.type = type;
		this.writeable = writeable;
		this.versionIdentityHashCode = versionIdentityHashCode;
		this.threadName = Thread.currentThread().getName();
	}

	@Override
	public String toString() {
		return GraphProjection.fieldwiseToStringOneLine(this);
	}

	public String toMultilineString() {
		FormatBuilder format = new FormatBuilder().withKvSpace(true)
				.separator("\n");
		format.appendIfNotBlankKv("threadName", threadName);
		format.appendIfNotBlankKv("currentTransactionId", currentTransactionId);
		format.appendIfNotBlankKv("currentTransactionPhase",
				currentTransactionPhase);
		format.appendIfNotBlankKv("type", type);
		format.appendIfNotBlankKv("versionIdentityHashCode",
				versionIdentityHashCode);
		format.appendIfNotBlankKv("writeable", writeable);
		format.appendIfNotBlankKv("fromTransaction", fromTransaction);
		format.appendIfNotBlankKv("toTransaction", toTransaction);
		format.newLine();
		format.separator("");
		format.indent(1);
		primitiveFieldValues.keySet().stream().sorted().forEach(k -> {
			String value = primitiveFieldValues.get(k);
			if (value != null && value.contains("\n")) {
				value = "\n" + CommonUtils.padLinesLeft(value, "      ");
				if (value.endsWith("\n")) {
					value = value.substring(0, value.length() - 1);
				}
			}
			format.line("[kv] %s: %s", k, value);
		});
		return format.toString();
	}

	public enum Type {
		VERSIONS_CREATION, VERSION_CREATION, VERSION_REMOVAL, VERSIONS_REMOVAL,
		END
	}
}