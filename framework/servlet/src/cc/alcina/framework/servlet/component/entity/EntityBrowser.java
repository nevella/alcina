package cc.alcina.framework.servlet.component.entity;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.user.client.History;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.meta.Feature;
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
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.TopLevelMissedEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.NotificationObservable;
import cc.alcina.framework.servlet.component.entity.EntityBrowser.Ui.EntityPeer;
import cc.alcina.framework.servlet.component.entity.EntitySelection.EntityTypeExtended.EntityDeletionHandler;
import cc.alcina.framework.servlet.component.entity.RootLayer.DomainGraphSelection;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponent;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser;
import cc.alcina.framework.servlet.component.traversal.TraversalBrowser.TraversalAnswerSupplier;
import cc.alcina.framework.servlet.component.traversal.TraversalObserver;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace;
import cc.alcina.framework.servlet.component.traversal.TraversalPlace.SelectionPath;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings;
import cc.alcina.framework.servlet.component.traversal.TraversalSettings.SecondaryAreaDisplayMode;
import cc.alcina.framework.servlet.environment.DomainUi;
import cc.alcina.framework.servlet.environment.RemoteUi;
import cc.alcina.framework.servlet.environment.SettingsSupport;
import cc.alcina.framework.servlet.job.JobContext;

@Feature.Ref(Feature_EntityBrowser.class)
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

		@Override
		protected List<?> createAdditionalLeftHeader() {
			return List.of(cache.createUi());
		}

		@Override
		public boolean isAppendTableSelections() {
			return true;
		}

		NonOptimisedQueryCache cache = new NonOptimisedQueryCache();

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
		protected TraversalPlace activePlace0() {
			return traversingPlace();
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
				TraversalObserver.get().evict(peer.traversal);
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
				// if the last layer matches exactly one entity, append it to
				// the
				// place + re-set
				Layer lastLayer = Ax.last(traversal().layers().getVisited());
				// if (place.attributesOrEmpty(lastLayer.index)
				// .has(StandardLayerAttributes.Filter.class)) {
				Collection<Selection> selections = lastLayer.getSelections();
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
						 * do not fall through to super.setPlace (the place will
						 * be replaced during the deferred place.go())
						 */
						return;
					}
				}
				// }
			}
		}

		@Override
		public void end() {
			super.end();
			evictPeer();
		}

		@Override
		public TraversalAnswerSupplier createAnswerSupplier(int forLayer,
				boolean hasClearableFilter) {
			return new EntityAnswers(forLayer, hasClearableFilter);
		}

		class EntityPeer
				implements TraversalContext, TraversalContext.ThrowOnException,
				TraversalContext.ShortTraversal {
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
				traversal.layers().setRoot(rootLayer);
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
		protected Model getEventHandlerCustomisation() {
			return new EntityEventHandler();
		}

		@Override
		public boolean isUseSelectionSegmentPath() {
			return true;
		}

		protected SelectionTraversal traversal0() {
			return peer().traversal;
		}
	}

	/*
	 * A delegating, zero-contents model (so it has no dom represenation, but
	 * can receive events)
	 */
	@Directed.Delegating
	static class EntityEventHandler extends Model
			implements ModelEvents.Delete.Handler,
			ModelEvents.TopLevelMissedEvent.Handler {
		@Override
		public final void onDelete(ModelEvents.Delete event) {
			PermitDeletion permitDeletion = Registry.impl(PermitDeletion.class);
			Entity entity = event.getModel();
			if (!permitDeletion.permitDeletion(entity)) {
				NotificationObservable
						.of("Cannot delete: %s", entity.toStringEntity())
						.publish();
				;
			} else {
				NotificationObservable
						.of("Deleting: %s", entity.toStringEntity()).publish();
				Registry.impl(EntityDeletionHandler.class, entity.entityClass())
						.delete(entity);
				NotificationObservable
						.of("Deleted: %s", entity.toStringEntity()).publish();
			}
		}

		@Override
		public void onTopLevelMissedEvent(TopLevelMissedEvent event) {
			ModelEvent wrappedEvent = event.getModel();
			if (wrappedEvent instanceof ModelEvents.Delete) {
				onDelete((ModelEvents.Delete) wrappedEvent);
			}
		}
	}

	// Allows project-specific customisation of
	// TraversalBrowser.onBeforeEnterContext()
	@Registration.Self
	public static class PermitDeletion {
		public boolean permitDeletion(Entity e) {
			return false;
		}
	}
}
