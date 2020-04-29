package cc.alcina.framework.entity.entityaccess.cache;

import javax.sql.DataSource;

public interface RetargetableDataSource extends DataSource {
	 void setConnectionUrl(String newUrl);
}
