package org.ikasan.dashboard.ui.scheduler.util;

public class ControlCharacterUtils {

    /**
     * Formats the given input string based on the specified control character type.
     *
     * @param input the original input string to be formatted
     * @param controlCharacter the type of control character to use for formatting (WINDOWS or UNIX)
     * @return the input string formatted with the appropriate line endings based on the control character type
     */
    public static String format(String input, ControlCharacter controlCharacter) {
        input = input.replace("\r\n", "\n");
        input = input.replace("\r", "\n");

        switch (controlCharacter) {
            case WINDOWS:
                return input.replace("\n", "\r\n");
            case UNIX:
            default:
                return input;
        }
    }

    public enum ControlCharacter {
        WINDOWS, UNIX;
    }
}
