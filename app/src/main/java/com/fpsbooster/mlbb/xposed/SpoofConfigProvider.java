package com.fpsbooster.mlbb.xposed;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Bundle;

/**
 * Exported read-only ContentProvider that allows the Xposed/LSPatch hook running inside
 * com.mobile.legends to fetch the currently selected SpoofProfile on Android 11–16 even when
 * SELinux or Scoped Storage blocks direct XSharedPreferences file access.
 */
public class SpoofConfigProvider extends ContentProvider {

    public static final String METHOD_GET_PROFILE = "get_profile";

    @Override
    public boolean onCreate() {
        return true;
    }

    private String readSavedProfile(Context context) {
        if (context == null) {
            return SpoofProfiles.DEFAULT_PROFILE_KEY;
        }
        SharedPreferences prefs =
                context.getSharedPreferences(SpoofProfiles.PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(
                SpoofProfiles.KEY_SELECTED_PROFILE,
                SpoofProfiles.DEFAULT_PROFILE_KEY);
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (METHOD_GET_PROFILE.equals(method)) {
            Bundle res = new Bundle();
            res.putString(SpoofProfiles.KEY_SELECTED_PROFILE, readSavedProfile(getContext()));
            return res;
        }
        return null;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        String profileId = readSavedProfile(getContext());
        MatrixCursor cursor = new MatrixCursor(new String[]{SpoofProfiles.KEY_SELECTED_PROFILE}, 1);
        cursor.addRow(new Object[]{profileId});
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return "vnd.android.cursor.item/vnd.com.fpsbooster.mlbb.config";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }
}
