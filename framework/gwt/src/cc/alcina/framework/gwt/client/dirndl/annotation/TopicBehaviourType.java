package cc.alcina.framework.gwt.client.dirndl.annotation;

public enum TopicBehaviourType {
	EMIT, RECEIVE, ACTIVATION;

	boolean isListenerTopic() {
		switch (this) {
		case RECEIVE:
		case ACTIVATION:
			return true;
		default:
			return false;
		}
	}
}