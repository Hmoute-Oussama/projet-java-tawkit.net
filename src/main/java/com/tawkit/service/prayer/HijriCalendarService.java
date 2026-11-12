package com.tawkit.service.prayer;

import com.tawkit.model.common.HijriDate;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

/**
 * Hijri (Islamic) calendar conversion using the widely-used tabular
 * ("Kuwaiti") algorithm. Accuracy is ±1–2 days vs. moon sighting, which is
 * why an optional manual offset (in days) is supported, like Tawkit does.
 */
public final class HijriCalendarService {

    /** Transliterated Arabic month names. */
    private static final List<String> MONTHS = List.of(
            "Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani",
            "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah");

    private int offsetDays = 0;

    public void setOffsetDays(int offsetDays) {
        this.offsetDays = offsetDays;
    }

    public int getOffsetDays() {
        return offsetDays;
    }

    /** Converts a Gregorian date to its Hijri equivalent. */
    public HijriDate toHijri(LocalDate date) {
        int jdn = gregorianToJdn(date.getYear(), date.getMonthValue(), date.getDayOfMonth()) + offsetDays;
        int[] dmy = jdnToIslamic(jdn);
        int day = dmy[0], month = dmy[1], year = dmy[2];
        String name = (month >= 1 && month <= 12) ? MONTHS.get(month - 1) : "?";
        return new HijriDate(day, month, year, name);
    }

    /** Localized month name, e.g. for FR/AR UIs. */
    public String monthName(int month, Locale locale) {
        if (month < 1 || month > 12) return "";
        if ("ar".equals(locale.getLanguage())) {
            String[] ar = {"محرم", "صفر", "ربيع الأول", "ربيع الثاني", "جمادى الأولى",
                    "جمادى الثانية", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة"};
            return ar[month - 1];
        }
        if ("fr".equals(locale.getLanguage())) {
            String[] fr = {"Mouharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Joumada al-Awwal",
                    "Joumada al-Thania", "Rajab", "Chaabane", "Ramadan", "Chawwal",
                    "Dhou al-Qi'da", "Dhou al-Hijja"};
            return fr[month - 1];
        }
        return MONTHS.get(month - 1);
    }

    /**
     * Days from today until 1 Ramadan of the current Hijri year (or next year
     * if Ramadan already passed). Negative/zero means Ramadan has started.
     */
    public long daysUntilRamadan(LocalDate today) {
        HijriDate todayH = toHijri(today);
        int targetJdn = islamicToJdn(todayH.getYear(), 9, 1);
        if (targetJdn < gregorianToJdn(today.getYear(), today.getMonthValue(), today.getDayOfMonth())) {
            targetJdn = islamicToJdn(todayH.getYear() + 1, 9, 1);
        }
        return targetJdn - gregorianToJdn(today.getYear(), today.getMonthValue(), today.getDayOfMonth());
    }

    /** True on Monday and Thursday — voluntary fasting days. */
    public static boolean isFastingDay(LocalDate date) {
        switch (date.getDayOfWeek()) {
            case MONDAY:
            case THURSDAY:
                return true;
            default:
                return false;
        }
    }

    // ------------------------------------------------ tabular algorithms

    private static int gregorianToJdn(int year, int month, int day) {
        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
    }

    private static int[] jdnToIslamic(int jdn) {
        long l = (long) jdn - 1948440L + 10632L;
        long n = (l - 1) / 10631;
        l = l - 10631 * n + 354;
        long j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238);
        l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29;
        int m = (int) ((24 * l) / 709);
        int d = (int) (l - (709 * m) / 24);
        int y = (int) (30 * n + j - 30);
        return new int[]{d, m, y};
    }

    private static int islamicToJdn(int year, int month, int day) {
        // Approximate inverse of the tabular calendar: iterate forward.
        int jdn = gregorianToJdn(2000, 1, 1) + (year - 1420) * 354;
        int[] dmy = jdnToIslamic(jdn);
        while (dmy[2] < year || (dmy[2] == year && dmy[1] < month)) {
            jdn += 30;
            dmy = jdnToIslamic(jdn);
        }
        while (dmy[2] > year || (dmy[2] == year && dmy[1] > month) || dmy[0] > day) {
            jdn -= 1;
            dmy = jdnToIslamic(jdn);
            if (dmy[2] < year || (dmy[2] == year && dmy[1] < month)) {
                jdn += 1;
                break;
            }
        }
        return jdn;
    }
}
