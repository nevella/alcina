/*
 * @(#)ThreadLocal.java	1.42 06/06/23
 *
 * Copyright 2006 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package java.lang;

/**
 * Thanks to the java dudes
 */
public class Thread {
	public static void sleep(long millis) throws Exception {
	}
	public long getId(){
		return 0;
	}
	public static Thread currentThread(){
		return new Thread();
	}
	public StackTraceElement[] getStackTrace(){
		return new StackTraceElement[0];
	}
}
