package com.termux.app.neon;

/** One button on the Neon Deck. */
public final class NeonAction {

    /** Text shown on the button. */
    public final String label;

    /** Shell command to run. A newline is appended, so it executes immediately. Can be empty if {@link #send} is used. */
    public final String command;

    /** Raw text to send to the terminal without a newline, for example "\u0003" for Ctrl+C. */
    public final String send;

    /** Open a new session first and run the command there. */
    public final boolean newSession;

    public NeonAction(String label, String command, String send, boolean newSession) {
        this.label = label;
        this.command = command == null ? "" : command;
        this.send = send == null ? "" : send;
        this.newSession = newSession;
    }

    /** The exact text written to the terminal for this action. */
    public String payload() {
        return send.isEmpty() ? command + "\n" : send;
    }
}
