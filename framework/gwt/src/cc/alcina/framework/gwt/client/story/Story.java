package cc.alcina.framework.gwt.client.story;

import java.lang.System.Logger.Level;
import java.lang.annotation.Annotation;
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
import cc.alcina.framework.gwt.client.story.Story.Action.Location;
import cc.alcina.framework.gwt.client.story.Story.Action.Location.Axis;
import cc.alcina.framework.gwt.client.story.StoryTeller.Visit;
import cc.alcina.framework.gwt.client.util.LineCallback;

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
	default Class<? extends Feature> getFeature() {
		return getPoint().getFeature();
	}

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
	 * A naming container (short for 'Declarative'). The single/repeated pattern
	 * is for annotation readability
	 */
	public interface Decl {
		/** Declaratively define a point. This may end up going unused */
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

		@Retention(RetentionPolicy.RUNTIME)
		@Documented
		@Target({ ElementType.TYPE })
		@Repeatable(Requires.class)
		public @interface Require {
			Class<? extends Story.State> value();
		}

		@Retention(RetentionPolicy.RUNTIME)
		@Documented
		@Target({ ElementType.TYPE })
		public @interface Requires {
			Require[] value();
		}

		@Retention(RetentionPolicy.RUNTIME)
		@Documented
		@Target({ ElementType.TYPE })
		@Repeatable(Children.class)
		public @interface Child {
			Class<? extends Story.Point> value();
		}

		@Retention(RetentionPolicy.RUNTIME)
		@Documented
		@Target({ ElementType.TYPE })
		public @interface Children {
			Child[] value();
		}

		@Retention(RetentionPolicy.RUNTIME)
		@Documented
		@Target({ ElementType.TYPE })
		public @interface Feature {
			Class<? extends cc.alcina.framework.common.client.meta.Feature> value();
		}

		/*
		 * A todo note
		 */
		@Retention(RetentionPolicy.RUNTIME)
		@Documented
		@Target({ ElementType.TYPE })
		public @interface Todo {
			String value();
		}

		/** Declarative actions */
		public interface Action {
			/*
			 * Possibly unused
			 */
			/** Declaratively define a code action */
			@Retention(RetentionPolicy.RUNTIME)
			@Documented
			@Target({ ElementType.TYPE })
			public @interface Code {
				Class<? extends Story.Action.Code> value();
			}

			/**
			 * UI actions. Could be done as annotation/type - but this way reads
			 * better. This includes navigation actions
			 */
			public interface UI {
				@Registration.NonGenericSubtypes(Converter.class)
				public interface Converter<A extends Annotation>
						extends Registration.AllSubtypes {
					Story.Action convert(A ann);
				}

				/** A click action */
				@Retention(RetentionPolicy.RUNTIME)
				@Documented
				@Target({ ElementType.TYPE })
				@Registration(Action.UI.class)
				public @interface Click {
					public static class ConverterImpl
							implements Converter<Click> {
						@Override
						public Story.Action convert(Click ann) {
							return new Story.Action.Ui.Click();
						}
					}
				}

				/** A (send) keys action */
				@Retention(RetentionPolicy.RUNTIME)
				@Documented
				@Target({ ElementType.TYPE })
				@Registration(Action.UI.class)
				public @interface Keys {
					String value();

					public static class ConverterImpl
							implements Converter<Keys> {
						@Override
						public Story.Action convert(Keys ann) {
							return new Story.Action.Ui.Keys()
									.withText(ann.value());
						}
					}
				}

				public interface Select {
					/** Define a select-by-value action */
					@Retention(RetentionPolicy.RUNTIME)
					@Documented
					@Target({ ElementType.TYPE })
					@Registration(Action.UI.class)
					public @interface ByValue {
						String value();

						public static class ConverterImpl
								implements Converter<ByValue> {
							@Override
							public Story.Action convert(ByValue ann) {
								return new Story.Action.Ui.SelectByValue()
										.withText(ann.value());
							}
						}
					}

					/** A select-by-text action */
					@Retention(RetentionPolicy.RUNTIME)
					@Documented
					@Target({ ElementType.TYPE })
					@Registration(Action.UI.class)
					public @interface ByText {
						String value();

						public static class ConverterImpl
								implements Converter<ByText> {
							@Override
							public Story.Action convert(ByText ann) {
								return new Story.Action.Ui.SelectByText()
										.withText(ann.value());
							}
						}
					}
				}

				public interface Navigation {
					/** A go-to-url action */
					@Retention(RetentionPolicy.RUNTIME)
					@Documented
					@Target({ ElementType.TYPE })
					@Registration(Action.UI.class)
					public @interface Go {
						public static class ConverterImpl
								implements Converter<Go> {
							@Override
							public Story.Action convert(Go ann) {
								return new Story.Action.Ui.Go();
							}
						}
					}

					/** A refresh-url action */
					@Retention(RetentionPolicy.RUNTIME)
					@Documented
					@Target({ ElementType.TYPE })
					@Registration(Action.UI.class)
					public @interface Refresh {
						public static class ConverterImpl
								implements Converter<Refresh> {
							@Override
							public Story.Action convert(Refresh ann) {
								return new Story.Action.Ui.Refresh();
							}
						}
					}
				}
			}
		}

		/** Declarative locations */
		public interface Location {
			@Registration.NonGenericSubtypes(Converter.class)
			public interface Converter<A extends Annotation>
					extends Registration.AllSubtypes {
				Story.Action.Location convert(A ann);
			}

			/** Define an xpath location */
			@Retention(RetentionPolicy.RUNTIME)
			@Documented
			@Target({ ElementType.TYPE })
			public @interface Xpath {
				String value();

				public static class ConverterImpl implements Converter<Xpath> {
					@Override
					public Story.Action.Location convert(Xpath ann) {
						return new Story.Action.Location.Xpath()
								.withText(ann.value());
					}
				}
			}

			/** Define an URL location */
			@Retention(RetentionPolicy.RUNTIME)
			@Documented
			@Target({ ElementType.TYPE })
			public @interface Url {
				String value();

				public static class ConverterImpl implements Converter<Url> {
					@Override
					public Story.Action.Location convert(Url ann) {
						return new Story.Action.Location.Url()
								.withText(ann.value());
					}
				}
			}
		}

		/** The point's label (for rendering in the UI or logs) */
		@Retention(RetentionPolicy.RUNTIME)
		@Documented
		@Target({ ElementType.TYPE })
		public @interface Label {
			String value();
		}

		/** The point's description */
		@Retention(RetentionPolicy.RUNTIME)
		@Documented
		@Target({ ElementType.TYPE })
		public @interface Description {
			String value();
		}

		/** Documentation hints */
		public interface Doc {
			/**
			 * Highlight the first matched UI node
			 */
			@Retention(RetentionPolicy.RUNTIME)
			@Documented
			@Target({ ElementType.TYPE })
			public @interface HighlightUiNode {
			}
		}

		/**
		 * Conditional execution attributes
		 */
		public interface Conditional {
			/**
			 * Child traversal will halt if a conditional (test) child returns
			 * false
			 */
			@Retention(RetentionPolicy.RUNTIME)
			@Documented
			@Target({ ElementType.TYPE })
			public @interface Traversal {
			}

			/**
			 * Invert the test result for ascent propagation (e.g. if the point
			 * is a dom existence test, the evaluated test result should be true
			 * if the node <i>doesn't</i> exist)
			 */
			@Retention(RetentionPolicy.RUNTIME)
			@Documented
			@Target({ ElementType.TYPE })
			public @interface Invert {
			}
		}
	}

	/**
	 * <p>
	 * Elements of a story. Progres characteristics are:
	 * <ul>
	 * <li>Specify an app location (url)
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

		Class<? extends Feature> getFeature();

		Location getLocation();
	}

	public interface Action {
		/*
		 * A code action.
		 */
		public interface Code extends Story.Action {
			void perform(Action.Context context) throws Exception;
		}

		/** A UI Location */
		public interface Location {
			public enum Axis {
				URL, DOCUMENT
			}

			Axis getAxis();

			String getText();

			abstract static class LocWithText implements Location {
				String text;

				public String getText() {
					return text;
				}

				public void setText(String text) {
					this.text = text;
				}

				public Location withText(String text) {
					setText(text);
					return this;
				}
			}

			public static class Xpath extends LocWithText {
				@Override
				public Axis getAxis() {
					return Axis.DOCUMENT;
				}
			}

			public static class Url extends LocWithText {
				@Override
				public Axis getAxis() {
					return Axis.URL;
				}
			}
		}

		/*
		 * A UI action.
		 */
		public interface Ui extends Story.Action {
			default String text() {
				return null;
			}

			public static class Click implements Ui {
			}

			public static class Go implements Ui {
			}

			public static class Refresh implements Ui {
			}

			abstract static class ActionWithText implements Ui {
				String text;

				public String getText() {
					return text;
				}

				public void setText(String text) {
					this.text = text;
				}

				public Ui withText(String text) {
					setText(text);
					return this;
				}
			}

			public static class Keys extends ActionWithText {
			}

			public static class SelectByText extends ActionWithText {
			}

			public static class SelectByValue extends ActionWithText {
			}
		}

		/*
		 * Provides context access to the story context during action
		 * performance
		 */
		public interface Context {
			default void log(String template, Object... args) {
				log(Level.INFO, template, args);
			}

			void log(Level level, String template, Object... args);

			LineCallback createLogCallback(Level warning);

			Visit getVisit();

			/**
			 * Returns a story-singleton PerformerResource object of type PR,
			 * with initialise() called (creating/initialising if need be)
			 * 
			 * @param <PR>
			 *            The PerformerResource type parameter
			 * @param clazz
			 *            The PerformerResource type
			 * @return The PerformerResource object
			 */
			<PR extends PerformerResource> PR
					performerResource(Class<PR> clazz);

			public interface PerformerResource {
				default void initialise(Context context) {
				}
			}

			<L extends Location> L getLocation(Axis url);
		}

		default Class<? extends Action> getActionClass() {
			return Reflections.at(this).provideAllImplementedInterfaces()
					.filter(intf -> Reflections.isAssignableFrom(Action.class,
							intf))
					.findFirst().get();
		}

		/*
		 * The action performer must set the Context.Visit.Result.testResult to
		 * true or false
		 */
		public interface Test {
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
