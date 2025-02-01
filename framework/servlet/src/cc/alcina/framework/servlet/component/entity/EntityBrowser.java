package cc.alcina.framework.servlet.component.entity;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.user.client.History;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.Selection;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.TraversalContext;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.entity.Io;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.util.MethodContext;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.servlet.component.entity.EntityBrowser.Ui.EntityPeer;
import cc.alcina.framework.servlet.component.entity.RootLayer.DomainGraphSelection;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.traversal.StandardLayerAttributes;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.TraversalAnswerSupplier;
import cc.alcina.framework.servlet.component.traversal.TraversalHistories;
import cc.alcina.framework.servlet.component.traversal.TraversalHistories.TraversalDoesNotPublishNullObservable;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionPath;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.SecondaryAreaDisplayMode;
import cc.alcina.framework.servlet.environment.DomainUi;
import cc.alcina.framework.servlet.environment.RemoteUi;
import cc.alcina.framework.servlet.environment.SettingsSupport;
import cc.alcina.framework.servlet.job.JobContext;

public class EntityBrowser {
	@Registration(RemoteComponent.class)
	public static class Component implements RemoteComponent {
		@Override
		public String getPath() {
			return "/entity";
		}

		@Override
		public Class<? extends RemoteUi> getUiType() {
			return EntityBrowser.Ui.class;
		}
	}

	static EntityPeer peer() {
		return Ui.cast().peer;
	}

	public static class Ui extends TraversalBrowser.Ui implements DomainUi {
		String traversalId;

		public Ui() {
		}

		public boolean isClearPostSelectionLayers() {
			return true;
		}

		public static Ui cast() {
			return (Ui) get();
		}

		@Override
		public void injectCss(String relativePath) {
			StyleInjector
					.injectNow(Io.read().relativeTo(TraversalBrowser.Ui.class)
							.resource(relativePath).asString());
		}

		public String getMainCaption() {
			return "Entity graph";
		}

		EntityPeer peer;

		static TraversalPlace traversingPlace() {
			return cast().peer.place;
		}

		public String getTraversalPath() {
			traversalId = SEUtilities.generatePrettyUuid();
			traverse(place());
			return traversalId;
		}

		@Override
		public void initialiseSettings(String settings) {
			this.settings = SettingsSupport.deserializeSettings(
					TraversalSettings.class, settings, newInstance -> {
						newInstance.secondaryAreaDisplayMode = SecondaryAreaDisplayMode.TABLE;
					});
			int debug = 3;
		}

		void traverse(TraversalPlace place) {
			long start = System.currentTimeMillis();
			evictPeer();
			peer = new EntityPeer(place);
			peer.initialiseTraversal();
			MethodContext.instance().withContextValue(
					RemoteComponentObservables.CONTEXT_OVERRIDE_EVICTION_TIME,
					5 * TimeConstants.ONE_SECOND_MS)
					.run(() -> peer.traversal.traverse());
			long end = System.currentTimeMillis();
			Ax.out("egv/traverse: %sms", end - start);
		}

		void evictPeer() {
			if (peer != null) {
				TraversalHistories.get().evict(peer.traversal);
			}
		}

		@Override
		public void init() {
			super.init();
		}

		@Override
		public void setPlace(TraversalPlace place) {
			if (!Objects.equals(place, this.place)) {
				// this must happen before setPlace
				traverse(place);
				super.setPlace(place);
				if (traversal() == null) {
					return;
				}
				// if the filter matches exactly one entity, append it to the
				// place + re-set
				Layer lastLayer = Ax.last(traversal().getVisitedLayers());
				if (place.attributesOrEmpty(lastLayer.index)
						.has(StandardLayerAttributes.Filter.class)) {
					Collection<Selection> selections = lastLayer
							.getSelections();
					if (selections.size() == 1) {
						Selection next = selections.iterator().next();
						if (next.get() instanceof Entity) {
							TraversalPlace f_place = place
									.appendSelections(List.of(next));
							Client.eventBus().queued().deferred().lambda(() -> {
								// key! never do cascading history changes
								// without this!
								History.runReplacing(() -> f_place.go());
							}).dispatch();
							/*
							 * do not fall through to super.setPlace (the place
							 * will be replaced during the deferred place.go())
							 */
							return;
						}
					}
				}
			}
		}

		@Override
		public void end() {
			super.end();
			evictPeer();
		}

		public TraversalAnswerSupplier createAnswerSupplier(int forLayer) {
			return new EntityAnswers(forLayer);
		}

		class EntityPeer
				implements TraversalContext, TraversalContext.ThrowOnException,
				TraversalDoesNotPublishNullObservable {
			SelectionTraversal traversal;

			RootLayer rootLayer;

			TraversalPlace place;

			public EntityPeer(TraversalPlace place) {
				this.place = place;
			}

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
				SelectionPath firstSelectionPath = place.firstSelectionPath();
				return firstSelectionPath != null && firstSelectionPath
						.nthSegmentPathIs(selection.processNode().depth() - 1,
								selection.getPathSegment());
			}

			int queryDepth() {
				SelectionPath firstSelectionPath = place.firstSelectionPath();
				return firstSelectionPath == null ? 0
						: firstSelectionPath.segmentCount();
			}
		}

		@Override
		public boolean isUseSelectionSegmentPath() {
			return true;
		}

		protected SelectionTraversal traversal0() {
			return peer().traversal;
		}
	}
}
