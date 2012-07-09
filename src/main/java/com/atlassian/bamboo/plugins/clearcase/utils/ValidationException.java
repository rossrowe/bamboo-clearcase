package com.atlassian.bamboo.plugins.clearcase.utils;

/**
 * Used to indicate some sort of validation error occured.
 */
public class ValidationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Automatically adds the cause to the message.
	 * 
	 * @param message The statt of trhe details message to add " cause by " to.
	 * @param cause the cause.
	 */
	public ValidationException(String message, Throwable cause) {
		super(message + " caused by: "+cause.getMessage(), cause);
	}

	/**
	 * New exception just iwth message no cause.
	 * @param message the detailed message
	 */
	public ValidationException(String message) {
		super(message);
	}
}