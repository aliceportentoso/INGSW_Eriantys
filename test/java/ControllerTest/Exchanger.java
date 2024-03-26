package ControllerTest;

import Controller.Message;

/**
 * Interface that allows the exchanging of {@link Message}s between {@link Controller.Controller Controller}s.
 * <p>
 *     This interface has been designed to be used while testing the exchange of {@link Message}s between
 *     {@link Controller.Controller Controller}s via their {@link SocketHandlerMockup}.
 * </p>
 * @implSpec Both the sender and the receiver need to implement the interface in order to be useful.
 */
public interface Exchanger {
    /**
     * Consumes last received {@link Message} still not read by the caller from the sender.
     * @param sender The sender of the message.
     * @return The {@link Message} consumed.
     */
    Message receiveLastMessageFrom(Exchanger sender);

    /**
     * Consumes and asserts the correctness of last received {@link Message} still not read by the caller from the sender.
     * @param sender The sender of the message.
     * @param message The type of message that should be consumed. This parameter is intended to be used by the asserting
     *                test method in order to check its correctness.
     * @param context Context in which the message it has been exchanged. This is  intended to be a merely descriptive
     *                string used by the asserting test method to provide context on console when the assertion fails.
     */
    void receiveAndAssertLastMessageFrom(Exchanger sender, String message, String context);
}
