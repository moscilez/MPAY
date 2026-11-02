package com.mpay.wallet;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.hardware.biometrics.BiometricManager;
import android.hardware.biometrics.BiometricPrompt;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import androidx.core.content.FileProvider;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import org.json.*;
import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.YearMonth;
import java.util.*;

public class MainActivity extends Activity {
    private static final int BG = 0xff101715, PANEL = 0xff1e2924, MINT = 0xffb7f7cc, TEXT = 0xffeff6f0, MUTED = 0xffa2b3a7;
    private LinearLayout root, content;
    private Vault vault;
    private JSONObject wallet;
    private String selectedId = "";
    private boolean unlocked, stopped = true, authenticating, scanReady, scanning;
    private int tab;
    private CancellationSignal cancellation;
    private AlertDialog activeDialog;
    private File scanFile;
    private TextRecognizer recognizer;
    private Runnable authenticatedAction;
    private long authenticatedAt;
    private int authenticationAttempt;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        vault = new Vault(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setPadding(dp(22), dp(14), dp(22), dp(10));
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
            root.setPadding(dp(22) + bars.left, dp(14) + bars.top, dp(22) + bars.right, dp(10) + bars.bottom);
            return insets;
        });
        setContentView(root);
        cleanScanFiles();
    }
    @Override protected void onResume() {
        super.onResume(); stopped = false;
        if (authenticating) return;
        root.setVisibility(View.VISIBLE);
        if (authenticatedAction != null) {
            Runnable action = authenticatedAction; authenticatedAction = null;
            if (SystemClock.elapsedRealtime() - authenticatedAt < 30_000) { action.run(); return; }
            unlocked = false; wallet = null;
        }
        if (!unlocked) { if (activeDialog != null) activeDialog.dismiss(); lockedScreen(); authenticate(this::load); }
        else processScan();
    }
    @Override protected void onStop() {
        stopped = true;
        // Device-credential confirmation may temporarily stop this Activity.
        // Keep its callback alive, but never expose the wallet while it is pending.
        if (authenticating) { root.setVisibility(View.INVISIBLE); super.onStop(); return; }
        unlocked = false; wallet = null; authenticatedAction = null;
        if (activeDialog != null) activeDialog.dismiss();
        lockedScreen(); super.onStop();
    }
    @Override protected void onDestroy() {
        authenticationAttempt++;
        if (cancellation != null) cancellation.cancel();
        authenticatedAction = null;
        if (recognizer != null) recognizer.close();
        if (scanFile != null && isFinishing()) scanFile.delete();
        super.onDestroy();
    }
    private void authenticate(Runnable success) {
        if (authenticating || stopped) return;
        int flags = BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        if (getSystemService(BiometricManager.class).canAuthenticate(flags) != BiometricManager.BIOMETRIC_SUCCESS) {
            message("Защита телефона", "Включите PIN-код, пароль или графический ключ в настройках блокировки экрана. После этого откройте MPay."); return;
        }
        authenticating = true; cancellation = new CancellationSignal();
        final int attempt = ++authenticationAttempt;
        new BiometricPrompt.Builder(this).setTitle("Открыть MPay")
                .setSubtitle("Биометрия или код блокировки телефона")
                .setAllowedAuthenticators(flags).build().authenticate(cancellation, getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                    @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        if (attempt != authenticationAttempt || isDestroyed()) return;
                        authenticating = false;
                        if (stopped) { authenticatedAction = success; authenticatedAt = SystemClock.elapsedRealtime(); }
                        else { root.setVisibility(View.VISIBLE); success.run(); }
                    }
                    @Override public void onAuthenticationError(int code, CharSequence error) {
                        if (attempt != authenticationAttempt || isDestroyed()) return;
                        authenticating = false;
                        if (stopped) { unlocked = false; wallet = null; if (activeDialog != null) activeDialog.dismiss(); }
                        else root.setVisibility(View.VISIBLE);
                        if (!stopped && code != BiometricPrompt.BIOMETRIC_ERROR_CANCELED) toast(error.toString());
                    }
                });
    }
    private void load() {
        try { wallet = vault.read(); unlocked = true; render(); processScan(); }
        catch (Exception e) { unlocked = false; lockedScreen(); message("Не удалось открыть хранилище", "Данные не изменены. Проверьте блокировку телефона и повторите вход. При утрате ключа Android Keystore данные восстановить нельзя."); }
    }
    private void lockedScreen() {
        root.removeAllViews();
        root.addView(label("MPay", 36, TEXT));
        root.addView(label("Личный кошелёк на вашем телефоне", 16, MUTED));
        root.addView(label("Данные защищены блокировкой устройства", 18, TEXT));
        root.addView(button("Открыть кошелёк", () -> authenticate(this::load)));
        root.addView(button("Настроить защиту телефона", () -> {
            Intent settings = new Intent(android.provider.Settings.ACTION_BIOMETRIC_ENROLL)
                    .putExtra(android.provider.Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                            BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
            try { startActivity(settings); } catch (ActivityNotFoundException e) { startActivity(new Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)); }
        }));
    }
    private JSONArray cards() { return wallet.optJSONArray("cards"); }
    private JSONArray entries() { return wallet.optJSONArray("entries"); }
    private JSONObject selected() {
        if (wallet == null) return null;
        for (int i = 0; i < cards().length(); i++) if (cards().optJSONObject(i).optString("id").equals(selectedId)) return cards().optJSONObject(i);
        if (cards().length() == 0) return null;
        JSONObject c = cards().optJSONObject(0); selectedId = c.optString("id"); return c;
    }
    private void render() {
        if (!unlocked || wallet == null || stopped) return;
        root.removeAllViews();
        LinearLayout header = row();
        TextView title = label("MPay", 30, TEXT); header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(button("Блокировать", () -> { unlocked = false; wallet = null; lockedScreen(); })); root.addView(header);
        root.addView(label("ЛОКАЛЬНЫЙ КОШЕЛЁК  ·  БЕЗ АККАУНТА", 11, MINT));
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true);
        content = column(); content.setPadding(0, dp(18), 0, dp(20));
        FrameLayout frame = new FrameLayout(this);
        FrameLayout.LayoutParams centered = new FrameLayout.LayoutParams(Math.min(getResources().getDisplayMetrics().widthPixels - dp(44), dp(620)), -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        frame.addView(content, centered); scroll.addView(frame); root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        if (tab == 0) showCards(); else if (tab == 1) showHistory(); else showAbout();
        LinearLayout nav = row();
        String[] names = {"Карты", "История", "О приложении"};
        for (int i = 0; i < names.length; i++) { final int index = i; Button b = button(names[i], () -> { tab = index; render(); }); b.setTextColor(tab == i ? MINT : MUTED); nav.addView(b, new LinearLayout.LayoutParams(0, -2, 1)); }
        root.addView(nav);
        TextView swipe = new SwipeHandle(this); swipe.setText("━━   Потяните вверх · Быстрый доступ"); swipe.setTextSize(13); swipe.setTextColor(MINT); swipe.setGravity(Gravity.CENTER); swipe.setPadding(0, dp(14), 0, dp(14));
        swipe.setContentDescription("Быстрый доступ к выбранной карте. Нажмите или смахните вверх.");
        swipe.setOnClickListener(v -> quickCard());
        root.addView(swipe);
    }
    private void showCards() {
        content.addView(label("Ваши карты", 25, TEXT));
        content.addView(label("Несколько карт. Одно защищённое место.", 14, MUTED));
        JSONObject c = selected();
        if (c == null) {
            content.addView(label("Пока нет карт", 24, TEXT));
            content.addView(label("Добавьте карту вручную или распознайте её камерой. Здесь сохраняются только последние четыре цифры.", 16, MUTED));
        } else {
            content.addView(cardView(c));
            content.addView(button("Быстрый доступ", this::quickCard));
            content.addView(button("Редактировать карту", () -> editCard(c, null)));
            content.addView(button("История этой карты", () -> { tab = 1; render(); }));
            content.addView(label("Все карты · " + cards().length(), 16, MUTED));
            for (int i = 0; i < cards().length(); i++) {
                JSONObject item = cards().optJSONObject(i);
                content.addView(button((item.optString("id").equals(selectedId) ? "✓  " : "") + item.optString("name") + " · " + item.optString("network") + " •• " + item.optString("last4"), () -> { selectedId = item.optString("id"); render(); }));
            }
        }
        content.addView(button("+ Добавить карту", () -> editCard(null, null)));
        content.addView(button("Распознать камерой", this::capture));
        content.addView(label("Оплата NFC не подключена. Добавление карты в каталог не привязывает её к платёжной системе.", 13, MUTED));
    }
    private View cardView(JSONObject c) {
        LinearLayout card = column(); card.setPadding(dp(24), dp(22), dp(24), dp(22));
        card.setBackground(background(0xffb7f7cc, 26));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(20), 0, dp(12)); card.setLayoutParams(lp);
        card.addView(label(c.optString("name"), 23, BG));
        card.addView(label(c.optString("network") + "  ·  ЛОКАЛЬНАЯ ЗАПИСЬ", 12, 0xff3a5844));
        TextView number = label("••••  ••••  ••••  " + c.optString("last4"), 25, BG); number.setPadding(0, dp(28), 0, dp(22)); card.addView(number);
        card.addView(label(c.optString("holder").isEmpty() ? "Держатель не указан" : c.optString("holder"), 15, BG));
        card.addView(label(c.optString("expiry").isEmpty() ? "Срок не указан" : "До " + c.optString("expiry"), 14, 0xff3a5844)); return card;
    }
    private void quickCard() {
        JSONObject c = selected(); if (c == null) { toast("Сначала добавьте карту"); return; }
        LinearLayout box = column(); box.setPadding(dp(20), 0, dp(20), dp(16)); box.addView(cardView(c));
        box.addView(label("Оплата пока недоступна", 22, TEXT));
        box.addView(label("Для оплаты этой картой необходимо подключение банка и платёжной системы. Приложение не передаёт реквизиты терминалу и не списывает деньги.", 16, MUTED));
        showDialog(new AlertDialog.Builder(this).setTitle("Быстрый доступ").setView(box).setPositiveButton("Закрыть", null).create());
    }
    private void editCard(JSONObject existing, String[] scanned) {
        LinearLayout form = column(); form.setPadding(dp(20), 0, dp(20), 0);
        EditText name = field(form, "Название карты", existing == null ? "" : existing.optString("name"), InputType.TYPE_CLASS_TEXT);
        EditText number = field(form, "Номер или последние 4 цифры", scanned != null ? scanned[0] : existing == null ? "" : existing.optString("last4"), InputType.TYPE_CLASS_NUMBER);
        number.setAutofillHints(View.AUTOFILL_HINT_CREDIT_CARD_NUMBER);
        EditText expiry = field(form, "Срок действия ММ/ГГ (необязательно)", scanned != null ? scanned[1] : existing == null ? "" : existing.optString("expiry"), InputType.TYPE_CLASS_DATETIME);
        EditText holder = field(form, "Держатель (необязательно)", scanned != null ? scanned[2] : existing == null ? "" : existing.optString("holder"), InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        Spinner network = new Spinner(this); network.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, CardRules.NETWORKS));
        String hint = scanned != null && !scanned[0].isEmpty() ? CardRules.network(scanned[0]) : existing == null ? "Другая" : existing.optString("network");
        network.setSelection(Arrays.asList(CardRules.NETWORKS).indexOf(hint));
        form.addView(label("Платёжная система · можно изменить", 13, MUTED)); form.addView(network);
        form.addView(label("Полный номер не сохраняется. CVV и PIN карты не нужны. Проверьте распознанные данные перед сохранением.", 13, MUTED));
        number.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) { if (CardRules.digits(s.toString()).length() >= 6) network.setSelection(Arrays.asList(CardRules.NETWORKS).indexOf(CardRules.network(s.toString()))); }
            public void afterTextChanged(android.text.Editable e) {}
        });
        ScrollView scroll = new ScrollView(this); scroll.addView(form);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(existing == null ? "Добавить карту" : "Данные карты").setView(scroll).setNegativeButton("Отмена", null).setPositiveButton("Сохранить", null).create();
        if (existing != null) dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Удалить", (d, which) -> confirmDelete(existing));
        showDialog(dialog);
        dialog.setOnDismissListener(d -> { number.setText(""); holder.setText(""); });
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String n = CardRules.digits(number.getText().toString()), exp = expiry.getText().toString().trim();
            if (name.getText().toString().trim().isEmpty()) { name.setError("Введите название"); return; }
            if (name.length() > 60 || holder.length() > 80) { toast("Название до 60 символов, держатель до 80"); return; }
            if (n.length() != 4 && !CardRules.luhn(n)) { number.setError("Нужны 4 цифры или корректный номер из 12–19 цифр"); return; }
            if (!exp.isEmpty() && !CardRules.validExpiry(exp, YearMonth.now())) { expiry.setError("Укажите действующий срок в формате ММ/ГГ"); return; }
            try {
                JSONObject card = new JSONObject().put("id", existing == null ? UUID.randomUUID().toString() : existing.optString("id"))
                        .put("name", name.getText().toString().trim()).put("last4", n.substring(n.length() - 4))
                        .put("expiry", exp).put("holder", holder.getText().toString().trim()).put("network", network.getSelectedItem().toString());
                JSONObject copy = new JSONObject(wallet.toString()); JSONArray list = copy.getJSONArray("cards");
                boolean replaced = false;
                for (int i = 0; i < list.length(); i++) if (list.getJSONObject(i).getString("id").equals(card.getString("id"))) { list.put(i, card); replaced = true; break; }
                if (!replaced) list.put(card);
                persist(copy, () -> { selectedId = card.optString("id"); dialog.dismiss(); render(); });
            } catch (JSONException e) { toast("Не удалось сохранить карту"); }
        });
    }
    private void persist(JSONObject copy, Runnable done) {
        authenticate(() -> {
            try { vault.write(copy); wallet = copy; done.run(); }
            catch (Exception e) { message("Сохранение не выполнено", "Изменения не записаны. Повторите попытку после разблокировки телефона."); }
        });
    }
    private void confirmDelete(JSONObject c) {
        showDialog(new AlertDialog.Builder(this).setTitle("Удалить карту?").setMessage("Будут удалены карта и все её ручные записи. Это действие нельзя отменить.")
                .setNegativeButton("Отмена", null).setPositiveButton("Удалить", (d, w) -> {
                    try {
                        JSONObject copy = new JSONObject(wallet.toString()); JSONArray cs = copy.getJSONArray("cards"), es = copy.getJSONArray("entries");
                        for (int i = cs.length() - 1; i >= 0; i--) if (cs.getJSONObject(i).getString("id").equals(c.optString("id"))) cs.remove(i);
                        for (int i = es.length() - 1; i >= 0; i--) if (es.getJSONObject(i).getString("cardId").equals(c.optString("id"))) es.remove(i);
                        persist(copy, this::render);
                    } catch (JSONException e) { toast("Не удалось удалить карту"); }
                }).create());
    }
    private void showHistory() {
        content.addView(label("История", 25, TEXT)); JSONObject c = selected();
        if (c == null) { content.addView(label("Добавьте карту, чтобы вести её историю.", 16, MUTED)); return; }
        Spinner picker = new Spinner(this); List<String> titles = new ArrayList<>(); int current = 0;
        for (int i = 0; i < cards().length(); i++) { JSONObject item = cards().optJSONObject(i); titles.add(item.optString("name") + " •• " + item.optString("last4")); if (item.optString("id").equals(selectedId)) current = i; }
        picker.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, titles)); picker.setSelection(current); content.addView(picker);
        picker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int position, long id) { String next = cards().optJSONObject(position).optString("id"); if (!next.equals(selectedId)) { selectedId = next; render(); } }
        });
        content.addView(label("Ручной журнал. Банковские операции автоматически не загружаются; записи не подтверждают оплату.", 14, MUTED));
        content.addView(button("+ Добавить запись о расходе", () -> addEntry(c)));
        int count = 0;
        SimpleDateFormat fmt = new SimpleDateFormat("dd MMM yyyy · HH:mm", new Locale("ru"));
        for (int i = entries().length() - 1; i >= 0; i--) {
            JSONObject e = entries().optJSONObject(i); if (!e.optString("cardId").equals(c.optString("id"))) continue; count++;
            LinearLayout box = column(); box.setPadding(dp(16), dp(14), dp(16), dp(14)); box.setBackground(background(PANEL, 16));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.setMargins(0, dp(10), 0, 0); box.setLayoutParams(lp);
            box.addView(label(e.optString("merchant"), 18, TEXT)); box.addView(label("−" + e.optString("amount") + " " + e.optString("currency"), 22, MINT));
            box.addView(label(fmt.format(new Date(e.optLong("time"))) + " · Вручную", 12, MUTED));
            box.addView(button("Удалить запись", () -> deleteEntry(e.optString("id")))); content.addView(box);
        }
        if (count == 0) content.addView(label("У этой карты пока нет записей", 18, TEXT));
    }
    private void addEntry(JSONObject c) {
        LinearLayout form = column(); form.setPadding(dp(20), 0, dp(20), 0);
        EditText merchant = field(form, "Место покупки", "", InputType.TYPE_CLASS_TEXT);
        EditText amount = field(form, "Сумма расхода", "", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText currency = field(form, "Код валюты: RUB, USD, EUR…", "RUB", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        form.addView(label("Запись в журнале не списывает деньги. Дата — текущее время телефона.", 13, MUTED));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Ручная запись · •• " + c.optString("last4")).setView(form).setNegativeButton("Отмена", null).setPositiveButton("Добавить", null).create(); showDialog(dialog);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                String place = merchant.getText().toString().trim(), curr = currency.getText().toString().trim().toUpperCase(Locale.ROOT);
                if (place.isEmpty() || place.length() > 100) { merchant.setError("От 1 до 100 символов"); return; }
                String raw = amount.getText().toString().trim().replace(',', '.');
                if (!raw.matches("\\d{1,12}(\\.\\d{1,3})?")) { amount.setError("Укажите сумму, до 3 знаков после запятой"); return; }
                BigDecimal value = new BigDecimal(raw);
                if (value.signum() <= 0) { amount.setError("Сумма должна быть больше нуля"); return; }
                try { Currency.getInstance(curr); } catch (IllegalArgumentException e) { currency.setError("Укажите код валюты ISO 4217"); return; }
                JSONObject copy = new JSONObject(wallet.toString());
                copy.getJSONArray("entries").put(new JSONObject().put("id", UUID.randomUUID().toString()).put("cardId", c.optString("id"))
                        .put("merchant", place).put("amount", value.toPlainString()).put("currency", curr).put("time", System.currentTimeMillis()).put("source", "manual"));
                persist(copy, () -> { dialog.dismiss(); render(); });
            } catch (JSONException | NumberFormatException e) { toast("Проверьте данные записи"); }
        });
    }
    private void deleteEntry(String id) {
        showDialog(new AlertDialog.Builder(this).setTitle("Удалить ручную запись?").setMessage("Отменить удаление нельзя.").setNegativeButton("Отмена", null).setPositiveButton("Удалить", (d, w) -> {
            try {
                JSONObject copy = new JSONObject(wallet.toString()); JSONArray es = copy.getJSONArray("entries");
                for (int i = es.length() - 1; i >= 0; i--) if (es.getJSONObject(i).optString("id").equals(id)) es.remove(i);
                persist(copy, this::render);
            } catch (JSONException e) { toast("Не удалось удалить запись"); }
        }).create());
    }
    private void showAbout() {
        content.addView(label("Только на вашем телефоне", 26, TEXT));
        content.addView(label("MPay Local · " + BuildConfig.VERSION_NAME, 14, MINT));
        content.addView(label("Без регистрации и собственного сервера. Данные зашифрованы AES-GCM с ключом Android Keystore. Доступ подтверждается системной биометрией или кодом блокировки телефона.", 16, TEXT));
        content.addView(label("Сохраняются название карты, платёжная система, последние четыре цифры, держатель, срок и ручной журнал расходов. Полный номер, CVV, PIN карты и фотографии не сохраняются в кошельке.", 16, MUTED));
        content.addView(label("Распознавание выполняется на телефоне. Поддерживаются цифры и латиница; результат нужно проверить. Снимок временно находится в закрытом кэше приложения и удаляется после обработки. Приложение камеры управляет собственной обработкой снимков.", 16, MUTED));
        content.addView(label("Оплата не подключена", 22, TEXT));
        content.addView(label("Каталог допускает Мир, Visa, Mastercard, UnionPay и другие карты. Это не означает поддержку оплаты. Для реальных покупок нужны платёжные токены, интеграция с банками и платёжными системами. Охват зависит от страны и банка.", 16, MUTED));
        content.addView(label("Свайп работает внутри MPay. Вызов с домашнего или заблокированного экрана не реализован. История — ручной журнал, а не банковская выписка.", 16, MUTED));
        content.addView(label("Резервное копирование отключено. Удаление приложения, сброс устройства или потеря ключа могут привести к безвозвратной потере локальных данных.", 15, MUTED));
    }
    private void capture() {
        if (scanning) { toast("Дождитесь завершения распознавания"); return; }
        try {
            File folder = new File(getCacheDir(), "scan"); if (!folder.exists() && !folder.mkdirs()) throw new java.io.IOException();
            cleanScanFiles(); scanFile = File.createTempFile("card-", ".jpg", folder);
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", scanFile);
            Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, uri)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            camera.setClipData(ClipData.newRawUri("scan", uri));
            startActivityForResult(camera, 100);
        } catch (Exception e) { cleanScanFiles(); scanFile = null; message("Камера недоступна", "Не удалось открыть приложение камеры. Можно добавить карту вручную."); }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == 100) {
            scanReady = result == RESULT_OK && scanFile != null && scanFile.length() > 0;
            if (!scanReady) { if (scanFile != null) scanFile.delete(); scanFile = null; }
        }
    }
    private void processScan() {
        if (!unlocked || !scanReady || scanning || stopped || scanFile == null) return;
        scanReady = false; scanning = true; toast("Распознаём карту на телефоне…");
        try {
            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            InputImage image = InputImage.fromFilePath(this, Uri.fromFile(scanFile));
            recognizer.process(image).addOnSuccessListener(text -> {
                if (!stopped && unlocked) {
                    String[] fields = CardRules.fromOcr(text.getText()); editCard(null, fields);
                    if (fields[0].isEmpty()) toast("Номер не распознан. Введите данные вручную.");
                }
            }).addOnFailureListener(e -> { if (!stopped) message("Не удалось распознать", "Попробуйте снять карту при хорошем освещении или введите данные вручную."); })
                    .addOnCompleteListener(task -> finishScan());
        } catch (Exception e) { finishScan(); toast("Не удалось прочитать снимок"); }
    }
    private void finishScan() { scanning = false; if (recognizer != null) { recognizer.close(); recognizer = null; } if (scanFile != null) { scanFile.delete(); scanFile = null; } }
    private void cleanScanFiles() { File[] files = new File(getCacheDir(), "scan").listFiles(); if (files != null) for (File f : files) if (f.isFile()) f.delete(); }
    private EditText field(LinearLayout parent, String hint, String value, int type) {
        parent.addView(label(hint, 13, MUTED)); EditText e = new EditText(this); e.setTextColor(TEXT); e.setTextSize(17); e.setSingleLine(true); e.setInputType(type | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS); e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO); e.setSaveEnabled(false); e.setText(value); parent.addView(e); return e;
    }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private LinearLayout column() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.VERTICAL); return v; }
    private LinearLayout row() { LinearLayout v = new LinearLayout(this); v.setOrientation(LinearLayout.HORIZONTAL); v.setGravity(Gravity.CENTER_VERTICAL); return v; }
    private TextView label(String text, int size, int color) { TextView v = new TextView(this); v.setText(text); v.setTextSize(size); v.setTextColor(color); v.setPadding(0, dp(7), 0, dp(7)); return v; }
    private Button button(String text, Runnable action) { Button b = new Button(this); b.setText(text); b.setTextSize(14); b.setAllCaps(false); b.setTextColor(MINT); b.setMinHeight(dp(48)); b.setOnClickListener(v -> { if (!stopped) action.run(); }); return b; }
    private GradientDrawable background(int color, int radius) { GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private void showDialog(AlertDialog dialog) { if (activeDialog != null && activeDialog.isShowing()) activeDialog.dismiss(); activeDialog = dialog; dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE); dialog.show(); }
    private void message(String title, String text) { if (!stopped) showDialog(new AlertDialog.Builder(this).setTitle(title).setMessage(text).setPositiveButton("Понятно", null).create()); }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
}
