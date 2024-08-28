package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Date;
import java.util.Map;

import cc.alcina.framework.common.client.domain.TransactionId;
import cc.alcina.framework.common.client.logic.domain.IdOrdered;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.IdCounter;
import cc.alcina.framework.entity.projection.GraphProjection;

@Bean(PropertySource.FIELDS)
public class MvccEvent implements IdOrdered<MvccEvent> {
	static transient IdCounter idCounter = new IdCounter();

	public transient MvccObject domainIdentity;

	public boolean writeable;

	public EntityLocator locator;

	public TransactionId fromTransaction;

	public TransactionId currentTransactionId;

	public TransactionPhase currentTransactionPhase;

	public TransactionId toTransaction;

	public Map<String, String> primitiveFieldValues;

	public String threadName;

	public int versionIdentityHashCode;

	public Date date;

	public long id;

	public String type;

	/*
	 * populated by history code
	 */
	public int versionId;

	public MvccEvent() {
	}

	MvccEvent(MvccObject domainIdentity, EntityLocator locator,
			TransactionId fromTransaction, Transaction currentTransaction,
			TransactionId toTransaction,
			Map<String, String> primitiveFieldValues, String type,
			boolean writeable, int versionIdentityHashCode) {
		this.domainIdentity = domainIdentity;
		this.locator = locator;
		this.fromTransaction = fromTransaction;
		this.currentTransactionId = currentTransaction.getId();
		this.currentTransactionPhase = currentTransaction.phase;
		this.toTransaction = toTransaction;
		this.primitiveFieldValues = primitiveFieldValues;
		this.writeable = writeable;
		this.versionIdentityHashCode = versionIdentityHashCode;
		this.threadName = Thread.currentThread().getName();
		this.type = type;
		this.date = new Date();
		this.id = idCounter.nextId();
	}

	public long getId() {
		return id;
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
		format.appendIfNotBlankKv("versionId", versionId);
		format.appendIfNotBlankKv("domainIdentity", versionId == 0);
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
}