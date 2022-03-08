package cc.alcina.framework.entity.persistence.mvcc;

import java.lang.reflect.Field;
import java.util.Iterator;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.mvcc.MvccObjectVersions.MvccObjectVersionsMvccObject;
import cc.alcina.framework.entity.transform.ThreadlocalTransformManager;

public class MvccObjectVersionsEntity<T extends Entity>
		extends MvccObjectVersionsMvccObject<T> {
	private int hash;

	public MvccObjectVersionsEntity(T t, Transaction initialTransaction,
			boolean initialObjectIsWriteable) {
		super(t, initialTransaction, initialObjectIsWriteable);
	}

	public void copyIdFieldsToCurrentVersion() {
		T currentCommitted = resolve(true);
		currentCommitted.setId(domainIdentity.getId());
		currentCommitted.setLocalId(domainIdentity.getLocalId());
	}

	@Override
	/*
	 * FIXME - jdk9 - check performance and maybe remove
	 */
	public int hashCode() {
		if (hash == 0) {
			if (domainIdentity.getId() == 0
					&& domainIdentity.getLocalId() == 0) {
				hash = super.hashCode();
			} else {
				hash = domainIdentity.hashCode();
			}
			if (hash == 0) {
				hash = -1;
			}
		}
		return hash;
	}

	@Override
	public String toString() {
		try {
			T object = domainIdentity;
			Transaction transaction = null;
			synchronized (this) {
				Iterator<ObjectVersion<T>> itr = versions().values().iterator();
				if (itr.hasNext()) {
					ObjectVersion<T> firstVersion = itr.next();
					object = firstVersion.object;
					transaction = firstVersion.transaction;
				}
			}
			/*
			 * use field rather than getters to not resolve
			 */
			Field idField = SEUtilities.getFieldByName(object.getClass(), "id");
			Field localIdField = SEUtilities.getFieldByName(object.getClass(),
					"localId");
			Object id = idField.get(object);
			return Ax.format("versions: %s : base: %s/%s/%s : initial-tx: %s",
					versions().size(), object.getClass(), id,
					System.identityHashCode(object),
					transaction == null ? transaction : "base");
		} catch (Exception e) {
			return "exception..";
		}
	}

	@Override
	protected T copyObject(T mostRecentObject) {
		T result = super.copyObject(mostRecentObject);
		if (mostRecentObject == null) {
			// id is always correct for domainIdentity version
			result.setId(domainIdentity.getId());
		}
		return result;
	}

	@Override
	protected void copyObject(T fromObject, T baseObject) {
		Transactions.copyObjectFields(fromObject, baseObject);
	}

	@Override
	protected void debugNotResolved() {
		FormatBuilder fb = new FormatBuilder();
		fb.line("visibleAllTransactions: %s",
				EntityLocator.instanceLocator(visibleAllTransactions));
		fb.line("domainIdentity: %s",
				EntityLocator.instanceLocator(domainIdentity));
		logger.warn(fb.toString());
		super.debugNotResolved();
	}

	@Override
	protected void onResolveNull(boolean write) {
		if (notifyResolveNullCount > 0) {
			logger.warn("onResolveNull - {}/{}", domainIdentity.getId(),
					domainIdentity.getClass().getSimpleName());
		}
		super.onResolveNull(write);
	}

	@Override
	protected void onVersionCreation(ObjectVersion<T> version) {
		if (version.writeable) {
			ThreadlocalTransformManager.cast()
					.registerDomainObject(version.object, true);
		}
	}
}
