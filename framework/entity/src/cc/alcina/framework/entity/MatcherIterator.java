package cc.alcina.framework.entity;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;

public class MatcherIterator implements Iterator<String> {
	private Matcher matcher;

	boolean peeked = false;

	String nextMatch;

	boolean finished = false;

	private int group;

	public MatcherIterator(Matcher matcher, int group) {
		this.matcher = matcher;
		this.group = group;
	}

	@Override
	public boolean hasNext() {
		ensurePeeked();
		return !finished;
	}

	@Override
	public String next() {
		ensurePeeked();
		if (finished) {
			throw new NoSuchElementException();
		}
		peeked = false;
		return nextMatch;
	}

	private void ensurePeeked() {
		if (!peeked) {
			peeked = true;
			finished = !matcher.find();
			if (!finished) {
				nextMatch = matcher.group(group);
			}
		}
	}
}