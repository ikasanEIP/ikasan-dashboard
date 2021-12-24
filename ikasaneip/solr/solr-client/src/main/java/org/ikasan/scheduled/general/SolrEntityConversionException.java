package org.ikasan.scheduled.general;

public class SolrEntityConversionException extends RuntimeException {

    public SolrEntityConversionException() {
    }

    public SolrEntityConversionException(String message) {
        super(message);
    }

    public SolrEntityConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    public SolrEntityConversionException(Throwable cause) {
        super(cause);
    }

    public SolrEntityConversionException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
