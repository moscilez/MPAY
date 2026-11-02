package com.mpay.wallet;

import android.test.ActivityInstrumentationTestCase2;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/** Device-level launch check; never enters a PIN or bypasses system authentication. */
@SuppressWarnings("deprecation")
public class LaunchTest extends ActivityInstrumentationTestCase2<MainActivity> {
    public LaunchTest() { super(MainActivity.class); }

    public void testLaunchKeepsWalletLocked() throws Throwable {
        MainActivity activity = getActivity();
        getInstrumentation().waitForIdleSync();
        runTestOnUiThread(() -> {
            View decor = activity.getWindow().getDecorView();
            assertTrue("Unlock action must be available", containsText(decor, "Открыть кошелёк"));
            assertTrue("Device protection setup must be discoverable", containsText(decor, "Настроить защиту телефона"));
            assertFalse("Private card screen must not open before authentication", containsText(decor, "Ваши карты"));
        });
    }

    private boolean containsText(View view, String expected) {
        if (view instanceof TextView && expected.contentEquals(((TextView) view).getText())) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (containsText(group.getChildAt(i), expected)) return true;
        }
        return false;
    }
}
