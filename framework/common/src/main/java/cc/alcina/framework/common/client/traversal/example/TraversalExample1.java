package cc.alcina.framework.common.client.traversal.example;

import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.traversal.AbstractSelection;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.TraversalContext;

public class TraversalExample1 {
	public static class MySelection extends AbstractSelection<String> {
		public MySelection(Node parentNode, String value, String pathSegment) {
			super(parentNode, value, pathSegment);
		}
	}

	// must be public, since Peer is
	public static class Layer1 extends Layer<MySelection> {
		public void process(MySelection selection) {
			state.traversalContext(Layer1.Peer.Has.class).getLayer1Peer()
					.checkSelection(selection);
		}

		public static class Peer {
			void checkSelection(MySelection selection) {
				// noop
			}

			public interface Has {
				Peer getLayer1Peer();
			}
		}
	}

	public static class MyTraversalPeerBase
			implements TraversalContext, Layer1.Peer.Has {
		public Layer1.Peer getLayer1Peer() {
			return new Layer1.Peer();
		}
	}

	public static class MyTraversalPeerSpecialisation1
			extends MyTraversalPeerBase {
		public Layer1.Peer getLayer1Peer() {
			return new PeerImpl();
		}

		static class PeerImpl extends Layer1.Peer {
			void checkSelection(MySelection selection) {
				if (selection.toString().contains("bruh")) {
					throw new IllegalArgumentException("No bruh");
				}
			}
		}
	}

	public void run() {
		// create and execute a dummy traversal
		TreeProcess process = new TreeProcess(this);
		SelectionTraversal traversal = new SelectionTraversal(
				new MyTraversalPeerSpecialisation1());
		TreeProcess.Node parentNode = process.getSelectedNode();
		traversal.select(new MySelection(parentNode, "bruh", "root"));
		traversal.traverse();
	}
}
