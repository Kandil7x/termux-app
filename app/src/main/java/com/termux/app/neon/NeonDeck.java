package com.termux.app.neon;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.app.terminal.TermuxTerminalSessionActivityClient;
import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.terminal.TerminalSession;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Neon Deck: a collapsible row of quick-action buttons above the extra keys.
 *
 * The terminal itself is not touched. Every button only writes text to the current terminal session,
 * exactly like typing it. Buttons come from ~/.termux/neon/deck.json, or from assets/neon/deck.json
 * when that file does not exist.
 */
public final class NeonDeck {

    private static final String LOG_TAG = "NeonDeck";
    private static final String PREFS_NAME = "neon_deck";
    private static final String PREF_VISIBLE = "visible";
    private static final String ASSET_DECK = "neon/deck.json";
    private static final long NEW_SESSION_DELAY_MS = 700;

    /** User configuration file: ~/.termux/neon/deck.json */
    public static final File CONFIG_FILE = new File(TermuxConstants.TERMUX_HOME_DIR_PATH, ".termux/neon/deck.json");

    private final TermuxActivity mActivity;
    private View mDeck;
    private LinearLayout mChips;

    public NeonDeck(TermuxActivity activity) {
        mActivity = activity;
    }

    /** Find the views, build the buttons and restore the last visibility. Call after setContentView. */
    public void attach() {
        mDeck = mActivity.findViewById(R.id.neon_deck);
        mChips = mActivity.findViewById(R.id.neon_deck_chips);
        if (mDeck == null || mChips == null) {
            Logger.logError(LOG_TAG, "Deck views not found in layout");
            return;
        }

        View drawerButton = mActivity.findViewById(R.id.neon_deck_button);
        if (drawerButton != null) {
            drawerButton.setOnClickListener(v -> {
                toggle();
                mActivity.getDrawer().closeDrawers();
            });
        }

        reload();
        setVisible(readVisible());
    }

    public boolean isVisible() {
        return mDeck != null && mDeck.getVisibility() == View.VISIBLE;
    }

    public void toggle() {
        setVisible(!isVisible());
    }

    public void setVisible(boolean visible) {
        if (mDeck == null) return;
        mDeck.setVisibility(visible ? View.VISIBLE : View.GONE);
        try {
            mActivity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(PREF_VISIBLE, visible).apply();
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to save deck visibility", e);
        }
    }

    private boolean readVisible() {
        try {
            SharedPreferences prefs = mActivity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return prefs.getBoolean(PREF_VISIBLE, false);
        } catch (Exception e) {
            return false;
        }
    }

    /** Re-read the configuration and rebuild all buttons. */
    public void reload() {
        if (mChips == null) return;
        mChips.removeAllViews();

        for (final NeonAction action : loadActions()) {
            mChips.addView(chip(action.label, v -> run(action)));
        }
        mChips.addView(chip(mActivity.getString(R.string.neon_deck_install), v -> NeonCatalog.show(mActivity, this)));
        mChips.addView(chip(mActivity.getString(R.string.neon_deck_edit), v -> editConfig()));
        mChips.addView(chip(mActivity.getString(R.string.neon_deck_reload), v -> reload()));
    }

    /** Run an action in the current session, or in a new one when the action asks for it. */
    public void run(final NeonAction action) {
        if (action.newSession) {
            TermuxTerminalSessionActivityClient client = mActivity.getTermuxTerminalSessionClient();
            if (client == null) return;
            client.addNewSession(false, action.label);
            mActivity.getTerminalView().postDelayed(() -> sendToCurrentSession(action.payload()), NEW_SESSION_DELAY_MS);
        } else {
            sendToCurrentSession(action.payload());
        }
    }

    /** Type a command into the current session and press Enter. */
    public void runCommand(String command) {
        run(new NeonAction(command, command, "", false));
    }

    private void sendToCurrentSession(String text) {
        TerminalSession session = mActivity.getCurrentSession();
        if (session == null || !session.isRunning()) {
            mActivity.showToast(mActivity.getString(R.string.neon_deck_no_session), false);
            return;
        }
        session.write(text);
    }

    /** Create the user file from the defaults if needed, then open it in nano. */
    private void editConfig() {
        try {
            if (!CONFIG_FILE.exists()) {
                NeonIo.writeFile(CONFIG_FILE, NeonIo.readAsset(mActivity, ASSET_DECK));
            }
        } catch (IOException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to create deck.json", e);
            return;
        }
        runCommand("nano ~/.termux/neon/deck.json");
    }

    private List<NeonAction> loadActions() {
        try {
            String json = CONFIG_FILE.isFile() ? NeonIo.readFile(CONFIG_FILE) : NeonIo.readAsset(mActivity, ASSET_DECK);
            return parse(json);
        } catch (IOException | JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Invalid deck.json, falling back to defaults", e);
            mActivity.showToast(mActivity.getString(R.string.neon_deck_config_error), true);
            try {
                return parse(NeonIo.readAsset(mActivity, ASSET_DECK));
            } catch (IOException | JSONException e2) {
                return new ArrayList<>();
            }
        }
    }

    /** Parse {"actions":[{"label":"..","command":".."}, ...]}. Entries without a label or payload are skipped. */
    static List<NeonAction> parse(String json) throws JSONException {
        JSONArray array = new JSONObject(json).getJSONArray("actions");
        List<NeonAction> actions = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            String label = o.optString("label", "").trim();
            String command = o.optString("command", "");
            String send = o.optString("send", "");
            if (label.isEmpty() || (command.isEmpty() && send.isEmpty())) continue;
            actions.add(new NeonAction(label, command, send, o.optBoolean("newSession", false)));
        }
        return actions;
    }

    private TextView chip(String text, View.OnClickListener listener) {
        TextView view = new TextView(mActivity);
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(mActivity, R.color.neon_cyan));
        view.setTypeface(Typeface.MONOSPACE);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        view.setSingleLine(true);
        view.setPadding(dp(12), dp(8), dp(12), dp(8));
        view.setBackgroundResource(R.drawable.neon_chip_bg);
        view.setClickable(true);
        view.setOnClickListener(listener);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMarginEnd(dp(6));
        view.setLayoutParams(params);
        return view;
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
            mActivity.getResources().getDisplayMetrics()));
    }
}
