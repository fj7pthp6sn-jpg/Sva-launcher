package com.sva.launcher;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.*;
import android.view.Gravity;
import android.view.View;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ScreensaverSettingsActivity extends MotionActivity {
    private SharedPreferences prefs;
    private LinearLayout page;
    private EditText album, caption;
    private TextView source, feedback;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final LifecycleWork lifecycleWork = new LifecycleWork(this, new android.os.Handler(android.os.Looper.getMainLooper()));
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        BackNavigation.install(this, this::requestClose);
        prefs = getSharedPreferences("launcher", MODE_PRIVATE);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                | android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        LinearLayout shell = SettingsUi.page(this, "Ekran koruyucu", "Sva Memories · Boşta ekranınızı kişiselleştirin", this::requestClose);
        page = SettingsUi.scroll(shell);
        SettingsUi.heading(page, "Fotoğraf kaynağı");
        source = SettingsUi.text(this, "", 14, true); page.addView(source); updateSource();
        album = input("Ayrı Google Fotoğraflar albümü (isteğe bağlı)", state == null
                ? prefs.getString(ScreensaverPreferences.ALBUM, "") : state.getString("album", ""));
        SettingsUi.hint(page, "Ana Sayfa albümünü izlemek için boş bırakın. Bağlantıya sahip herkesle paylaşılan bir albüm kullanın.");
        caption = input("Alt yazı (isteğe bağlı)", state == null
                ? prefs.getString(ScreensaverPreferences.CAPTION, "") : state.getString("caption", ""));
        feedback = SettingsUi.text(this, "", 14, true); SettingsUi.add(page, feedback);
        SettingsUi.add(page, SettingsUi.action(this, "Ana Sayfa albümünü kullan", () -> {
            album.setText(""); prefs.edit().remove(ScreensaverPreferences.ALBUM).apply();
            updateSource(); feedback.setText("Ekran koruyucu artık Ana Sayfa albümünü izliyor.");
        }));
        SettingsUi.add(page, SettingsUi.action(this, "Albümü kontrol et", this::checkAlbum));
        SettingsUi.heading(page, "Görünüm");
        int atmosphere = ScreensaverPreferences.themeIndex(prefs.getString(ScreensaverPreferences.THEME, "cinema"));
        TextView atmosphereDescription = SettingsUi.text(this, ScreensaverPreferences.DESCRIPTIONS[atmosphere], 14, true);
        SettingsUi.add(page, SettingsUi.choice(this, "Atmosfer", ScreensaverPreferences.NAMES,
                atmosphere, index -> {
                    prefs.edit().putString(ScreensaverPreferences.THEME, ScreensaverPreferences.THEMES[index]).apply();
                    atmosphereDescription.setText(ScreensaverPreferences.DESCRIPTIONS[index]);
                }));
        SettingsUi.add(page, atmosphereDescription);
        int interval = ScreensaverPreferences.interval(prefs.getInt(ScreensaverPreferences.INTERVAL, 20));
        String[] intervals = new String[ScreensaverPreferences.INTERVALS.length];
        int selected = 0;
        for (int i = 0; i < intervals.length; i++) {
            intervals[i] = ScreensaverPreferences.INTERVALS[i] + " saniye";
            if (ScreensaverPreferences.INTERVALS[i] == interval) selected = i;
        }
        SettingsUi.add(page, SettingsUi.choice(this, "Fotoğrafı değiştir", intervals, selected,
                index -> prefs.edit().putInt(ScreensaverPreferences.INTERVAL, ScreensaverPreferences.INTERVALS[index]).apply()));
        SettingsUi.heading(page, "Katmanlar");
        overlayToggle("Saat", ScreensaverPreferences.CLOCK, true);
        overlayToggle("Hava Durumu", ScreensaverPreferences.WEATHER, false);
        overlayToggle("Etkinlikler", ScreensaverPreferences.EVENTS, false);
        SettingsUi.hint(page, "Hava durumu ve etkinlikler Ana Sayfa konumunuzu ve takviminizi kullanır.");
        SettingsUi.heading(page, "Etkinleştirme");
        String[] idleOptions = new String[ScreensaverPreferences.IDLE_MINUTES.length];
        int idle = ScreensaverPreferences.idleMinutes(prefs.getInt(ScreensaverPreferences.IDLE, 0));
        selected = 0;
        for (int i = 0; i < idleOptions.length; i++) {
            int minutes = ScreensaverPreferences.IDLE_MINUTES[i];
            idleOptions[i] = minutes == 0 ? "Kapalı" : minutes + (minutes == 1 ? " dakika" : " dakika");
            if (idle == minutes) selected = i;
        }
        SettingsUi.add(page, SettingsUi.choice(this, "Sva boştayken başlat", idleOptions, selected,
                index -> prefs.edit().putInt(ScreensaverPreferences.IDLE, ScreensaverPreferences.IDLE_MINUTES[index]).apply()));
        SettingsUi.hint(page, "Sva açıkken çalışır. Oynatma ve ayarlar kesintiye uğramaz.");
        SettingsUi.add(page, SettingsUi.action(this, "Android TV ayarlarında seç", this::openScreensaverSettings));
        SettingsUi.hint(page, "Sistem genelinde ekran koruyucu için Android TV ayarlarında Sva Memories’i seçin. Daha önce indirilen fotoğraflar çevrimdışı çalışır.");
        LinearLayout footer = new LinearLayout(this);
        footer.setClipChildren(false);
        footer.setClipToPadding(false);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.setMargins(0, dp(10), 0, dp(12));
        Button save = SettingsUi.action(this, "Değişiklikleri kaydet", this::save);
        save.setTextColor(LiquidTheme.accent(this));
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, -2, 1);
        saveParams.setMargins(dp(4), dp(4), dp(6), dp(4));
        footer.addView(save, saveParams);
        Button preview = SettingsUi.action(this, "Önizleme", () -> {
            if (save()) startActivity(new Intent(this, ScreensaverPreviewActivity.class));
        });
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(0, -2, 1);
        previewParams.setMargins(dp(6), dp(4), dp(4), dp(4));
        footer.addView(preview, previewParams);
        shell.addView(footer, footerParams);
        setContentView(shell); save.requestFocus();
        if (state != null) page.post(() -> ((ScrollView) page.getParent()).scrollTo(0, state.getInt("scroll", 0)));
    }

    private void overlayToggle(String label, String key, boolean fallback) {
        SettingsUi.add(page, SettingsUi.toggle(this, label, prefs.getBoolean(key, fallback),
                enabled -> prefs.edit().putBoolean(key, enabled).apply()));
    }

    private void requestClose() {
        boolean dirty = !album.getText().toString().trim().equals(prefs.getString(ScreensaverPreferences.ALBUM, ""))
                || !caption.getText().toString().trim().equals(prefs.getString(ScreensaverPreferences.CAPTION, ""));
        if (!dirty) { finish(); return; }
        android.app.AlertDialog prompt = new android.app.AlertDialog.Builder(this, LiquidTheme.alertDialogTheme(this))
                .setTitle("Ekran koruyucu değişiklikleri kaydedilsin mi?")
                .setMessage("Albümünüzde ve alt yazınızda kaydedilmemiş değişiklikler var.")
                .setPositiveButton("Kaydet", (d, w) -> { if (save()) finish(); })
                .setNegativeButton("At", (d, w) -> finish()).setNeutralButton("Düzenlemeye devam et", null).create();
        Motion.dialog(prompt); prompt.show();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("album", album.getText().toString());
        state.putString("caption", caption.getText().toString());
        state.putInt("scroll", ((ScrollView)page.getParent()).getScrollY());
        super.onSaveInstanceState(state);
    }

    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() { requestClose(); }

    private void openScreensaverSettings() {
        Intent[] candidates = new Intent[] {
            new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName("com.android.tv.settings", "com.android.tv.settings.device.display.daydream.DaydreamActivity")),
            new Intent(Settings.ACTION_DREAM_SETTINGS),
            new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName("com.amazon.tv.settings", "com.amazon.tv.settings.SettingsActivity")),
            new Intent("com.google.android.tv.settings.ambient"),
            new Intent(Intent.ACTION_MAIN).setComponent(new ComponentName("com.android.tv.settings", "com.android.tv.settings.MainSettings")),
            new Intent(Settings.ACTION_SETTINGS)
        };
        for (Intent intent : candidates) {
            try {
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                    return;
                }
            } catch (Exception ignored) { }
        }
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (Exception unavailable) {
            feedback.setText("Bu TV sistem ekran koruyucu ayarlarını kısıtlıyor. Yukarıdaki “Sva boştayken başlat” seçeneğini kullanın veya TV’nin Ekran koruyucu / Ortam modu ayarlarını açın.");
        }
    }
    private boolean save() {
        String value = album.getText().toString().trim();
        if (!ScreensaverPreferences.validAlbum(value)) { feedback.setText("Paylaşılan bir Google Fotoğraflar albümü bağlantısı girin veya Ana Sayfa’yı kullanmak için boş bırakın."); return false; }
        prefs.edit().putString(ScreensaverPreferences.ALBUM, value)
                .putString(ScreensaverPreferences.CAPTION, caption.getText().toString().trim()).apply();
        updateSource(); feedback.setText("Kaydedildi."); return true;
    }
    private void checkAlbum() {
        if (!save()) return;
        String selected = ScreensaverPreferences.album(prefs);
        if (selected.isEmpty()) { feedback.setText("Ana Sayfa’da bir albüm ekleyin veya yukarıya ayrı bir albüm yapıştırın."); return; }
        feedback.setText("Paylaşılan albüm kontrol ediliyor…");
        lifecycleWork.execute(worker, () -> {
            String result;
            try { result = "Albüm hazır · " + new ScreensaverPhotoRepository(this).refresh(selected, true).size() + " fotoğraf"; }
            catch (Exception e) { result = "Albüm okunamadı. Bağlantıyı, paylaşım ayarlarını ve internet bağlantısını kontrol edin."; }
            String message = result;
            lifecycleWork.post(() -> feedback.setText(message));
        });
    }
    private void updateSource() {
        boolean home = prefs.getString(ScreensaverPreferences.ALBUM, "").isEmpty();
        source.setText("Fotoğraf kaynağı · " + (home ? "Ana Sayfa albümü" : "Ayrı albüm")
                + (ScreensaverPreferences.album(prefs).isEmpty() ? " · henüz bağlantı ayarlanmadı" : ""));
    }
    private int dp(int n) { return LiquidTheme.dp(this, n); }
    private EditText input(String label, String value) {
        SettingsUi.hint(page, label);
        EditText edit = new EditText(this); edit.setText(value); edit.setSingleLine(true);
        edit.setSelectAllOnFocus(true);
        edit.setInputType(android.text.InputType.TYPE_CLASS_TEXT | (label.startsWith("Ayrı")
                ? android.text.InputType.TYPE_TEXT_VARIATION_URI : android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));
        edit.setHint(label.startsWith("Ayrı") ? "https://photos.app.goo.gl/…" : "Kişisel bir alt yazı ekleyin");
        SettingsUi.styleInput(edit, false); SettingsUi.add(page, edit); return edit;
    }
    @Override protected void onDestroy() { lifecycleWork.close(); worker.shutdownNow(); super.onDestroy(); }
}
