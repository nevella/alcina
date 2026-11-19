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

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * FIXME - dirndl 1x3 - the topic payload should be a [Model,String] tuple - and
 * possibly reduce topics to just one (plus a variant)
 *
 * Actually - don't use in dirndl - use notificationobservable
 *
 * @author Nick Reddel
 */
@Registration.Singleton
@Registration.EnvironmentSingleton
public class MessageManager {
	public static final Topic<String> topicMessagePublished = Topic.create();

	public static final Topic<Model> topicNotificationModelPublished = Topic
			.create();

	public static final Topic<String> topicCenterMessagePublished = Topic
			.create();

	public static final Topic<String> topicIcyMessagePublished = Topic.create();

	public static final Topic<String> topicIcyCenterMessagePublished = Topic
			.create();

	public static final Topic<String> topicExceptionMessagePublished = Topic
			.create();

	public static final Topic<String> topicAppMessagePublished = Topic.create();

	public static MessageManager get() {
		return Registry.impl(MessageManager.class);
	}

	public void centerMessage(String message) {
		checkRomcom();
		topicCenterMessagePublished.publish(message);
	}

	public void exceptionMessage(String messageHtml) {
		checkRomcom();
		topicExceptionMessagePublished.publish(messageHtml);
	}

	public void icyCenterMessage(String message) {
		checkRomcom();
		topicIcyCenterMessagePublished.publish(message);
	}

	public void icyMessage(String message) {
		checkRomcom();
		topicIcyMessagePublished.publish(message);
	}

	public void showMessage(String message) {
		checkRomcom();
		topicMessagePublished.publish(message);
	}

	void checkRomcom() {
		if (Al.isRomcom()) {
			throw new UnsupportedOperationException(
					"statics are not romcom-friendly. Use notificationobservable");
		}
	}

	public void showMessage(String string, Object... args) {
		showMessage(Ax.format(string, args));
	}
}
