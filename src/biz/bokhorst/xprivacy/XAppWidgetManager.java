package biz.bokhorst.xprivacy;

import java.util.ArrayList;

import android.appwidget.AppWidgetProviderInfo;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class XAppWidgetManager extends XHook {

	public XAppWidgetManager(String methodName, String restrictionName, String[] permissions) {
		super(methodName, restrictionName, permissions, null);
	}

	// public List<AppWidgetProviderInfo> getInstalledProviders()
	// frameworks/base/core/java/android/appwidget/AppWidgetManager.java

	@Override
	protected void before(MethodHookParam param) throws Throwable {
		if (isRestricted())
			param.setResult(new ArrayList<AppWidgetProviderInfo>());
	}

	@Override
	protected void after(MethodHookParam param) throws Throwable {
		// Do nothing
	}
}
