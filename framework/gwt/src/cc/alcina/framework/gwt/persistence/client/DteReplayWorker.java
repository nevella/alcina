package cc.alcina.framework.gwt.persistence.client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import cc.alcina.framework.common.client.logic.MutablePropertyChangeSupport;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;

import com.google.gwt.core.client.Scheduler.RepeatingCommand;

public class DteReplayWorker implements RepeatingCommand {
	private Iterator<DomainTransformEvent> iterator;

	public DteReplayWorker(Collection<DomainTransformEvent> items) {
		this.iterator = items.iterator();
	}

	@Override
	public boolean execute() {
		MutablePropertyChangeSupport.setMuteAll(true);
		List<DomainTransformEvent> slice = new ArrayList<DomainTransformEvent>();
		for (int count = 0; count < 50 && iterator.hasNext(); count++) {
			slice.add(iterator.next());
		}
		TransformManager.get().replayRemoteEvents(slice, false);
		MutablePropertyChangeSupport.setMuteAll(false);
		return iterator.hasNext();
	}
}