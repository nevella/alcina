package cc.alcina.framework.entity.entityaccess.cache;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

public interface RetargetableDataSource extends DataSource {
	void setConnectionUrl(String newUrl);

	public static class RetargetableDataSourceWrapper extends DataSourceAdapter
			implements RetargetableDataSource {
		private DataSource delegate;

		public RetargetableDataSourceWrapper(DataSource delegate) {
			this.delegate = delegate;
		}

		@Override
		public Connection getConnection() throws SQLException {
			return delegate.getConnection();
		}

		@Override
		public void setConnectionUrl(String newUrl) {
			throw new UnsupportedOperationException();
		}
	}
}
