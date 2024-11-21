package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;

/**
 * <p>
 * Wraps a reference to a collection property, and (via delegation/transform)
 * ensures that only the changed elements are rendered. In brief, it does this
 * by wrapping every collection element in a RelativeInsert structure that has
 * before- and after- locations - and collection deltas cause modifications in
 * those structures.
 * 
 * <p>
 * If the collection is modified many times, this may lead to deep
 * RelativeInsert structures - there's currently no facility to
 * clean-up/rationalise at a certain depth, but that could be added
 * 
 * <p>
 * The collection may not contain <code>null</code>
 * 
 * <p>
 * WIP - the structure/sequence looks about right, need to implement the
 * unsupporteds and add set of "pendings to flush"
 */
@TypedProperties
@Directed.Delegating
public class CollectionDeltaModel extends Model.Fields {
	static PackageProperties._CollectionDeltaModel properties = PackageProperties.collectionDeltaModel;

	/*
	 * FIXME - jdk16 - move to static member of inner class
	 */
	static PackageProperties._CollectionDeltaModel_RelativeInsert RelativeInsert_properties = PackageProperties.collectionDeltaModel_relativeInsert;

	@TypedProperties
	@Directed.Delegating
	class RelativeInsert extends Model.All {
		RelativeInsert before;

		List<RelativeInsert> contents;

		@Property.Not
		List<RelativeInsert> pendingContents;

		Object element;

		RelativeInsert after;

		/*
		 * a node is the parent of before, after and the elements of contents
		 */
		RelativeInsert parent;

		RelativeInsert firstDescendantOrSelf() {
			if (before != null) {
				return before.firstDescendantOrSelf();
			}
			if (contents != null) {
				return Ax.first(contents).firstDescendantOrSelf();
			}
			return this;
		}

		/**
		 * computes next; removes this from the structure if redundant; move
		 * cursor to next
		 */
		void validatingNext() {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'validatingNext'");
		}

		boolean matches(Object element) {
			return Objects.equals(this.element, element);
		}

		RelativeInsert lastDescendantOrSelf() {
			if (after != null) {
				return after.lastDescendantOrSelf();
			}
			if (contents != null) {
				return Ax.last(contents).lastDescendantOrSelf();
			}
			return this;
		}

		void render(Object current) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException(
					"Unimplemented method 'render'");
		}
	}

	/*
	 * As in other tree-like structures, this is an "off-grid" container - a
	 * logical, not a render root
	 */
	RelativeInsert root;

	@Directed
	List<RelativeInsert> initialElements;

	public Collection<?> collection;

	public CollectionDeltaModel() {
		bindings().from(this).on(properties.collection)
				.signal(this::updateElements);
	}

	/*
	 * Manages some state used to compute updates, and performs the update
	 * iteration/traversal
	 */
	class Update {
		RelativeInsert cursor = root.firstDescendantOrSelf();

		Set<?> existenceTest;

		Set<?> rendered = AlcinaCollections.newHashSet();

		Object current;

		RelativeInsert renderAt;

		Update() {
			existenceTest = CommonUtils.wrapInSet(collection);
			Preconditions.checkArgument(!existenceTest.contains(null));
		}

		void update() {
			Iterator<?> itr = collection.iterator();
			while (itr.hasNext()) {
				current = itr.next();
				/*
				 * either cursor matches; cursor does not match; cursor is null
				 * 
				 * The latter two cases are the same (ensure cursor does not
				 * directly contaain anything, move on)
				 */
				while (renderAt == null) {
					if (cursor == null) {
						renderAt = root.lastDescendantOrSelf();
					} else if (cursor.matches(current)) {
						renderAt = cursor;
					} else if (willMatch(cursor)) {
						renderAt = cursor;
					} else {
						cursor.validatingNext();
					}
				}
				if (cursor.matches(current)) {
					// already rendered
					cursor.validatingNext();
				} else {
					cursor.render(current);
				}
			}
			/*
			 * All remaining nodes must be either rendered or non-existent - so
			 * they'll be removed
			 */
			while (cursor != null) {
				cursor.validatingNext();
			}
			// TODO -
		}

		// true if there's still an underendered instance of current that the
		// insert instance
		// wraps
		boolean willMatch(RelativeInsert insert) {
			throw new UnsupportedOperationException();
		}
	}

	transient Update update;

	void updateElements() {
		if (collection == null || collection.isEmpty()) {
			properties.initialElements.set(this, null);
			root = null;
			return;
		}
		if (root == null) {
			root = new RelativeInsert();
		}
		update = new Update();
		update.update();
	}
}
