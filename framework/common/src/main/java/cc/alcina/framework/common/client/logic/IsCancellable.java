package cc.alcina.framework.common.client.logic;

public interface IsCancellable {
	public abstract boolean isCancelled();

	public abstract void setCancelled(boolean cancelled);
}
