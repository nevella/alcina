package cc.alcina.framework.gwt.client.story;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.meta.Feature;

/**
 * <h2>Goal</h2>
 * <p>
 * A story is the journey along a path whose {@link Point}s illustrate a
 * {@link Feature} or group of features.
 * <p>
 * It has similarities to both a tour and test sequence - both of which it's
 * intended to eventually replace.
 * <h3>An example - suggestor/filtering in selection
 * traversal/croissanteria</h3>
 * 
 * <pre>
 * <code>
 - the journey:
  - [dep] ensure the alc dev console is running on port x and has executed the croissanteria selection traversal
  - [dep] ensure the app suggestor is cleared
  - enter "flour" in the suggestor
  - observe the change in the displayed selections
 - the notes:
  - annotate a (non-flour) selection [point 'enter text ['flour'].1'] [doc level debug]['this selection will be filtered out']
  - display ui hint showing where to click (the suggestor)[point 'enter text ['flour'].2'][doc level info]
  - mark a flour selection, explaining the change [point 'selections filtered'.1][doc level info]
 - the commentary:
  - this belongs at the ui/header component/suggestor path
   - [ancestor] document what the ui does
   - [ancestor] document what the header does
   - [ancestor] document what the suggestor does
  - 'filter selections for investigation by typing a string. it will match text contents of input nodes
     and can be customised to match any aspect of a node (such as index in the document text run)
  

 * </code>
 * </pre>
 */
public interface Story {
	/**
	 *
	 * @return the top-level feature illustrated by the story
	 */
	Class<? extends Feature> getFeature();

	/**
	 *
	 * @return the top-level point illustrating the feature
	 */
	Point getPoint();

	/**
	 * A marker interface, modelled by subtypes which represent particular
	 * states in the environment of the Story. A {@link Point} can provide
	 * and/or require states, required state resolution will be resolved before
	 * the Point is told
	 */
	public interface State {
		@Retention(RetentionPolicy.RUNTIME)
		@Inherited
		@Documented
		@Target(ElementType.TYPE)
		public @interface Provides {
			Class<? extends State> value();
		}
	}

	/**
	 * <p>
	 * Elements of a story. Progres characteristics are:
	 * <ul>
	 * <li>Specify an app location (href)
	 * <li>Specify a UI element (xpath)
	 * 
	 * <li>Perform a UI action
	 * <li>Perform a non-UI action
	 * <li>Record the screen, possibly animated
	 * <li>Specify a point id [for later reference]
	 * <li>Specify required {@link State} elements
	 * <li>Specify child Point elements
	 * </ul>
	 * <p>
	 * Commentary characteristics are:
	 * <ul>
	 * <li>Annotate a UI element
	 * </ul>
	 * <h3>Notes</h3>
	 * <ul>
	 * <li>Points which contain other points should not themselves perform
	 * actions (although they can define required states). The class structure
	 * -will- encourage this
	 * </ul>
	 * 
	 */
	public interface Point {
		public interface Container {
		}
	}

	public interface StoryAction {
	}

	/**
	 * <p>
	 * Traverses the points of a story. Implementation is (presumably) via
	 * SelectionTraversal. The Per-point characteristics are:
	 * <ul>
	 * <li>Resolve point dependencies
	 * <li>Perform point actions (see Point)
	 * <li>Perform child actions
	 * <li>
	 * </ul>
	 */
	public interface Teller {
	}
}
