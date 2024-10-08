package cc.alcina.framework.gwt.client.story;

import cc.alcina.framework.gwt.client.story.Story.State.Provider;

public interface TellerContext {
	void init(StoryTeller storyTeller);

	Provider resolveSatisfies(Class<? extends Story.State> state);

	/**
	 * Get a context part. An example is the webdriver teller configuration,
	 * with properties like "shouldMaximiseTab"
	 * 
	 * @param <P>
	 *            The part type parameter
	 * @param clazz
	 *            The part type, must be a concrete class (the default part
	 *            implementation) with a no-args constructor
	 * @return The configured part, or the default
	 */
	<P extends Part> P getPart(Class<? extends Part> clazz);

	/**
	 * <p>
	 * A context part - often used to configure a StoryTeller process class
	 * (provided by the peer).
	 * 
	 * <p>
	 * The existence of parts is broadcast on story start via
	 * <code>Registry.optional(PartConfigurable.class,(part ckass)).ifPresent...configure...</code>
	 * - which allows relatively decoupled story observation (see
	 * {@link cc.alcina.framework.servlet.story.doc.StoryDoc})
	 */
	public interface Part {
	}

	public interface PartConfigurable<P extends Part> {
		void configure(StoryTeller teller, P part);
	}

	boolean isThrowOnFailure();
}
