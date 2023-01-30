package cc.alcina.framework.gwt.client;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.util.Topic;

public class ClientTopics {
	public static class DevMessage extends Bindable {
		public static final transient Topic<DevMessage> topic = Topic.create();

		private String category;

		private String message;

		public DevMessage() {
		}

		public DevMessage(String category, String message) {
			this.category = category;
			this.message = message;
		}

		public String getCategory() {
			return this.category;
		}

		public String getMessage() {
			return this.message;
		}

		public void publish() {
		}

		public void setCategory(String category) {
			this.category = category;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}
}
