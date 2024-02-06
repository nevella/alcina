package cc.alcina.framework.servlet.task;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringEscapeUtils;

import cc.alcina.framework.common.client.domain.Domain.EntityTreeLogger;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.servlet.schedule.PerformerTask;

public class TaskDomainQuery extends PerformerTask {
	private List<String> resultPaths = List.of("*").stream()
			.collect(Collectors.toList());

	private EntityLocator from;

	private int maxElementsPerCollection = 100;

	private transient FormatBuilder fb = new FormatBuilder();

	private int maxChars = 20000;

	private transient PathSegments pathSegments;

	private boolean withEntityToString;

	public EntityLocator getFrom() {
		return this.from;
	}

	public int getMaxChars() {
		return this.maxChars;
	}

	public int getMaxElementsPerCollection() {
		return this.maxElementsPerCollection;
	}

	private List<Entity> getPropertyEntities(Entity entity, Property p) {
		Object value = p.get(entity);
		List<Entity> result = new ArrayList<>();
		if (value instanceof Entity) {
			result.add((Entity) value);
		} else {
			if (value instanceof Collection) {
				((Collection) value).stream().forEach(e -> {
					if (e instanceof Entity) {
						result.add((Entity) e);
					}
				});
			}
		}
		return result;
	}

	private String getPropertyValue(Entity entity, Property p) {
		return getStringRepresentation(p.get(entity));
	}

	public List<String> getResultPaths() {
		return this.resultPaths;
	}

	private String getStringRepresentation(Object object) {
		if (object == null) {
			return "(null)";
		}
		if (object instanceof Entity) {
			Entity entity = (Entity) object;
			return isWithEntityToString()
					? Ax.format("%s :: %s", entity.getId(), entity)
					: String.valueOf(entity.getId());
		}
		if (object instanceof Collection) {
			Collection collection = (Collection) object;
			if (collection.isEmpty()) {
				return "[]";
			} else {
				FormatBuilder collectionWrapperBuilder = new FormatBuilder();
				FormatBuilder collectionBuilder = new FormatBuilder()
						.separator(",");
				int limit = Math.min(collection.size(),
						maxElementsPerCollection);
				collectionWrapperBuilder.append(
						Ax.format("[(%s/%s):", limit, collection.size()));
				collection.stream().limit(limit).forEach(o -> collectionBuilder
						.append(this.getStringRepresentation(o)));
				collectionWrapperBuilder.append(collectionBuilder);
				collectionWrapperBuilder.append("]");
				return collectionWrapperBuilder.toString();
			}
		}
		if (object instanceof String) {
			maxChars = 200;
			return Ax.format("\"%s\"", StringEscapeUtils.escapeJava(CommonUtils
					.trimToWsChars(object.toString(), maxChars, true)));
		} else {
			return object.toString();
		}
	}

	public boolean isWithEntityToString() {
		return this.withEntityToString;
	}

	private void logPath(Entity entity, int depth) {
		ClassReflector classReflector = Reflections.at(entity.entityClass());
		List<Property> properties = pathSegments.properties(entity);
		fb.indent(depth * 4);
		fb.line("Entity: %s", entity.toStringId());
		int propertyNameMaxLength = 20;
		properties.stream().filter(Property::provideNotDefaultIgnoreable)
				.filter(Property::isReadable)
				.sorted(new Property.NameComparator()).forEach(p -> {
					fb.indent(depth * 4 + 2);
					String name = CommonUtils.trimToWsChars(p.getName(),
							propertyNameMaxLength);
					String propertyValue = getPropertyValue(entity, p);
					fb.line("%s : %s", CommonUtils.padStringRight(name,
							propertyNameMaxLength, ' '), propertyValue);
					if (pathSegments.tryDescend(name)) {
						// exact path match, can descend
						List<Entity> entities = getPropertyEntities(entity, p);
						entities.forEach(e -> logPath(e, depth + 1));
						pathSegments.ascend(name);
					}
				});
	}

	@Override
	public void run() {
		Entity entity = from.find();
		if (resultPaths == null || resultPaths.isEmpty()) {
			resultPaths = List.of("*");
		}
		pathSegments = new PathSegments();
		fb.line("Entity: %s", from);
		fb.line("Paths: %s", resultPaths);
		fb.line("=============================================");
		logPath(entity, 0);
		logger.info(fb.toString());
	}

	public void setFrom(EntityLocator from) {
		this.from = from;
	}

	public void setMaxChars(int maxChars) {
		this.maxChars = maxChars;
	}

	public void setMaxElementsPerCollection(int maxElementsPerCollection) {
		this.maxElementsPerCollection = maxElementsPerCollection;
	}

	public void setResultPaths(List<String> resultPaths) {
		this.resultPaths = resultPaths;
	}

	public void setWithEntityToString(boolean withEntityToString) {
		this.withEntityToString = withEntityToString;
	}

	public TaskDomainQuery withFrom(Class clazz, long id) {
		setFrom(new EntityLocator(clazz, id, 0L));
		return this;
	}

	public TaskDomainQuery withFrom(Entity entity) {
		setFrom(entity.toLocator());
		return this;
	}

	public TaskDomainQuery withResultPaths(String... resultPaths) {
		this.resultPaths = Arrays.stream(resultPaths)
				.collect(Collectors.toList());
		return this;
	}

	@Registration(EntityTreeLogger.class)
	public static class EntityTreeLoggerImpl implements EntityTreeLogger {
		@Override
		public void log(Entity entity, String... paths) {
			new TaskDomainQuery().withFrom(entity).withResultPaths(paths).run();
		}
	}

	class PathSegment {
		List<String> activeSegments = new ArrayList<>();

		List<String> segments;

		PathSegment(String path) {
			segments = Arrays.asList(path.split("\\."));
		}

		void ascendIfActive() {
			if (isActive()) {
				activeSegments.remove(activeSegments.size() - 1);
			}
		}

		protected boolean isActive() {
			return activeSegments.size() == pathSegments.descendedSegments
					.size();
		}

		public List<Property> properties(Entity entity) {
			if (!isActive()) {
				return Collections.emptyList();
			}
			ClassReflector classReflector = Reflections
					.at(entity.entityClass());
			String segment = segments.get(activeSegments.size());
			return segment.equals("*") ? classReflector.properties()
					: List.of(classReflector.property(segment));
		}

		boolean tryDescend(String segment) {
			if (isActive()) {
				if (segments.size() > activeSegments.size() + 1) {
					String nextSegment = segments.get(activeSegments.size());
					if (Objects.equals(nextSegment, segment)
							|| nextSegment.equals("*") || segment.equals("*")) {
						activeSegments.add(nextSegment);
						return true;
					}
				}
			}
			return false;
		}
	}

	class PathSegments {
		List<PathSegment> list;

		List<String> descendedSegments = new ArrayList<>();

		PathSegments() {
			list = resultPaths.stream().map(PathSegment::new)
					.collect(Collectors.toList());
		}

		void ascend(String segment) {
			list.forEach(PathSegment::ascendIfActive);
			descendedSegments.remove(descendedSegments.size() - 1);
		}

		// return visible properties at [path] for entity
		List<Property> properties(Entity entity) {
			return list.stream()
					.map(pathSegment -> pathSegment.properties(entity))
					.flatMap(Collection::stream).distinct()
					.collect(Collectors.toList());
		}

		boolean tryDescend(String segment) {
			boolean descended = false;
			for (PathSegment pathSegment : list) {
				if (pathSegment.tryDescend(segment)) {
					descended = true;
				}
			}
			if (descended) {
				descendedSegments.add(segment);
			}
			return descended;
		}
	}
}
