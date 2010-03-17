package cc.alcina.framework.common.client.logic.domain;

import java.util.Comparator;

import cc.alcina.framework.common.client.util.CommonUtils;

public interface HasOrderValue extends HasId{
	public Integer getOrderValue();

	public void setOrderValue(Integer value);

	public Integer indexInParentCollection();

	public static final Comparator<HasOrderValue> COMPARATOR = new Comparator<HasOrderValue>() {
		public int compare(HasOrderValue o1, HasOrderValue o2) {
			if (o1 == null) {
				return o2 == null ? 0 : -1;
			}
			if (o2==null){
				return 1;
			}
			int r = CommonUtils.compareWithNullMinusOne(o1.getOrderValue(), o2
					.getOrderValue());
			if (r != 0) {
				return r;
			}
			return  new Long(o1.getId()).compareTo( o2
					.getId());
		}
	};
}
