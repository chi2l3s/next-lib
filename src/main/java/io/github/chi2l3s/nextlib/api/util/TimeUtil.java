package io.github.chi2l3s.nextlib.api.util;

public class TimeUtil {
    public static String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    public static int parseTime(String input) {
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Время не может быть пустым");
        }

        input = input.trim().toLowerCase();

        char unit = input.charAt(input.length() - 1);

        if (Character.isDigit(unit)) {
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Неверный формат числа: " + input);
            }
        }

        String numberPart = input.substring(0, input.length() - 1).trim();

        int number;
        try {
            number = Integer.parseInt(numberPart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Неверный формат числа: " + numberPart);
        }

        if (number < 0) {
            throw new IllegalArgumentException("Время не может быть отрицательным");
        }

        return switch (unit) {
            case 's' -> number;
            case 'm' -> number * 60;
            case 'h' -> number * 3600;
            case 'd' -> number * 86400;
            default -> throw new IllegalArgumentException("Неизвестная единица времени: " + unit + ". Используйте s, m, h или d");
        };
    }
}
