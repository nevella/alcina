package cc.alcina.framework.servlet.component.entity;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

import com.google.gwt.dom.client.StyleInjector;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.TraversalContext;
import cc.alcina.framework.common.client.traversal.layer.SelectionMarkup;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor.AppSuggestor.AnswerSupplier;
import cc.alcina.framework.servlet.component.entity.EntityGraphView.Ui.EntityPeer;
import cc.alcina.framework.servlet.component.entity.RootLayer.DomainGraphSelection;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import cc.alcina.framework.servlet.component.traversal.TraversalHistories.TraversalDoesNotPublishNullObservable;
import cc.alcina.framework.servlet.component.traversal.TraversalProcessView;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.place.TraversalPlace.SelectionPath;
import cc.alcina.framework.servlet.dom.RemoteUi;
import cc.alcina.framework.servlet.job.JobContext;

public class EntityGraphView {
	@Registration(RemoteComponent.class)
	public static class Component implements RemoteComponent {
		@Override
		public String getPath() {
			return "/entity";
		}

		@Override
		public Class<? extends RemoteUi> getUiType() {
			return EntityGraphView.Ui.class;
		}
	}

	static EntityPeer peer() {
		return Ui.cast().peer;
	}

	public static class Ui extends TraversalProcessView.Ui {
		String traversalId;

		static CountDownLatch loadedLatch = new CountDownLatch(1);
		static {
			DomainStore.topicStoreLoadingComplete
					.addWithPublishedCheck(loadedLatch::countDown);
		}

		public Ui() {
		}

		public static Ui cast() {
			return (Ui) get();
		}

		@Override
		public void injectCss(String relativePath) {
			StyleInjector.injectNow(
					Io.read().relativeTo(TraversalProcessView.Ui.class)
							.resource(relativePath).asString());
		}

		public String getMainCaption() {
			return "Entity graph";
		}

		EntityPeer peer;

		@Override
		public void onBeforeEnterFrame() {
			try {
				loadedLatch.await();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
			Transaction.ensureBegun();
		}

		@Override
		public void onExitFrame() {
			Transaction.end();
		}

		public String getTraversalPath() {
			traversalId = SEUtilities.generatePrettyUuid();
			traverse();
			return traversalId;
		}

		void traverse() {
			peer = new EntityPeer();
			peer.initialiseTraversal();
			peer.traversal.traverse();
		}

		@Override
		public void init() {
		}

		TraversalPlace currentPlace;

		@Override
		public void setPlace(TraversalPlace place) {
			if (this.currentPlace == null) {
			} else {
				if (!Objects.equals(place, this.currentPlace)) {
					traverse();
				}
			}
			this.currentPlace = place;
		}

		public AnswerSupplier createAnswerSupplier() {
			return new AnswerSupplierImpl();
		}

		class EntityPeer implements TraversalContext,
				TraversalDoesNotPublishNullObservable {
			SelectionTraversal traversal;

			RootLayer rootLayer;

			SelectionMarkup selectionMarkup;

			void initialiseTraversal() {
				traversal = new SelectionTraversal(this);
				traversal.id = traversalId;
				rootLayer = new RootLayer();
				traversal.setRootLayer(rootLayer);
				TreeProcess.Node parentNode = JobContext
						.getSelectedProcessNode();
				traversal.select(new DomainGraphSelection(parentNode, this));
			}

			boolean isSelected(Selection selection) {
				TraversalPlace place = Client.currentPlace();
				SelectionPath firstSelectionPath = place.firstSelectionPath();
				return firstSelectionPath != null && firstSelectionPath
						.nthSegmentPathIs(selection.processNode().depth() - 1,
								selection.getPathSegment());
			}

			int queryDepth() {
				TraversalPlace place = Client.currentPlace();
				SelectionPath firstSelectionPath = place.firstSelectionPath();
				return firstSelectionPath == null ? 0
						: firstSelectionPath.segmentCount();
			}
		}

		@Override
		public boolean isUseSelectionSegmentPath() {
			return true;
		}
	}
}
