package com.atlassian.bamboo.plugins.clearcase.utils;

/**
 * Processs supplied string data by breaking it up into tokens based on a
 * delimiter expresssion.  The default delimter expression breaks the string 
 * into seperate lines.
 * <p>
 * Each token found is passed to {{@link #processToken(String)} for a subclasses
 * to do what they like with.
 */
public abstract class AbstractTokenProcessor {

	private String data;

	private int tokenCount = 0;

	private boolean trim = true;

	private static String DEFAULT_DELIMS = "[\n\f\r]";

	private String delimExp = DEFAULT_DELIMS;

	/** if true any token that is an empty after applying {#trim} is ignored * */
	private boolean ignoreEmptyToken = true;

	public AbstractTokenProcessor(String data) {
		this.data = data;
	}

	public AbstractTokenProcessor(String data, String delimExp) {
		this(data);
		setDelimExp(delimExp);
	}

	/**
	 * Set the regexp used to split data into tokens, if null then
	 * default {@link #DEFAULT_DELIMS} is set.
	 * 
	 * @param delimExp the delimer regular epression
	 * 
	 * @see java.util.regex.Pattern for details of the delimExp
	 */
	public void setDelimExp(String delimExp) {
		this.delimExp = delimExp == null ? DEFAULT_DELIMS : delimExp;
	}

	/**
	 * Process the data supplied by break it up into tokens
	 */
	public void process() {
		String[] tokens = data.split(delimExp);
		for (int i = 0; i < tokens.length; i++) {
			String currentToken = tokens[i];
			if (trim) {
				currentToken = currentToken.trim();
			}
			if (ignoreEmptyToken && currentToken.length() == 0) {
				// ignore token
			} else {
				tokenCount++;
				processToken(currentToken);
			}
		}
	}

	/**
	 * Passed each token found in the data. If {{@link #trim} is true, the
	 * default, then lines are trimmed before being passed.
	 * If {{@link #ignoreEmptyToken} is true then not empty tokens, ie zero in 
	 * lenght, are passed to the method.
	 * 
	 * @param token
	 *            a token to processs.
	 */
	protected abstract void processToken(String token);

	/**
	 * The number of tokens that were processed (or have been processed so far.
	 * 
	 * @return the number of lines processed, will always be zero if dat has not
	 *         yet been procesed.
	 */
	public int getTokenCount() {
		return tokenCount;
	}

	/**
	 * Test if lines aqre being trimmed befre passing to {{@link #processToken(String)},
	 * default is true.
	 * 
	 * @return true if trimming false if not
	 */
	public boolean isTrimLines() {
		return trim;
	}

	/**
	 * Turn on or off line timming.
	 * 
	 * @param trimLinestrue
	 *            to trim, false to not
	 */
	public void setTrimLines(boolean trimLines) {
		this.trim = trimLines;
	}

	public boolean isIgnoreEmptyToken() {
		return ignoreEmptyToken;
	}

	public void setIgnoreEmptyToken(boolean ignoreEmptyToken) {
		this.ignoreEmptyToken = ignoreEmptyToken;
	}

}
