package cc.alcina.framework.servlet.domain.view;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.csobjects.view.DomainView;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Request;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Request.Element;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Response;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Transform;
import cc.alcina.framework.common.client.csobjects.view.DomainViewNode.Transform.Type;
import cc.alcina.framework.common.client.csobjects.view.DomainViewSearchDefinition;
import cc.alcina.framework.common.client.csobjects.view.TreePath;
import cc.alcina.framework.common.client.domain.DomainListener;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.DomainUpdate.DomainTransformCommitPosition;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.flat.FlatTreeSerializer;
import cc.alcina.framework.entity.persistence.domain.DomainStore;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

@RegistryLocation(registryPoint = DomainViews.class, implementationType = ImplementationType.SINGLETON)
public abstract class DomainViews {
	public static DomainViews get() {
		return Registry.impl(DomainViews.class);
	}

	protected LiveListener liveListener;

	private Map<Key, LiveView> views = new ConcurrentHashMap<>();

	public DomainViews() {
		liveListener = new LiveListener();
		setupIndexingInterceptors();
	}

	public Response handleRequest(
			Request<? extends DomainViewSearchDefinition> request) {
		Key key = new Key(request);
		LiveView view = views.computeIfAbsent(key, LiveView::new);
		return view.generateResponse(request);
	}

	protected abstract void setupIndexingInterceptors();

	public static class GeneratingContext {
		public Map<TreePath, DomainViewNode> byPath;

		public Map<TreePath, Object> byPathModel;

		public Predicate<?> modelFilter = o -> true;

		DomainViewNode cursor = null;

		public DomainViewNode generate(Object object) {
			DomainViewNode node = Registry
					.impl(NodeGenerator.class, object.getClass())
					.generate(object, this);
			TreePath path = new TreePath();
			path.setParent(cursor == null ? null : cursor.getTreePath());
			if (object instanceof Entity) {
				path.setLocator(((Entity) object).toLocator());
			} else {
				path.putDiscriminator(object);
			}
			node.setTreePath(path);
			node.setParent(cursor);
			byPath.put(path, node);
			byPathModel.put(path, object);
			cursor = node;
			List<Object> childModels = Registry
					.impl(NodeGenerator.class, object.getClass())
					.getChildModels(node, this);
			for (Object childModel : childModels) {
				DomainViewNode child = generate(childModel);
				node.getChildren().add(child);
			}
			cursor = cursor.getParent();
			return node;
		}
	}

	public interface NodeGenerator<I, N extends DomainViewNode> {
		public N generate(I in, GeneratingContext context);

		public List<? extends Object> getChildModels(DomainViewNode node,
				GeneratingContext generatingContext);
	}

	private static class LiveListener implements DomainListener {
		@Override
		public Class<? extends Entity> getListenedClass() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void insert(Entity o) {
			// TODO Auto-generated method stub
		}

		@Override
		public boolean isEnabled() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void remove(Entity o) {
			// TODO Auto-generated method stub
		}

		@Override
		public void setEnabled(boolean enabled) {
			throw new UnsupportedOperationException();
		}
	}

	static class Key {
		private Request request;

		private String stringKey;

		public Key(Request request) {
			this.request = request;
			this.stringKey = FlatTreeSerializer
					.serializeElided(request.getSearchDefinition());
		}

		@Override
		public boolean equals(Object anObject) {
			return this.stringKey.equals(anObject);
		}

		@Override
		public int hashCode() {
			return this.stringKey.hashCode();
		}
	}

	// TODO - sync get/modify
	static class LiveView {
		private DomainTransformCommitPosition earliestPosition;

		private DomainViewNode rootNode;

		Map<TreePath, DomainViewNode> byPath = new LinkedHashMap<>();

		Map<TreePath, Object> byPathModel = new LinkedHashMap<>();

		public LiveView(Key key) {
			earliestPosition = DomainStore.writableStore()
					.getTransformCommitPosition();
		}

		public Response generateResponse(
				Request<? extends DomainViewSearchDefinition> request) {
			Response response = new Response();
			response.setClearExisting(request.getSince() == null
					|| request.getSince().compareTo(earliestPosition) < 0);
			if (rootNode == null) {
				generateTree(request.getRoot().find(),
						new SearchPredicate(request.getSearchDefinition()));
			}
			for (Element element : request.getElements()) {
				response.getTransforms()
						.addAll(elementToTransform(element, request));
			}
			return response;
		}

		private List<Transform> elementToTransform(Element element,
				Request<? extends DomainViewSearchDefinition> request) {
			List<Transform> result = new ArrayList<>();
			TreePath path = element.getPath();
			DomainViewNode<?> node = byPath.get(path);
			if (node != null) {
				{
					Transform transform = new Transform();
					transform.setPath(path);
					transform.setNode(node);
					transform.setType(Type.APPEND);
					result.add(transform);
				}
				switch (element.getChildren()) {
				case IMMEDIATE_ONLY:
					for (DomainViewNode child : node.getChildren()) {
						{
							Transform transform = new Transform();
							transform.setPath(child.getTreePath());
							transform.setNode(child);
							transform.setType(Type.APPEND);
							result.add(transform);
						}
					}
				}
			}
			return result;
		}

		private void generateTree(DomainView rootEntity,
				Predicate searchPredicate) {
			GeneratingContext context = new GeneratingContext();
			context.modelFilter = searchPredicate;
			context.byPath = byPath;
			context.byPathModel = byPathModel;
			rootNode = context.generate(rootEntity);
		}

		public static class SearchPredicate implements Predicate {
			private DomainViewSearchDefinition searchDefinition;

			public SearchPredicate(
					DomainViewSearchDefinition searchDefinition) {
				this.searchDefinition = searchDefinition;
			}

			@Override
			public boolean test(Object t) {
				return true;
			}
		}
	}

	static class ViewsEvent {
		ModelChange modelChange = new ModelChange();

		PathChange pathChange = new PathChange();

		Type type;

		static class ModelChange {
			Transaction preCommit;

			Transaction commit;

			Request request;
		}

		static class PathChange {
			TreePath path;

			Type type;

			static enum Type {
				ADD, REMOVE, CHANGE;
			}
		}

		static enum Type {
			MODEL_CHANGE, PATH_CHANGE;
		}
	}
}
