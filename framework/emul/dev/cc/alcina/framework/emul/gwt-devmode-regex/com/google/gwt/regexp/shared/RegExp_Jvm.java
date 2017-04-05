package com.google.gwt.regexp.shared;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.gwt.core.client.GwtScriptOnly;
@GwtScriptOnly
public class RegExp_Jvm implements IRegExp{

	  public static RegExp compile(String pattern) {
		  throw new UnsupportedOperationException();
	  }
	  public RegExp_Jvm(){
		  
	  }
	  public void yup(){
		  
	  }

	  /**
	   * Creates a regular expression object from a pattern using the given flags.
	   *
	   * @param pattern the Javascript regular expression pattern to compile
	   * @param flags the flags string, containing at most one occurrence of {@code
	   *        'g'} ({@link #getGlobal()}), {@code 'i'} ({@link #getIgnoreCase()}),
	   *        or {@code 'm'} ({@link #getMultiline()}).
	   * @return a new regular expression
	   * @throws RuntimeException if the pattern or the flags are invalid
	   */
	  public static RegExp compile(String pattern, String flags) {
	   throw new UnsupportedOperationException();
	    }


	  /**
	   * Returns a literal pattern <code>String</code> for the specified
	   * <code>String</code>.
	   *
	   * <p>This method produces a <code>String</code> that can be used to
	   * create a <code>RegExp</code> that would match the string
	   * <code>s</code> as if it were a literal pattern.</p> Metacharacters
	   * or escape sequences in the input sequence will be given no special
	   * meaning.
	   *
	   * @param  input The string to be literalized
	   * @return  A literal string replacement
	   */
	  public static String quote(String input) {
		  throw new UnsupportedOperationException();
	  }

	 

	  /**
	   * Applies the regular expression to the given string. This call affects the
	   * value returned by {@link #getLastIndex()} if the global flag is set.
	   *
	   * @param input the string to apply the regular expression to
	   * @return a match result if the string matches, else {@code null}
	   */
	  public MatchResult exec(String input) {
		  throw new UnsupportedOperationException();
	  }

	  /**
	   * Returns whether the regular expression captures all occurrences of the
	   * pattern.
	   */
	  public boolean getGlobal() {
		  throw new UnsupportedOperationException();
		  }

	  /**
	   * Returns whether the regular expression ignores case.
	   */
	  public boolean getIgnoreCase() {
		  throw new UnsupportedOperationException();
	  }

	  /**
	   * Returns whether '$' and '^' match line returns ('\n' and '\r') in addition
	   * to the beginning or end of the string.
	   */
	  public boolean getMultiline() {
		  throw new UnsupportedOperationException();
	  }

	  /**
	   * Returns the input string with the part(s) matching the regular expression
	   * replaced with the replacement string. If the global flag is set, replaces
	   * all matches of the regular expression. Otherwise, replaces the first match
	   * of the regular expression. As per Javascript semantics, backslashes in the
	   * replacement string get no special treatment, but the replacement string can
	   * use the following special patterns:
	   * <ul>
	   * <li>$1, $2, ... $99 - inserts the n'th group matched by the regular
	   * expression.
	   * <li>$&amp; - inserts the entire string matched by the regular expression.
	   * <li>$$ - inserts a $.
	   * </ul>
	   * Note: $` and $' are *not* supported in the pure Java implementation, and
	   * throw an exception.
	   *
	   * @param input the string in which the regular expression is to be searched.
	   * @param replacement the replacement string.
	   * @return the input string with the regular expression replaced by the
	   *         replacement string.
	   * @throws RuntimeException if {@code replacement} is invalid
	   */
	  public String replace(String input, String replacement) {
		  throw new UnsupportedOperationException();
		  }

	  /**
	   * Splits the input string around matches of the regular expression. If the
	   * regular expression is completely empty, splits the input string into its
	   * constituent characters. If the regular expression is not empty but matches
	   * an empty string, the results are not well defined.
	   *
	   * @param input the string to be split.
	   * @return the strings split off, any of which may be empty.
	   */
	  public SplitResult split(String input) {
		  throw new UnsupportedOperationException();
		  }

	  /**
	   * Splits the input string around matches of the regular expression. If the
	   * regular expression is completely empty, splits the input string into its
	   * constituent characters. If the regular expression is not empty but matches
	   * an empty string, the results are not well defined.
	   *
	   * Note: There are some browser inconsistencies with this implementation, as
	   * it is delegated to the browser, and no browser follows the spec completely.
	   * A major difference is that IE will exclude empty strings in the result.
	   *
	   * @param input the string to be split.
	   * @param limit the maximum number of strings to split off and return,
	   *        ignoring the rest of the input string. If negative, there is no
	   *        limit.
	   * @return the strings split off, any of which may be empty.
	   */
	  public SplitResult split(String input, int limit) {
		  throw new UnsupportedOperationException();
		  }

	  /**
	   * Determines if the regular expression matches the given string. This call
	   * affects the value returned by {@link #getLastIndex()} if the global flag is
	   * set. Equivalent to: {@code exec(input) != null}
	   *
	   * @param input the string to apply the regular expression to
	   * @return whether the regular expression matches the given string.
	   */
	  public boolean test(String input) {
		  throw new UnsupportedOperationException();
	  }
	  public int getLastIndex() {
		    throw new UnsupportedOperationException();
		  }


		  /**
		   * Returns the pattern string of the regular expression.
		   */
		  public String getSource() {
			  throw new UnsupportedOperationException();
		  }
		  public void setLastIndex(int lastIndex) {
			  throw new UnsupportedOperationException();
		  }
	}
