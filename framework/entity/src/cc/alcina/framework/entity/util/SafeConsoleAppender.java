package cc.alcina.framework.entity.util;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;

import cc.alcina.framework.common.client.util.Ax;

public class SafeConsoleAppender extends ConsoleAppender {
	public SafeConsoleAppender() {
	    if(!Ax.isTest()){
	        throw new RuntimeException("Non-server only");
	    }
	}

	public SafeConsoleAppender(Layout layout) {
		super(layout);
		if(!Ax.isTest()){
            throw new RuntimeException("Non-server only");
        }
	}

	
}