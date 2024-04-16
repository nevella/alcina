package cc.alcina.framework.gwt.client.story;

import java.lang.System.Logger.Level;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Reflections;

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
	 * <p>
	 * A marker interface, modelled by subtypes which represent particular
	 * states in the environment of the Story. A {@link Point} can provide
	 * and/or require states, required state resolution will be resolved before
	 * the Point is told
	 * 
	 * <p>
	 * State resolvers should be idempotent, and query their context to avoid
	 * multiple-execution
	 */
	public interface State {
		/*
		 * Marker interface, implemented by providers. The default execution
		 * behaviour is for a state to be resolved (added to the set of resolved
		 * states) once the Provider Point is visited. For async/parallel
		 * dependency resolution, that behaviour would need to be overridden
		 */
		@Registration.NonGenericSubtypes(Provider.class)
		public interface Provider<S extends State>
				extends Point, Registration.AllSubtypesClient {
			default Class<? extends State> resolvesState() {
				return Reflections.at(this).firstGenericBound();
			}
		}
	}

	/**
	 * A naming container (short for 'Declarative')
	 */
	public interface Decl {
		/** Declaratively define a point */
		@Retention(RetentionPolicy.RUNTIME)
		@Documented
		@Target({ ElementType.TYPE })
		@Repeatable(Points.class)
		public @interface Point {
			Decl.Action.Code[] code() default {};
		}

		@Retention(RetentionPolicy.RUNTIME)
		@Documented
		@Target({ ElementType.TYPE })
		public @interface Points {
			Point[] value();
		}

		public interface Action {
			/** Declaratively define a code action */
			@Retention(RetentionPolicy.RUNTIME)
			@Documented
			@Target({ ElementType.TYPE })
			public @interface Code {
				Class<? extends Story.Action.Code> value();
			}
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
		List<Class<? extends Story.State>> getRequires();

		List<? extends Point> getChildren();

		String getName();

		Story.Action getAction();
	}

	public interface Action {
		/*
		 * A code action.
		 */
		public interface Code extends Story.Action {
			void perform(Action.Context context) throws Exception;
		}

		/*
		 * Provides context access to the story context during action
		 * performance
		 */
		public interface Context {
			void log(Level info, String template, Object... args);
		}

		default Class<? extends Action> getActionClass() {
			return Reflections.at(this).provideAllImplementedInterfaces()
					.filter(intf -> Reflections.isAssignableFrom(Action.class,
							intf))
					.findFirst().get();
		}
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
