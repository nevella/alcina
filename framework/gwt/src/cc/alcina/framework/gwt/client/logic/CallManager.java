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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cc.alcina.framework.common.client.util.TopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.GlobalTopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * 
 * @author Nick Reddel
 */
public class CallManager {
	public static final String TOPIC_CALL_MADE = CallManager.class.getName()
			+ "::TOPIC_CALL_MADE";

	private ArrayList<AsyncCallback> cancelled;

	private Map<AsyncCallback, String> displayTexts;

	private Map<AsyncCallback, TopicListener> topicListeners;

	private ArrayList<AsyncCallback> running;

	private static CallManager theInstance;

	public static CallManager get() {
		if (theInstance == null) {
			theInstance = new CallManager();
		}
		return theInstance;
	}

	private CallManager() {
		super();
		cancelled = new ArrayList<AsyncCallback>();
		displayTexts = new HashMap<AsyncCallback, String>();
		topicListeners = new HashMap<AsyncCallback, TopicPublisher.TopicListener>();
		running = new ArrayList<AsyncCallback>();
	}

	public void appShutdown() {
		theInstance = null;
	}

	public void cancel(AsyncCallback runningSearch) {
		cancelled.add(runningSearch);
		updateDisplay();
	}

	@SuppressWarnings("unchecked")
	public void clear() {
		ArrayList<AsyncCallback> clone = (ArrayList<AsyncCallback>) running
				.clone();
		for (AsyncCallback asyncCallback : clone) {
			remove(asyncCallback);
		}
	}

	public void completed(AsyncCallback ac) {
		remove(ac);
	}

	public boolean isCancelled(AsyncCallback ac) {
		boolean yep = cancelled.contains(ac);
		return yep;
	}

	public boolean isRunning(AsyncCallback ac) {
		return running.contains(ac) && !cancelled.contains(ac);
	}

	public void register(AsyncCallback callback, String displayText) {
		running.add(callback);
		displayTexts.put(callback, displayText);
		updateDisplay();
	}

	public void register(AsyncCallback callback, String displayText,
			TopicListener<String> callbackTopicListener) {
		topicListeners.put(callback, callbackTopicListener);
		register(callback, displayText);
	}

	private void remove(AsyncCallback callback) {
		cancelled.remove(callback);
		running.remove(callback);
		displayTexts.remove(callback);
		topicListeners.remove(callback);
		updateDisplay();
	}

	private void updateDisplay() {
		String message = null;
		TopicListener topicListener = null;
		if (running.size() != 0) {
			AsyncCallback topRunning = running.get(running.size() - 1);
			message = displayTexts.get(topRunning);
			topicListener = topicListeners.get(topRunning);
		}
		if (topicListener != null) {
			topicListener.topicPublished(TOPIC_CALL_MADE, message);
		} else {
			GlobalTopicPublisher.get().publishTopic(TOPIC_CALL_MADE, message);
		}
	}
}
