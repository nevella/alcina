package cc.alcina.framework.common.client.csobjects;

public interface IsBindable {
	default Bindable provideBindable() {
		return (Bindable) this;
	}
}
