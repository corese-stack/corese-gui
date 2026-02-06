package fr.inria.corese.gui.core.service;

/**
 * Runtime exception for RDF conversion and parsing failures.
 */
public class RdfConversionException extends RuntimeException {

  public RdfConversionException(String message) {
    super(message);
  }

  public RdfConversionException(String message, Throwable cause) {
    super(message, cause);
  }
}
