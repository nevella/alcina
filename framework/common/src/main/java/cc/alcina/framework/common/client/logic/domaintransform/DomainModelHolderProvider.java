package cc.alcina.framework.common.client.logic.domaintransform;

public interface DomainModelHolderProvider<DM extends DomainModelHolder> {
	public DM getDomainModelHolder();
}
