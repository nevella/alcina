package cc.alcina.framework.gwt.client.logic;

import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.entity.ClientLogRecord;
import cc.alcina.framework.common.client.entity.UserStory;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;
import cc.alcina.framework.gwt.client.util.AtEndOfEventSeriesTimer;
import cc.alcina.framework.gwt.persistence.client.LogStore;

@ClientInstantiable
@RegistryLocation(registryPoint = UserStoryTeller.class, implementationType = ImplementationType.SINGLETON)
public abstract class UserStoryTeller
		implements TopicListener<ClientLogRecord> {
	public static UserStoryTeller get() {
		return Registry.impl(UserStoryTeller.class);
	}

	long delay=2000;
	private AtEndOfEventSeriesTimer<ClientLogRecord> seriesTimer = new AtEndOfEventSeriesTimer<>(
			2000, new Runnable() {
				@Override
				public void run() {
					publish();
				}
			}).maxDelayFromFirstAction(2000);

	protected boolean attached = false;

	protected UserStory story;

	protected boolean publishDisabled = false;

	public UserStoryTeller() {
		super();
	}

	@Override
	public void topicPublished(String key, ClientLogRecord message) {
		seriesTimer.triggerEventOccurred(message);
	}

	protected void publish() {
		if (publishDisabled) {
			return;
		}
		story.setStory(LogStore.get().dumpLogsAsString());
		persistRemote();
		Ax.out("persisted user story");
	}

	protected abstract void persistRemote();

	public void tell(String trigger) {
		if (!attached) {
			attached = true;
			LogStore.topicLogEvent().add(this);
			this.story = createUserStory();
			story.setTrigger(trigger);
			Window.addWindowClosingHandler(evt -> publish());
			publish();
		}
	}

	protected abstract UserStory createUserStory();
}