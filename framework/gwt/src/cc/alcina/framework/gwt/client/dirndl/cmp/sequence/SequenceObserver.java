package cc.alcina.framework.gwt.client.dirndl.cmp.sequence;

import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.util.ListenerReference;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.LogUtil;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence.SequenceGenerationComplete;
import cc.alcina.framework.servlet.LifecycleService;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.sequence.SequenceBrowser;
import cc.alcina.framework.servlet.component.traversal.TraversalObserver;

/**
 * <p>
 * This class observes and retains references to in-memory sequence generation
 * events (sequences can either be persistent or in-memory)
 * 
 * @see TraversalObserver
 * 
 */
@Registration.Singleton
public class SequenceObserver extends LifecycleService.AlsoDev {
	public static final Configuration.Key KEY_ENABLED = Configuration
			.key("enabled");

	public static SequenceObserver get() {
		return Registry.impl(SequenceObserver.class);
	}

	public RemoteComponentObservables<Sequence> observables;

	public SequenceObserver() {
		observables = new RemoteComponentObservables<>(
				SequenceBrowser.Component.class, Sequence.class,
				Sequence::getName, Configuration.getInt("evictionMinutes")
						* TimeConstants.ONE_MINUTE_MS);
	}

	public void observe() {
		new SequenceGenerationCompleteObserver().bind();
		observables.observe();
	}

	class SequenceGenerationCompleteObserver
			implements ProcessObserver<SequenceGenerationComplete> {
		@Override
		public void topicPublished(SequenceGenerationComplete message) {
			observables.publish(null, message.sequence);
			observables.publish(message.sequence.getUid(), message.sequence);
		}
	}

	@Override
	public void onApplicationStartup() {
		if (KEY_ENABLED.is()) {
			observe();
		}
	}

	void evict(Sequence sequence) {
		observables.evict(sequence.getUid());
	}

	public static class ObservedSequenceLoader implements Sequence.Loader,
			TopicListener<RemoteComponentObservables<Sequence>.ObservableEntry> {
		static Pattern locationPattern = Pattern.compile("location_(.*)");

		class ParsedLocation {
			boolean matches;

			String locationPart;

			ParsedLocation(String location) {
				Matcher matcher = locationPattern.matcher(location);
				if (matcher.matches()) {
					this.matches = true;
					locationPart = matcher.group(1);
					if (locationPart.isEmpty()) {
						locationPart = null;
					}
				}
			}
		}

		@Override
		public boolean handlesSequenceLocation(String location) {
			return new ParsedLocation(location).matches;
		}

		ListenerReference subscribe(String sequenceKey,
				TopicListener<RemoteComponentObservables<Sequence>.ObservableEntry> subscriber) {
			return SequenceObserver.get().observables.subscribe(sequenceKey,
					subscriber);
		}

		CountDownLatch completionLatch = new CountDownLatch(1);

		Sequence<?> observedSequence;

		@Override
		public Sequence<?> load(String location) {
			String locationPart = new ParsedLocation(location).locationPart;
			subscribe(locationPart, this);
			try {
				LogUtil.classLogger().info("Awaiting sequence publication: {}",
						locationPart);
				completionLatch.await();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
			return observedSequence;
		}

		@Override
		public void topicPublished(
				RemoteComponentObservables<Sequence>.ObservableEntry entry) {
			observedSequence = entry.getObservable();
			completionLatch.countDown();
		}
	}

	void observableObserved(Sequence<?> sequence) {
		observables.observed(sequence.getUid());
	}
}
