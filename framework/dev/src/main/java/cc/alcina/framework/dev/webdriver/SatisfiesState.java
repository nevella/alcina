package cc.alcina.framework.dev.webdriver;

public interface SatisfiesState<S extends SatisfiesState> {
	boolean satisfiesState(S state);
}