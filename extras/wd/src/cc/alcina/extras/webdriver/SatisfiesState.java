package cc.alcina.extras.webdriver;

public interface SatisfiesState<S extends SatisfiesState> {
	boolean satisfiesState(S state);
}