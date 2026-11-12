package com.tawkit.model.common;

/**
 * Simple representation of a Hijri (Islamic) date.
 */
public class HijriDate {

    private final int day;
    private final int month;
    private final int year;
    private final String monthName;

    public HijriDate(int day, int month, int year, String monthName) {
        this.day = day;
        this.month = month;
        this.year = year;
        this.monthName = monthName;
    }

    public int getDay() {
        return day;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public String getMonthName() {
        return monthName;
    }

    /**
     * Formatted display string, e.g. "15 Ramadan 1446".
     */
    public String toDisplayString() {
        return day + " " + monthName + " " + year + " AH";
    }

    @Override
    public String toString() {
        return toDisplayString();
    }
}
