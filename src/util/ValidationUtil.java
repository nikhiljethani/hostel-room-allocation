package util;

import java.util.regex.Pattern;

/** Small helper methods for validating text typed by the user. */
public final class ValidationUtil {

    private static final Pattern ENROLLMENT = Pattern.compile("^[A-Za-z0-9/_-]{3,30}$");
    private static final Pattern PERSON_NAME = Pattern.compile("^[A-Za-z][A-Za-z .'-]{1,99}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{10,13}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern ROOM_NUMBER = Pattern.compile("^[A-Za-z0-9-]{1,10}$");

    private ValidationUtil() {
    }

    public static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static boolean isValidEnrollmentNo(String text) {
        return text != null && ENROLLMENT.matcher(text.trim()).matches();
    }

    public static boolean isValidName(String text) {
        return text != null && PERSON_NAME.matcher(text.trim()).matches();
    }

    /** 10 to 13 digits, optionally starting with '+'. */
    public static boolean isValidPhone(String text) {
        return text != null && PHONE.matcher(text.trim()).matches();
    }

    public static boolean isValidEmail(String text) {
        return text != null && EMAIL.matcher(text.trim()).matches();
    }

    public static boolean isValidRoomNumber(String text) {
        return text != null && ROOM_NUMBER.matcher(text.trim()).matches();
    }

    /** Parses a whole number; returns null if the text is not a valid integer. */
    public static Integer parseInteger(String text) {
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
