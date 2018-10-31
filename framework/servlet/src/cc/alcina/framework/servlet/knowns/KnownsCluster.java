package cc.alcina.framework.servlet.knowns;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.csobjects.KnownsDelta;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.KryoUtils;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.util.CachingConcurrentMap;

@RegistryLocation(registryPoint = KnownsCluster.class, implementationType = ImplementationType.SINGLETON)
public class KnownsCluster implements RegistrableService {
	public static KnownsCluster get() {
		return Registry.impl(KnownsCluster.class);
	}

	CachingConcurrentMap<String, KnownsClusterSystemElement> systemDeltas = new CachingConcurrentMap<>(
			KnownsClusterSystemElement::new, 10);

	boolean finished = false;

	@Override
	public void appShutdown() {
		finished = true;
	}

	public void start() {
		List<String> systemUrls = Arrays
				.asList(ResourceUtilities.get(KnownsCluster.class, "systemUrls")
						.split(";"))
				.stream().filter(s -> !s.isEmpty())
				.collect(Collectors.toList());
		if (systemUrls.isEmpty()) {
			return;
		}
		for (String systemUrl : systemUrls) {
			new Thread() {
				@Override
				public void run() {
					doSystemLoop(systemUrl);
				};
			}.start();
		}
	}

	protected void doSystemLoop(String systemUrl) {
		String host = systemUrl.replaceFirst("https?://(.+?)/.+", "$1");
		Thread.currentThread().setName(Ax.format("knownsCluster-%s", host));
		KnownsClusterSystemElement system = systemDeltas.get(systemUrl);
		String lastDelta = null;
		while (!finished) {
			String url = Ax.format("%s&since=%s", systemUrl,
					system.provideLastTimestamp());
			try {
				// FIXME - logging
				Ax.out("loading deltas: %s", url);
				String knownsDeltaB64 = ResourceUtilities.readUrlAsString(url);
				if (Objects.equals(system.lastDeltaB64, knownsDeltaB64)) {
					// docker caching when remote knowns server shutdown?
					try {
						Thread.sleep(1 * TimeConstants.ONE_SECOND_MS);
					} catch (Exception e2) {
						continue;
					}
				}
				system.lastDeltaB64 = knownsDeltaB64;
				KnownsDelta delta = KryoUtils.deserializeFromBase64(
						knownsDeltaB64, KnownsDelta.class);
				if (delta.added.size() != 0) {
					system.lastDelta = delta;
					synchronized (Knowns.reachableKnownsModificationNotifier) {
						Knowns.lastModified = System.currentTimeMillis();
						Knowns.reachableKnownsModificationNotifier.notifyAll();
					}
					try {
						Thread.sleep(1 * TimeConstants.ONE_SECOND_MS);
					} catch (Exception e2) {
					}
				}
			} catch (Exception e) {
				try {
					Ax.out("loading deltas failed: %s\n\t%s: %s", url,
							e.getClass().getSimpleName(), e.getMessage());
					Thread.sleep(1 * TimeConstants.ONE_MINUTE_MS);
				} catch (Exception e2) {
				}
			}
		}
	}

	static class KnownsClusterSystemElement {
		public String lastDeltaB64;

		String url;

		KnownsDelta lastDelta;

		public KnownsClusterSystemElement(String url) {
			this.url = url;
		}

		public long provideLastTimestamp() {
			return lastDelta == null ? 0L : lastDelta.timeStamp;
		}
	}
}
