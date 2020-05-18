package cc.alcina.framework.common.client.domain;

public interface FilterCost {
	public double estimatedMatchFraction();

	public double perIncomingEntityCost();

	public double perOutgoingEntityCost();
}