package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Preconditions;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CountingMap;
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

	enum InsertDirection {
		/*
		 * if before == null, to before, otherwise to 'most-after-before'
		 * (descend before.after)
		 */
		before,
		/*
		 * append to the insert's pending list, if possible, otherwise if after
		 * == null, to after, otherwise to most-before-after
		 */
		after
	}

	/**
	 * See {@link #next()} for the structure that these instances create.
	 * Essentially it's a tree - but optimised for the common case of 'insert a
	 * consecutive list of elements'
	 */
	@TypedProperties
	@Directed.Delegating
	class RelativeInsert extends Model.All {
		RelativeInsert(RelativeInsert parent) {
			this.parent = parent;
		}

		RelativeInsert before;

		/* Note that only one of {element, contents} can be non-null */
		Object element;

		List<RelativeInsert> contents;

		@Property.Not
		List<RelativeInsert> pendingContents;

		RelativeInsert after;

		/*
		 * a node is the parent of before, the {elements of contents/contnets}
		 * and after
		 */
		RelativeInsert parent;

		RelativeInsert firstDescendantOrSelf() {
			if (before != null) {
				return before.firstDescendantOrSelf();
			}
			/*
			 * Don't descend into contents (that's logically after this, but
			 * before after) - so this method is not symmetrical wrt
			 * lastDescendantOrSelf
			 */
			// if (contents != null) {
			// return Ax.first(contents).firstDescendantOrSelf();
			// }
			return this;
		}

		/**
		 * computes next; removes this from the structure if redundant; move
		 * cursor to next
		 * 
		 * @return
		 */
		RelativeInsert validatingNext() {
			RelativeInsert next = next();
			if (element != null && !Objects.equals(element, update.current)) {
				RelativeInsert_properties.element.set(this, null);
			}
			/*
			 * in the future, could *possibly* simplify the RelativeInsert
			 * tree/structure here, but not now
			 */
			return next;
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

		void insert(Object object, InsertDirection insertDirection) {
			if (insertDirection == InsertDirection.after) {
				if (canAppend()) {
					append(object);
				} else {
					if (after == null) {
						RelativeInsert child = new RelativeInsert(this);
						child.append(object);
						RelativeInsert_properties.after.set(this, child);
					} else {
						RelativeInsert firstDescendantOrSelfOfAfter = after
								.firstDescendantOrSelf();
						/*
						 * it's guaranteed that
						 * firstDescendantOrSelfOfAfter.before is null, since
						 * all leaves are null
						 */
						RelativeInsert child = new RelativeInsert(
								firstDescendantOrSelfOfAfter);
						child.append(object);
						RelativeInsert_properties.before
								.set(firstDescendantOrSelfOfAfter, child);
					}
				}
			} else {
				if (before == null) {
					RelativeInsert child = new RelativeInsert(this);
					child.append(object);
					RelativeInsert_properties.before.set(this, child);
				} else {
					RelativeInsert lastDescendantOrSelfOfBefore = after
							.lastDescendantOrSelf();
					/*
					 * it's guaranteed that lastDescendantOrSelfOfBefore.after
					 * is null, since all leaves are null
					 */
					RelativeInsert child = new RelativeInsert(
							lastDescendantOrSelfOfBefore);
					child.append(object);
					RelativeInsert_properties.after
							.set(lastDescendantOrSelfOfBefore, child);
				}
			}
		}

		void append(Object current) {
			RelativeInsert child = new RelativeInsert(this);
			if (pendingContents == null) {
				pendingContents = new ArrayList<>();
			}
			child.element = current;
			pendingContents.add(child);
		}

		boolean canAppend() {
			return element == null && contents == null
					&& pendingContents == null;
		}

		/**
		 * <p>
		 * Iteration order (recursive):
		 * 
		 * <ul>
		 * <li>if contents!=null, first tree descendant of first contents elem
		 * <li>elif after!=null, first tree descendant of after
		 * <li>else parent.getSuccessorPostDescent(this):
		 * <ul>
		 * <li>if(child==before, successor := this)
		 * <li>elif(child==after, successor := this.parent.getSuccessor(this))
		 * (tree ascent)
		 * <li>elif(child==not-last-contents elem, successor :- first tree
		 * descendant of contents.successor(child)
		 * <li>else(child==last-contents elem:)
		 * <ul>
		 * <li>if after!=null, first tree descendant of after
		 * <li>else parent.getSuccessor(this)
		 * </ul>
		 * </ul>
		 * </ul>
		 * 
		 * iteration order (recursive) - for node n: if contents!=null, iif
		 * after !=null, firstDesc/Self of after.
		 * 
		 * insert model:text art!
		 * 
		 * <pre>
		 * <code>
		 * 
		RenderedInsert models are renoted r(gen).(global idx)
		Initial list: i0.0 - i0.1 - i0.2. 
		subsequent insert: i1.0 before i0.0 - i1.1 before i0.2 - i1.2 after io.2 - i1.3 after i1.2
		
		op0 		
								ROOT(r0.0 :: null)
		BEFORE:null      CONTENT:[(r0.1 :: i0.0),(r0.2 :: i0.1),(r0.3 :: i0.2)]   AFTER: null
		
		
		op1
		
											ROOT(r0.0 :: null)
		BEFORE:(r1.4 :: i1.0)      CONTENT:[(r0.1 :: i0.0),(r0.2 :: i0.1),(r0.3 :: i0.2)]                                       AFTER: (r1.6)
													BEFORE: (r1.5 :: i1.1) AFTER: null       BEFORE:null      CONTENT:[(r1.7 :: i1.2),(r1.8 :: i1.3)   AFTER: null
		 * 
		 * </code>
		 * </pre>
		 * 
		 * @return
		 */
		RelativeInsert next() {
			if (contents != null) {
				return Ax.first(contents).firstDescendantOrSelf();
			} else if (after != null) {
				return after.firstDescendantOrSelf();
			} else {
				return getSuccessorPostDescent();
			}
		}

		@Property.Not
		RelativeInsert getSuccessorPostDescent() {
			if (parent == null) {
				return null;
			} else {
				return parent.getSuccessorPostDescent(this);
			}
		}

		@Property.Not
		RelativeInsert getSuccessorPostDescent(RelativeInsert child) {
			if (child == before) {
				return this;
			} else if (child == after) {
				return getSuccessorPostDescent();
			} else {
				// contents non-null
				int idx = contents.indexOf(child);
				if (idx < contents.size() - 1) {
					RelativeInsert nextContentsElem = contents.get(idx + 1);
					return nextContentsElem.firstDescendantOrSelf();
				} else {
					if (after != null) {
						return after.firstDescendantOrSelf();
					} else {
						return getSuccessorPostDescent();
					}
				}
			}
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

		/**
		 * The delta logic needs to know "is there an element later in the
		 * collection that is wrapped by this RelativeInsert"
		 */
		CountingMap<Object> remainingToRender;

		/**
		 * The delta logic needs to know "is there a RelativeInsert later in the
		 * structure that wraps this collection element"
		 */
		CountingMap<Object> remainingRendered;

		Object current;

		RelativeInsert renderAt;

		List<RelativeInsert> pending = new ArrayList<>();

		InsertDirection insertDirection;

		Update() {
			Preconditions.checkArgument(!collection.contains(null));
			remainingToRender = new CountingMap<>();
			remainingRendered = new CountingMap<>();
			collection.forEach(remainingToRender::add);
			cursor = root.firstDescendantOrSelf();
			while (cursor != null) {
				remainingRendered.add(cursor.element);
				cursor = cursor.next();
			}
			cursor = root.firstDescendantOrSelf();
		}

		void update() {
			Iterator<?> itr = collection.iterator();
			while (itr.hasNext()) {
				current = itr.next();
				/*
				 * reset 'where does this render' coordinates
				 */
				insertDirection = null;
				renderAt = null;
				/*
				 * either cursor matches; cursor does not match; cursor is null
				 * 
				 * The latter two cases are the same (ensure cursor does not
				 * directly contaain anything, move on)
				 */
				while (renderAt == null) {
					if (cursor == null) {
						renderAt = root.lastDescendantOrSelf();
						insertDirection = InsertDirection.after;
					} else if (cursor.matches(current)) {
						renderAt = cursor;
						// not necessary, but this indicates 'we're not
						// inserting'
						// insertDirection = null;
					} else if (willMatch(cursor)) {
						insertDirection = InsertDirection.before;
						renderAt = cursor;
					} else {
						/* will clear out all remaining inserts */
						cursor = cursor.validatingNext();
					}
				}
				if (renderAt.matches(current)) {
					// already rendered
					cursor = renderAt.validatingNext();
				} else {
					renderAt.insert(current, insertDirection);
					cursor = renderAt.validatingNext();
				}
			}
			/*
			 * All remaining nodes must be either rendered or non-existent - so
			 * they'll be removed
			 */
			while (cursor != null) {
				cursor = cursor.validatingNext();
			}
		}

		/*
		 * true if there's still an underendered instance of current that the
		 * insert instance
		 */
		boolean willMatch(RelativeInsert insert) {
			if (insert.element == null) {
				return false;
			} else {
				return remainingToRender.containsKey(insert.element);
			}
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
			root = new RelativeInsert(null);
		}
		update = new Update();
		update.update();
	}
}
