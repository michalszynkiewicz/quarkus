package io.quarkus.reactivemessaging.http;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 *         <br>
 *         Date: 09/07/2019
 */
public class PayloadParsingException extends Exception {

    public PayloadParsingException(String message) {
        super(message);
    }

    public PayloadParsingException(String message, Exception cause) {
        super(message, cause);
    }
}
