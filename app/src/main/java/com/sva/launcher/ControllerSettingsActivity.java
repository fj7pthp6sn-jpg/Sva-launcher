package com.sva.launcher;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public final class ControllerSettingsActivity extends MotionActivity {
    private final List<InputDevice> controllers = new ArrayList<>();
    private InputDevice mappingDevice; private ControllerProfile mapping; private int step; private TextView prompt;
    private String focusedController;

    @Override public void onCreate(Bundle state) { super.onCreate(state); getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_FULLSCREEN | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY); BackNavigation.install(this, this::handleBack); renderList(); }

    private void renderList() {
        controllers.clear(); for (int id : InputDevice.getDeviceIds()) {
            InputDevice device = InputDevice.getDevice(id); if (device == null) continue;
            if ((device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                    || (device.getSources() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK) controllers.add(device);
        }
        LinearLayout shell = SettingsUi.page(this, "Oyun kolları", "Bağlı oyun kollarınızın tuşlarını ayarlayın", this::handleBack);
        LinearLayout page = SettingsUi.scroll(shell);
        SettingsUi.heading(page, "Bağlı kontrolcüler");
        SettingsUi.hint(page, "Sol analog çubuk ve yön tuşları otomatik çalışır. Tuşlarını yeniden atamak için bir kontrolcü seçin.");
        Button preferred = null;
        if (controllers.isEmpty()) {
            SettingsUi.heading(page, "Bağlı kontrolcü yok");
            SettingsUi.hint(page, "Bluetooth veya USB ile bir oyun kolu bağlayın, sonra Yenile’yi seçin.");
        }
        for (InputDevice device : controllers) {
            Button button = SettingsUi.action(this, device.getName(), () -> {
                focusedController = device.getDescriptor(); begin(device);
            });
            SettingsUi.add(page, button);
            if (preferred == null || device.getDescriptor().equals(focusedController)) preferred = button;
        }
        Button refresh = SettingsUi.action(this, "Kontrolcüleri yenile", this::renderList);
        SettingsUi.add(page, refresh);
        setContentView(shell); (preferred == null ? refresh : preferred).requestFocus();
    }

    private void begin(InputDevice device) {
        mappingDevice = device; mapping = new ControllerProfile(); step = 0;
        LinearLayout shell = SettingsUi.page(this, "Kontrolcüyü eşle", device.getName(), this::handleBack);
        LinearLayout page = SettingsUi.scroll(shell);
        SettingsUi.heading(page, "Tuş eşleme");
        prompt = SettingsUi.text(this, "", 28, false);
        prompt.setPadding(0, SettingsUi.dp(this, 32), 0, SettingsUi.dp(this, 32));
        page.addView(prompt);
        SettingsUi.hint(page, "Seçili kontrolcüde bir tuşa basın. Kaydetmeden iptal etmek için Geri’ye basın.");
        setContentView(shell); updatePrompt();
    }

    private void updatePrompt() {
        prompt.setText("Tuş " + (step + 1) + " / " + mapping.keys.length + "\n\n" + ControllerProfile.NAMES[step]);
    }

    private void handleBack() {
        if (mappingDevice != null) { mappingDevice = null; renderList(); } else finish();
    }

    // Physical Back must cancel mapping before it can be recorded as a controller input.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (mappingDevice != null && event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            if (event.getAction() == KeyEvent.ACTION_UP) handleBack();
            return true;
        }
        if (mappingDevice == null || event.getAction() != KeyEvent.ACTION_DOWN || event.getRepeatCount() > 0)
            return super.dispatchKeyEvent(event);
        if (event.getDeviceId() != mappingDevice.getId()) return true;
        mapping.keys[step++] = event.getKeyCode();
        if (step < mapping.keys.length) updatePrompt();
        else {
            try { mapping.save(this, mappingDevice); Toast.makeText(this, "Kontrolcü eşlemesi kaydedildi.", Toast.LENGTH_SHORT).show(); }
            catch (Exception e) { Toast.makeText(this, "Eşleme kaydedilemedi.", Toast.LENGTH_LONG).show(); }
            mappingDevice = null; renderList();
        }
        return true;
    }
}
