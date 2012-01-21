package cc.alcina.framework.jvmclient.domaintransform;

import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager;

public class ThreadedClientTransformManager extends ClientTransformManager {
	private static ThreadLocal<Boolean> ignorePropertyChanges = new ThreadLocal() {
		protected synchronized Boolean initialValue() {
			return false;
		}
	};

	private static ThreadLocal<Boolean> replayingRemoteEvent = new ThreadLocal() {
		protected synchronized Boolean initialValue() {
			return false;
		}
	};

	@Override
	public boolean isIgnorePropertyChanges() {
		return ignorePropertyChanges.get();
	}

	@Override
	public boolean isReplayingRemoteEvent() {
		return replayingRemoteEvent.get();
	}

	@Override
	public void setIgnorePropertyChanges(boolean _ignorePropertyChanges) {
		ignorePropertyChanges.set(_ignorePropertyChanges);
	}

	@Override
	public void setReplayingRemoteEvent(boolean _replayingRemoteEvent) {
		replayingRemoteEvent.set(_replayingRemoteEvent);
	}
}
