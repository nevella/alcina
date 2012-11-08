/* 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.gwt.client.logic;

import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;

/**
 * 
 * @author Nick Reddel
 */
public class MessageManager {
	public static final String TOPIC_MESSAGE_PUBLISHED = MessageManager.class
			.getName() + ".TOPIC_MESSAGE_PUBLISHED";

	public static final String TOPIC_ICY_MESSAGE_PUBLISHED = MessageManager.class
			.getName() + ".TOPIC_ICY_MESSAGE_PUBLISHED";

	public static final String TOPIC_CENTER_MESSAGE_PUBLISHED = MessageManager.class
			.getName() + ".TOPIC_CENTER_MESSAGE_PUBLISHED";

	public void showMessage(String message) {
		GlobalTopicPublisher.get().publishTopic(TOPIC_MESSAGE_PUBLISHED,
				message);
	}

	private MessageManager() {
		super();
	}

	private static MessageManager theInstance;

	public static MessageManager get() {
		if (theInstance == null) {
			theInstance = new MessageManager();
		}
		return theInstance;
	}

	public void appShutdown() {
		theInstance = null;
	}

	public void icyMessage(String message) {
		GlobalTopicPublisher.get().publishTopic(TOPIC_ICY_MESSAGE_PUBLISHED,
				message);
	}

	public void centerMessage(String message) {
		GlobalTopicPublisher.get().publishTopic(TOPIC_CENTER_MESSAGE_PUBLISHED,
				message);
	}
}
