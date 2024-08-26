package cc.alcina.framework.servlet.process.observer.mvcc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.entity.persistence.mvcc.MvccObject;
import cc.alcina.framework.entity.persistence.mvcc.MvccObservables;

/**
 * A detailed history of changes to the observed mvcc object
 */
public class MvccHistory {
	public MvccObject domainIdentity;

	public List<MvccObservables.Observable> observables = new CopyOnWriteArrayList<>();

	public MvccHistory(MvccObject domainIdentity) {
		this.domainIdentity = domainIdentity;
	}

	void add(MvccObservables.Observable observable) {
		observables.add(observable);
	}

	@Override
	public String toString() {
		FormatBuilder format = new FormatBuilder();
		format.line("entity: %s", ((Entity) domainIdentity).toStringId());
		format.indent(1);
		observables.forEach(format::line);
		return format.toString();
	}
}
