package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.gwt.client.dirndl.behaviour.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.TopicEvent;

public class TreeEvents {
	public static class NodeLabelClicked
			extends TopicEvent<Object, NodeLabelClicked.Handler> {
		@Override
		public void dispatch(NodeLabelClicked.Handler handler) {
			handler.onNodeLabelClicked(this);
		}

		@Override
		public Class<NodeLabelClicked.Handler> getHandlerClass() {
			return NodeLabelClicked.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onNodeLabelClicked(NodeLabelClicked event);
		}
	}

	public static class NodeToggleButtonClicked
			extends TopicEvent<Tree.TreeNode, NodeToggleButtonClicked.Handler> {
		@Override
		public void dispatch(NodeToggleButtonClicked.Handler handler) {
			handler.onNodeToggleButtonClicked(this);
		}

		@Override
		public Class<NodeToggleButtonClicked.Handler> getHandlerClass() {
			return NodeToggleButtonClicked.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onNodeToggleButtonClicked(NodeToggleButtonClicked event);
		}
	}

	public static class PaginatorVisible
			extends TopicEvent<Tree.TreeNode, PaginatorVisible.Handler> {
		@Override
		public void dispatch(PaginatorVisible.Handler handler) {
			handler.onPaginatorVisible(this);
		}

		@Override
		public Class<PaginatorVisible.Handler> getHandlerClass() {
			return PaginatorVisible.Handler.class;
		}

		public interface Handler extends NodeEvent.Handler {
			void onPaginatorVisible(PaginatorVisible event);
		}
	}
}
