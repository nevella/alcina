package cc.alcina.framework.servlet.sync;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.HasEquivalenceString;
import cc.alcina.framework.common.client.util.ThrowingSupplier;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.servlet.sync.TreeSync.Syncer.Operation;

// instances should only .equals themselves -- and equivalence shd be unique
//
// equivalence is shallow (i.e. exclusive of TreeSyncable children)
public interface TreeSyncable<T extends TreeSyncable>
		extends HasEquivalenceString<T> {
	default String deepEquivalenceString() {
		FormatBuilder fb = new FormatBuilder().separator("->");
		provideSelfAndDescendantTree()
				.forEach(s -> fb.append(s.equivalenceString()));
		return fb.toString();
	}

	default String name() {
		return equivalenceString();
	}

	default Stream<Field> provideChildFields(boolean syncable) {
		List<Field> allFields = SEUtilities.allFields(getClass());
		List<Field> result = new ArrayList<>();
		for (Field field : allFields) {
			if (Modifier.isTransient(field.getModifiers())) {
				continue;
			}
			Class<?> type = field.getType();
			boolean ts = TreeSyncable.class.isAssignableFrom(type);
			boolean tsCollection = false;
			if (Collection.class.isAssignableFrom(type)) {
				Type genericType = GraphProjection.getGenericType(field);
				if (genericType instanceof ParameterizedType) {
					Type parameterizingType = ((ParameterizedType) genericType)
							.getActualTypeArguments()[0];
					if (parameterizingType instanceof Class) {
						Class parameterizingClass = (Class) parameterizingType;
						tsCollection = TreeSyncable.class
								.isAssignableFrom(parameterizingClass);
					}
				}
			}
			boolean treeSyncable = ts || tsCollection;
			if (treeSyncable ^ !syncable) {
				result.add(field);
			}
		}
		return result.stream();
	}

	default Stream<TreeSyncable> provideChildSyncables() {
		return provideChildFields(true)
				.flatMap(f -> ThrowingSupplier.wrap(() -> {
					Object value = f.get(this);
					if (value == null) {
						return Stream.empty();
					} else if (value instanceof TreeSyncable) {
						return Stream.of((TreeSyncable) value);
					} else {
						return ((Collection<TreeSyncable<?>>) value).stream();
					}
				}));
	}

	default Stream<TreeSyncable<?>> provideSelfAndDescendantTree() {
		// some fairly heavy casting to handle alternation between
		// TreeSyncable<?> and TreeSyncable
		Function<TreeSyncable<?>, List<TreeSyncable<?>>> childrenSupplier = s -> (List) s
				.provideChildSyncables()
				.sorted(Comparator
						.comparing(HasEquivalenceString::equivalenceString))
				.map(t -> (TreeSyncable) t).collect(Collectors.toList());
		DepthFirstTraversal<TreeSyncable<?>> traversal = new DepthFirstTraversal<TreeSyncable<?>>(
				this, childrenSupplier);
		return traversal.stream();
	}

	// copy non-persistent, non-stateful fields
	//
	// this is called to ease logic concerning remote objects (since local
	// objects will often have a richer transient model)
	//
	// mark any non-treesync but stateful fields (e.g. remote state) as
	// @CreateIgnore to ensure CREATE (which uses reflective field replication)
	// and this are synced
	default void updateFromSyncEquivalent(Operation<T> operation, T other) {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target(ElementType.FIELD)
	public @interface CreateIgnore {
	}
}
