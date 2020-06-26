package cc.alcina.framework.servlet.servlet.control;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.entity.entityaccess.cache.mvcc.Transaction;

public class MvccFilter implements Filter {
	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest arg0, ServletResponse arg1,
			FilterChain chain) throws IOException, ServletException {
		try {
			LooseContext.push();
			Transaction.begin();
			chain.doFilter(arg0, arg1);
		} finally {
			Transaction.end();
			LooseContext.pop();
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
}
