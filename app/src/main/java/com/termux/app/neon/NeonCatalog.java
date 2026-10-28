package com.termux.app.neon;

import android.app.AlertDialog;

import com.termux.R;
import com.termux.app.TermuxActivity;
import com.termux.shared.logger.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** A described list of Termux packages, installed with one tap. Source: assets/neon/catalog.json. */
final class NeonCatalog {

    private static final String LOG_TAG = "NeonCatalog";
    private static final String ASSET_CATALOG = "neon/catalog.json";

    private NeonCatalog() {}

    private static final class Pkg {
        final String name;
        final String category;
        final String description;

        Pkg(String name, String category, String description) {
            this.name = name;
            this.category = category;
            this.description = description;
        }
    }

    static void show(final TermuxActivity activity, final NeonDeck deck) {
        final List<Pkg> packages;
        try {
            packages = load(activity);
        } catch (IOException | JSONException e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load catalog", e);
            return;
        }

        final String[] labels = new String[packages.size()];
        for (int i = 0; i < labels.length; i++) {
            Pkg p = packages.get(i);
            labels[i] = "[" + p.category + "] " + p.name + " - " + p.description;
        }
        final boolean[] checked = new boolean[labels.length];

        new AlertDialog.Builder(activity, R.style.NeonDialog)
            .setTitle(R.string.neon_catalog_title)
            .setMultiChoiceItems(labels, checked, (dialog, which, isChecked) -> checked[which] = isChecked)
            .setPositiveButton(R.string.neon_catalog_install, (dialog, which) -> {
                StringBuilder command = new StringBuilder("pkg install -y");
                int count = 0;
                for (int i = 0; i < checked.length; i++) {
                    if (checked[i]) {
                        command.append(' ').append(packages.get(i).name);
                        count++;
                    }
                }
                if (count == 0) {
                    activity.showToast(activity.getString(R.string.neon_catalog_none_selected), false);
                    return;
                }
                deck.runCommand(command.toString());
            })
            .setNegativeButton(android.R.string.cancel, null)
            .show();
    }

    private static List<Pkg> load(TermuxActivity activity) throws IOException, JSONException {
        boolean arabic = "ar".equals(Locale.getDefault().getLanguage());
        JSONArray array = new JSONObject(NeonIo.readAsset(activity, ASSET_CATALOG)).getJSONArray("packages");
        List<Pkg> packages = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.getJSONObject(i);
            String name = o.optString("name", "");
            // Package names are appended to a shell command, so accept only the characters Termux package names use.
            if (!name.matches("[a-z0-9][a-z0-9+.-]*")) continue;
            String description = o.optString("description", "");
            String ar = o.optString("description_ar", "");
            packages.add(new Pkg(name, o.optString("category", ""), arabic && !ar.isEmpty() ? ar : description));
        }
        return packages;
    }
}
