package cc.alcina.framework.gwt.client.logic;

import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.IUserStory;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringPair;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.gwt.client.util.EventCollator;
import cc.alcina.framework.gwt.persistence.client.LogStore;

@Reflected
@Registration.Singleton
public abstract class UserStoryTeller
		implements TopicListener<ClientLogRecord> {
	public static UserStoryTeller get() {
		return Registry.impl(UserStoryTeller.class);
	}

	public static native void tellJs(String trigger);

	/*
	 * The 4000 ms timer is so that editors can see - vaguely live - what peeps
	 * are doing
	 */
	private EventCollator<ClientLogRecord> seriesTimer = new EventCollator<>(
			4000, new Runnable() {
				@Override
				public void run() {
					publish();
				}
			}).withMaxDelayFromFirstEvent(20000);

	protected boolean listening = false;

	protected IUserStory story;

	protected boolean publishDisabled = false;

	private boolean publishing;

	private int cumulativeCharCount = 0;

	public UserStoryTeller() {
		super();
	}

	public void ensureListening() {
		if (!listening) {
			listening = true;
			LogStore.topicEventOccurred.add(this);
			this.story = createUserStory();
			AlcinaTopics.categorisedLogMessage
					.publish(new StringPair(AlcinaTopics.LOG_CATEGORY_MESSAGE,
							Ax.format("Started logging - url: %s",
									Window.Location.getHref())));
		}
	}

	public IUserStory getStory() {
		return this.story;
	}

	public native void registerWithJs();

	public void tell(String trigger) {
		ensureListening();
		ensurePublishing(trigger);
	}

	@Override
	public void topicPublished(ClientLogRecord message) {
		persistLocal();
		seriesTimer.eventOccurred(message);
	}

	private void ensurePublishing(String trigger) {
		if (!publishing) {
			publishing = true;
			story.setTrigger(trigger);
			Window.addWindowClosingHandler(evt -> publish());
			publish();
		}
	}

	protected abstract IUserStory createUserStory();

	protected int getMaxCumulativeCharCount() {
		return 300000;
	}

	protected void persistLocal() {
	}

	protected abstract void persistRemote();

	protected void publish() {
		if (publishDisabled || !publishing) {
			return;
		}
		if (cumulativeCharCount > getMaxCumulativeCharCount()) {
			return;
		}
		String contents = LogStore.get().dumpLogsAsString();
		if (contents.length() > getMaxCumulativeCharCount()) {
			return;
		}
		cumulativeCharCount += contents.length();
		story.setStory(contents);
		story.obfuscateClientEvents();
		persistRemote();
		Ax.out("persisted user story");
	}
}
