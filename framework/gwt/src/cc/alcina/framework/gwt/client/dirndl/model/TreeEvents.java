package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;

public class TreeEvents {
	public static class KeyboardSelectNode
			extends ModelEvent<Object, KeyboardSelectNode.Handler> {
		@Override
		public void dispatch(KeyboardSelectNode.Handler handler) {
			handler.onKeyboardSelectNode(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onKeyboardSelectNode(KeyboardSelectNode event);
		}
	}

	public static class NodeLabelClicked
			extends ModelEvent<Object, NodeLabelClicked.Handler> {
		@Override
		public void dispatch(NodeLabelClicked.Handler handler) {
			handler.onNodeLabelClicked(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onNodeLabelClicked(NodeLabelClicked event);
		}
	}

	public static class NodeToggleButtonClicked
			extends ModelEvent<Tree.TreeNode, NodeToggleButtonClicked.Handler> {
		@Override
		public void dispatch(NodeToggleButtonClicked.Handler handler) {
			handler.onNodeToggleButtonClicked(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onNodeToggleButtonClicked(NodeToggleButtonClicked event);
		}
	}

	public static class PaginatorVisible
			extends ModelEvent<Tree.TreeNode, PaginatorVisible.Handler> {
		@Override
		public void dispatch(PaginatorVisible.Handler handler) {
			handler.onPaginatorVisible(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onPaginatorVisible(PaginatorVisible event);
		}
	}

	public static class SelectNode
			extends ModelEvent<Object, SelectNode.Handler> {
		@Override
		public void dispatch(SelectNode.Handler handler) {
			handler.onSelectNode(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onSelectNode(SelectNode event);
		}
	}
}
