package biz.bokhorst.xprivacy;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Environment;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;

public class PrivacyProvider extends ContentProvider {

	public static final String AUTHORITY = "biz.bokhorst.xprivacy.provider";
	public static final String PREF_RESTRICTION = AUTHORITY;
	public static final String PREF_USAGE = AUTHORITY + ".usage";
	public static final String PREF_SETTINGS = AUTHORITY + ".settings";
	public static final String PATH_RESTRICTION = "restriction";
	public static final String PATH_USAGE = "usage";
	public static final String PATH_SETTINGS = "settings";
	public static final Uri URI_RESTRICTION = Uri.parse("content://" + AUTHORITY + "/" + PATH_RESTRICTION);
	public static final Uri URI_USAGE = Uri.parse("content://" + AUTHORITY + "/" + PATH_USAGE);
	public static final Uri URI_SETTING = Uri.parse("content://" + AUTHORITY + "/" + PATH_SETTINGS);

	public static final String COL_UID = "Uid";
	public static final String COL_RESTRICTION = "Restriction";
	public static final String COL_RESTRICTED = "Restricted";
	public static final String COL_METHOD = "Method";
	public static final String COL_USED = "Used";
	public static final String COL_SETTING = "Setting";
	public static final String COL_VALUE = "Value";

	private static final UriMatcher sUriMatcher;
	private static final int TYPE_RESTRICTION = 1;
	private static final int TYPE_USAGE = 2;
	private static final int TYPE_SETTING = 3;

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, PATH_RESTRICTION, TYPE_RESTRICTION);
		sUriMatcher.addURI(AUTHORITY, PATH_USAGE, TYPE_USAGE);
		sUriMatcher.addURI(AUTHORITY, PATH_SETTINGS, TYPE_SETTING);
	}

	@Override
	public boolean onCreate() {
		return true;
	}

	@Override
	public String getType(Uri uri) {
		if (sUriMatcher.match(uri) == TYPE_RESTRICTION)
			return String.format("vnd.android.cursor.dir/%s.%s", AUTHORITY, PATH_RESTRICTION);
		else if (sUriMatcher.match(uri) == TYPE_USAGE)
			return String.format("vnd.android.cursor.dir/%s.%s", AUTHORITY, PATH_USAGE);
		else if (sUriMatcher.match(uri) == TYPE_SETTING)
			return String.format("vnd.android.cursor.dir/%s.%s", AUTHORITY, PATH_SETTINGS);
		throw new IllegalArgumentException();
	}

	@Override
	@SuppressWarnings("deprecation")
	@SuppressLint("WorldReadableFiles")
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		if (sUriMatcher.match(uri) == TYPE_RESTRICTION && selectionArgs != null && selectionArgs.length >= 2) {
			// Get arguments
			String restrictionName = selection;
			int uid = Integer.parseInt(selectionArgs[0]);
			boolean usage = Boolean.parseBoolean(selectionArgs[1]);
			String methodName = (selectionArgs.length >= 3 ? selectionArgs[2] : null);

			@SuppressWarnings("resource")
			MatrixCursor cursor = new MatrixCursor(
					new String[] { COL_UID, COL_RESTRICTION, COL_METHOD, COL_RESTRICTED });
			SharedPreferences prefs = getContext().getSharedPreferences(PREF_RESTRICTION, Context.MODE_WORLD_READABLE);

			// Build restriction list
			List<String> listRestrictionName;
			if (restrictionName == null)
				listRestrictionName = PrivacyManager.getRestrictions();
			else {
				listRestrictionName = new ArrayList<String>();
				listRestrictionName.add(restrictionName);
			}

			if (uid == 0) {
				// Process restrictions
				for (String eRestrictionName : listRestrictionName) {
					// Get data
					String restrictions = prefs.getString(getRestrictionPref(eRestrictionName), "*");
					List<String> listRestriction = new ArrayList<String>(Arrays.asList(restrictions.split(",")));
					boolean defaultAllowed = listRestriction.get(0).equals("*");
					if (defaultAllowed)
						listRestriction.remove(0);
					else
						throw new IllegalArgumentException();

					// Process data
					for (String sUid : listRestriction) {
						int eUid = Integer.parseInt(sUid);

						// Category
						cursor.addRow(new Object[] { eUid, eRestrictionName, null, true });

						// Exceptions
						for (String eMethodName : PrivacyManager.getMethodNames(eRestrictionName)) {
							boolean allowed = prefs.getBoolean(getExceptionPref(eUid, eRestrictionName, eMethodName),
									false);
							if (allowed)
								cursor.addRow(new Object[] { eUid, eRestrictionName, eMethodName, !allowed });
						}
					}
				}
			} else {
				// Update usage time
				if (usage && restrictionName != null) {
					long timeStamp = new Date().getTime();
					SharedPreferences uprefs = getContext().getSharedPreferences(PREF_USAGE, Context.MODE_PRIVATE);
					SharedPreferences.Editor editor = uprefs.edit();
					editor.putLong(getUsagePref(uid, restrictionName), timeStamp);
					if (methodName != null)
						editor.putLong(getUsagePref(uid, restrictionName, methodName), timeStamp);
					editor.apply();
				}

				// Process restrictions
				for (String eRestrictionName : listRestrictionName) {
					boolean allowed = getAllowed(uid, eRestrictionName, methodName, prefs);
					cursor.addRow(new Object[] { uid, eRestrictionName, null, Boolean.toString(!allowed) });
				}
			}
			return cursor;
		} else if (sUriMatcher.match(uri) == TYPE_USAGE && selectionArgs != null && selectionArgs.length >= 1) {
			// Return usage
			List<String> listRestriction;
			if (selection == null)
				listRestriction = PrivacyManager.getRestrictions();
			else {
				listRestriction = new ArrayList<String>();
				listRestriction.add(selection);
			}
			int uid = Integer.parseInt(selectionArgs[0]);
			String methodName = (selectionArgs.length >= 2 ? selectionArgs[1] : null);
			SharedPreferences prefs = getContext().getSharedPreferences(PREF_USAGE, Context.MODE_PRIVATE);
			MatrixCursor cursor = new MatrixCursor(new String[] { COL_UID, COL_RESTRICTION, COL_METHOD, COL_USED });
			for (String restrictionName : listRestriction)
				if (methodName == null)
					cursor.addRow(new Object[] { uid, restrictionName, null,
							prefs.getLong(getUsagePref(uid, restrictionName), 0) });
				else
					cursor.addRow(new Object[] { uid, restrictionName, methodName,
							prefs.getLong(getUsagePref(uid, restrictionName, methodName), 0) });

			return cursor;
		} else if (sUriMatcher.match(uri) == TYPE_SETTING && selectionArgs == null) {
			// Return setting
			String settingName = selection;
			SharedPreferences prefs = getContext().getSharedPreferences(PREF_SETTINGS, Context.MODE_WORLD_READABLE);
			MatrixCursor cursor = new MatrixCursor(new String[] { COL_SETTING, COL_VALUE });
			if (settingName == null)
				for (String settingKey : prefs.getAll().keySet())
					try {
						cursor.addRow(new Object[] { getSettingName(settingKey), prefs.getString(settingKey, null) });
					} catch (Throwable ex) {
						// Legacy boolean
					}
			else
				cursor.addRow(new Object[] { settingName, prefs.getString(getSettingPref(settingName), null) });
			return cursor;
		}

		throw new IllegalArgumentException(uri.toString());
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// Check access
		enforcePermission();

		throw new IllegalArgumentException(uri.toString());
	}

	@Override
	@SuppressWarnings("deprecation")
	@SuppressLint("WorldReadableFiles")
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		if (sUriMatcher.match(uri) == TYPE_RESTRICTION) {
			// Check access
			enforcePermission();

			// Get arguments
			String restrictionName = selection;
			int uid = values.getAsInteger(COL_UID);
			String methodName = values.getAsString(COL_METHOD);
			boolean allowed = !Boolean.parseBoolean(values.getAsString(COL_RESTRICTED));

			// Update
			updateRestriction(uid, restrictionName, methodName, allowed);

			return 1; // rows
		} else if (sUriMatcher.match(uri) == TYPE_USAGE) {
			// Word readable

			// Get arguments
			int uid = values.getAsInteger(COL_UID);
			String restrictionName = values.getAsString(PrivacyProvider.COL_RESTRICTION);
			String methodName = values.getAsString(COL_METHOD);
			long timeStamp = values.getAsLong(PrivacyProvider.COL_USED);
			Util.log(Log.INFO, String.format("Update usage data %d/%s/%s", uid, restrictionName, methodName));

			// Update usage data
			SharedPreferences uprefs = getContext().getSharedPreferences(PREF_USAGE, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = uprefs.edit();
			editor.putLong(getUsagePref(uid, restrictionName), timeStamp);
			if (methodName != null)
				editor.putLong(getUsagePref(uid, restrictionName, methodName), timeStamp);
			editor.apply();

			return 1;
		} else if (sUriMatcher.match(uri) == TYPE_SETTING) {
			// Check access
			enforcePermission();

			// Get arguments
			String settingName = selection;

			// Update setting
			SharedPreferences prefs = getContext().getSharedPreferences(PREF_SETTINGS, Context.MODE_WORLD_READABLE);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(getSettingPref(settingName), values.getAsString(COL_VALUE));
			editor.apply();
			setPrefFileReadable(PREF_SETTINGS);

			return 1;
		}

		throw new IllegalArgumentException(uri.toString());
	}

	@Override
	@SuppressWarnings("deprecation")
	@SuppressLint("WorldReadableFiles")
	public int delete(Uri uri, String where, String[] selectionArgs) {
		// Check access
		enforcePermission();

		if (sUriMatcher.match(uri) == TYPE_RESTRICTION) {
			int rows = 0;

			// Get argument
			int uid = Integer.parseInt(selectionArgs[0]);

			// Method restrictions
			SharedPreferences prefs = getContext().getSharedPreferences(PREF_RESTRICTION, Context.MODE_WORLD_READABLE);
			SharedPreferences.Editor editor = prefs.edit();
			for (String restrictionName : PrivacyManager.getRestrictions()) {
				for (String methodName : PrivacyManager.getMethodNames(restrictionName)) {
					rows++;
					editor.remove(getExceptionPref(uid, restrictionName, methodName));
				}
			}
			editor.apply();
			setPrefFileReadable(PREF_RESTRICTION);

			// Group restrictions
			for (String restrictionName : PrivacyManager.getRestrictions()) {
				rows++;
				updateRestriction(uid, restrictionName, null, true);
			}

			return rows;
		} else if (sUriMatcher.match(uri) == TYPE_USAGE && selectionArgs != null && selectionArgs.length == 1) {
			// Get arguments
			String restrictionName = where;
			int uid = Integer.parseInt(selectionArgs[0]);

			// Delete audit trail
			int rows = 0;
			String prefix = getUsagePref(uid, restrictionName);
			SharedPreferences prefs = getContext().getSharedPreferences(PREF_USAGE, Context.MODE_PRIVATE);
			SharedPreferences.Editor editor = prefs.edit();
			for (String pref : prefs.getAll().keySet())
				if (pref.startsWith(prefix)) {
					rows++;
					editor.remove(pref);
					Util.log(Log.INFO, "Removed audit=" + pref);
				}
			editor.apply();
			return rows;
		} else if (sUriMatcher.match(uri) == TYPE_SETTING && selectionArgs == null) {
			int rows = 0;
			SharedPreferences prefs = getContext().getSharedPreferences(PREF_SETTINGS, Context.MODE_WORLD_READABLE);
			SharedPreferences.Editor editor = prefs.edit();
			for (String pref : prefs.getAll().keySet()) {
				rows++;
				editor.remove(pref);
				Util.log(Log.INFO, "Removed setting=" + pref);
			}
			editor.apply();
			setPrefFileReadable(PREF_SETTINGS);
			return rows;
		}

		throw new IllegalArgumentException(uri.toString());
	}

	// Public helper methods

	public static void setPrefFileReadable(String preference) {
		new File(getPrefFileName(preference)).setReadable(true, false);
	}

	// The following methods are used as fallback, when:
	// - there is no context (Java threads)
	// - the content provider cannot be queried (PackageManagerService)

	public static boolean getRestrictedFallback(XHook hook, int uid, String restrictionName, String methodName) {
		// Get restrictions
		SharedPreferencesEx xprefs = new SharedPreferencesEx(new File(getPrefFileName(PREF_RESTRICTION)));
		return !getAllowed(uid, restrictionName, methodName, xprefs);
	}

	public static String getSettingFallback(String settingName, String defaultValue) {
		// Get restrictions
		SharedPreferencesEx xprefs = new SharedPreferencesEx(new File(getPrefFileName(PREF_SETTINGS)));
		return xprefs.getString(getSettingPref(settingName), defaultValue);
	}

	// Private helper methods

	private void enforcePermission() throws SecurityException {
		if (Binder.getCallingUid() != Process.myUid())
			throw new SecurityException();
	}

	private static boolean getAllowed(int uid, String restrictionName, String methodName, SharedPreferences prefs) {
		// Get restrictions
		String restrictions = prefs.getString(getRestrictionPref(restrictionName), "*");

		// Decode restrictions
		List<String> listRestriction = new ArrayList<String>(Arrays.asList(restrictions.split(",")));
		boolean defaultAllowed = listRestriction.get(0).equals("*");

		// Check if restricted
		boolean allowed = !listRestriction.contains(Integer.toString(uid));
		if (!defaultAllowed)
			allowed = !allowed;

		// Check for exception
		if (!allowed && methodName != null)
			if (prefs.getBoolean(getExceptionPref(uid, restrictionName, methodName), false))
				allowed = true;

		return allowed;
	}

	@SuppressWarnings("deprecation")
	@SuppressLint("WorldReadableFiles")
	private void updateRestriction(int uid, String restrictionName, String methodName, boolean allowed) {
		// Get restrictions
		SharedPreferences prefs = getContext().getSharedPreferences(PREF_RESTRICTION, Context.MODE_WORLD_READABLE);
		String restrictions = prefs.getString(getRestrictionPref(restrictionName), "*");

		// Decode restrictions
		List<String> listRestriction = new ArrayList<String>(Arrays.asList(restrictions.split(",")));
		boolean defaultAllowed = listRestriction.get(0).equals("*");

		// Allow or deny
		String sUid = Integer.toString(uid);
		if (defaultAllowed ? allowed : !allowed)
			listRestriction.remove(sUid);
		if (defaultAllowed ? !allowed : allowed)
			if (!listRestriction.contains(sUid))
				listRestriction.add(sUid);

		// Encode restrictions
		restrictions = TextUtils.join(",", listRestriction.toArray(new String[0]));

		// Update restriction
		SharedPreferences.Editor editor = prefs.edit();
		if (methodName == null || !allowed)
			editor.putString(getRestrictionPref(restrictionName), restrictions);
		if (methodName != null)
			editor.putBoolean(getExceptionPref(uid, restrictionName, methodName), allowed);
		editor.apply();
		setPrefFileReadable(PREF_RESTRICTION);
	}

	private static String getPrefFileName(String preference) {
		String packageName = PrivacyManager.class.getPackage().getName();
		return Environment.getDataDirectory() + "/data/" + packageName + "/shared_prefs/" + preference + ".xml";
	}

	private static String getRestrictionPref(String restrictionName) {
		return String.format("%s.%s", COL_RESTRICTED, restrictionName);
	}

	@SuppressLint("DefaultLocale")
	private static String getExceptionPref(int uid, String restrictionName, String methodName) {
		return String.format("%s.%d.%s.%s", COL_METHOD, uid, restrictionName, methodName);
	}

	@SuppressLint("DefaultLocale")
	private static String getUsagePref(int uid, String restrictionName) {
		return String.format("%s.%d.%s", COL_USED, uid, restrictionName);
	}

	@SuppressLint("DefaultLocale")
	private static String getUsagePref(int uid, String restrictionName, String methodName) {
		return String.format("%s.%d.%s.%s", COL_USED, uid, restrictionName, methodName);
	}

	private static String getSettingPref(String settingName) {
		return String.format("%s.%s", COL_SETTING, settingName);
	}

	private static String getSettingName(String settingKey) {
		return settingKey.substring(COL_SETTING.length() + 1);
	}
}
