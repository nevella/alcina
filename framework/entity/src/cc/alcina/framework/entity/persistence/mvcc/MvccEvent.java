package cc.alcina.framework.entity.persistence.mvcc;

import java.util.Date;
import java.util.Map;

import cc.alcina.framework.common.client.domain.TransactionId;
import cc.alcina.framework.common.client.logic.domain.IdOrdered;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.projection.GraphProjection;

@Bean(PropertySource.FIELDS)
public class MvccEvent implements IdOrdered<MvccEvent> {
	public transient MvccObject domainIdentity;

	public boolean writeable;

	public EntityLocator locator;

	public TransactionId fromTransaction;

	public TransactionId currentTransactionId;

	public TransactionPhase currentTransactionPhase;

	public TransactionId toTransaction;

	public Map<String, String> primitiveFieldValues;

	public String threadName;

	public int versionObjectIdentityHashCode;

	public int visibleAllTransactionsIdentityHashCode;

	public int fromObjectIdentityHashCode;

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
			boolean writeable, int versionObjectIdentityHashCode,
			int visibleAllTransactionsIdentityHashCode) {
		this.domainIdentity = domainIdentity;
		this.visibleAllTransactionsIdentityHashCode = visibleAllTransactionsIdentityHashCode;
		this.locator = locator;
		this.fromTransaction = fromTransaction;
		this.currentTransactionId = currentTransaction.getId();
		this.currentTransactionPhase = currentTransaction.phase;
		this.toTransaction = toTransaction;
		this.primitiveFieldValues = primitiveFieldValues;
		this.writeable = writeable;
		this.versionObjectIdentityHashCode = versionObjectIdentityHashCode;
		this.threadName = Thread.currentThread().getName();
		this.type = type;
		this.date = new Date();
		this.id = ProcessObservable.Id.nextId();
	}

	@Override
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
		if (fromObjectIdentityHashCode != 0) {
			format.appendIfNotBlankKv("fromObjectIdentityHashCode",
					fromObjectIdentityHashCode);
		}
		if (visibleAllTransactionsIdentityHashCode != 0) {
			format.appendIfNotBlankKv("visibleAllTransactionsIdentityHashCode",
					visibleAllTransactionsIdentityHashCode);
		}
		format.appendIfNotBlankKv("versionId", versionId);
		format.appendIfNotBlankKv("versionObjectIdentityHashCode",
				versionObjectIdentityHashCode);
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