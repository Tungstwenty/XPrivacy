package biz.bokhorst.xprivacy;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class XSystemProperties extends XHook {

	private String mPropertyName;
	private boolean isGetMethod;

	public static void installHooks() {
		String[] props = new String[] { "ro.gsm.imei", "net.hostname", "ro.serialno", "ro.boot.serialno",
				"ro.boot.wifimacaddr", "ro.boot.btmacaddr" };
		String[] getters = new String[] { "get", "getBoolean", "getInt", "getLong" };
		for (String prop : props)
			for (String getter : getters)
				XPrivacy.hook(new XSystemProperties(getter, PrivacyManager.cIdentification, new String[] {}, prop),
						"android.os.SystemProperties");
	}
	
	private XSystemProperties(String methodName, String restrictionName, String[] permissions, String propertyName) {
		super(methodName, restrictionName, permissions, propertyName);
		mPropertyName = propertyName;
		isGetMethod = methodName.equals("get");
	}

	// public static String get(String key)
	// public static String get(String key, String def)
	// public static boolean getBoolean(String key, boolean def)
	// public static int getInt(String key, int def)
	// public static long getLong(String key, long def)
	// frameworks/base/core/java/android/os/SystemProperties.java

	@Override
	protected void before(MethodHookParam param) throws Throwable {
		String key = (String) param.args[0];
		if (mPropertyName.equals(key))
			if (isRestricted(mPropertyName))
				if (isGetMethod)
					param.setResult(PrivacyManager.getDefacedProp(mPropertyName));
				else
					param.setResult(param.args[1]);
	}

	@Override
	protected void after(MethodHookParam param) throws Throwable {
		// Do nothing
	}
}
