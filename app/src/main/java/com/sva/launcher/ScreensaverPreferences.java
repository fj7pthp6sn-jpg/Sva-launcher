package com.sva.launcher;

import android.content.SharedPreferences;
import java.net.URI;
import java.util.Locale;

final class ScreensaverPreferences {
    static final String ALBUM = "screensaverAlbum";
    static final String THEME = "screensaverTheme";
    static final String INTERVAL = "screensaverIntervalSeconds";
    static final String CAPTION = "screensaverCaption";
    static final String IDLE = "screensaverIdleMinutes";
    static final int[] IDLE_MINUTES = {0, 1, 3, 5, 10};
    static final String CLOCK = "screensaverClock";
    static final String WEATHER = "screensaverWeather";
    static final String EVENTS = "screensaverEvents";
    static final String[] THEMES = {"cinema", "darkroom", "editorial", "floating_prints", "monolith", "prism", "silhouette", "travel_journal", "photo_weave"};
    static final String[] NAMES = {"Cinema", "Darkroom", "Editorial", "Floating Prints", "Monolith", "Prism", "Silhouette", "Travel Journal", "Photo Weave"};
    static final String[] DESCRIPTIONS = {
            "Yavaş kamera hareketi, yumuşak siyah kenar gölgesi ve sakin bir sinematik geçiş.",
            "Kehribar ışıklı bir kontak sayfası, sıcak fotoğraflar ve geçip giden bir ışık izi.",
            "Büyük boy bir fotoğraf, fildişi kenar boşlukları ve özenle yerleştirilmiş kişisel alt yazılar.",
            "Albüm fotoğrafları, görsellerinizden alınan renklerin önünde katmanlı kâğıt baskılar gibi süzülür.",
            "Heykelsi, süzülen bir fotoğraf, görselden ilham alan derin renkler ve sakin hareket.",
            "Yarı saydam yüzeyler, zümrüt ışık şeritleri ve parlak bir fotoğraf merkezi.",
            "Geniş, hareketli dikey panellerle ortaya çıkan siyah beyaz fotoğrafçılık.",
            "Sıcak dokulu kâğıt, fotoğraflı hatıralar ve zarif rota çizgileri.",
            "Farklı albüm fotoğrafları, yavaşça değişen asimetrik bir ızgarada iç içe geçer."
    };
    static final int[] INTERVALS = {12, 20, 30, 60};
    private ScreensaverPreferences() { }
    static String album(SharedPreferences prefs) {
        return resolveAlbum(prefs.getString(ALBUM, ""), prefs.getString("wallpaperAlbum", ""));
    }
    static String resolveAlbum(String custom, String home) {
        return custom == null || custom.trim().isEmpty() ? (home == null ? "" : home.trim()) : custom.trim();
    }
    static boolean validAlbum(String value) {
        if (value.isEmpty()) return true;
        try {
            URI uri = new URI(value);
            String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
            return "https".equalsIgnoreCase(uri.getScheme()) && (host.equals("photos.google.com")
                    || host.equals("photos.app.goo.gl") || host.equals("goo.gl"));
        } catch (Exception e) { return false; }
    }
    static int themeIndex(String theme) {
        String[] legacy = {"cinema", "vintage", "memory", "postcards", "gallery", "aurora", "noir", "storybook", "mosaic"};
        for (int i = 0; i < legacy.length; i++) if (legacy[i].equals(theme)) return i;
        for (int i = 0; i < THEMES.length; i++) if (THEMES[i].equals(theme)) return i;
        return 0;
    }
    static int idleMinutes(int value) {
        for (int allowed : IDLE_MINUTES) if (allowed == value) return value;
        return 0;
    }
    static int interval(int seconds) {
        for (int allowed : INTERVALS) if (allowed == seconds) return seconds;
        return 20;
    }
}
