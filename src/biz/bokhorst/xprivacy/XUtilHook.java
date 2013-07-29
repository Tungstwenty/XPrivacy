package biz.bokhorst.xprivacy;

import android.util.Log;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class XUtilHook extends XHook {

	public XUtilHook(String methodName, String restrictionName, String[] permissions) {
		super(methodName, restrictionName, permissions, null);
	}

	// isXposedEnabled

	@Override
	protected void before(MethodHookParam param) throws Throwable {
		Util.log(this.getClass(), Log.INFO, getMethodName() + "=true");
		param.setResult(true);
	}

	@Override
	protected void after(MethodHookParam param) throws Throwable {
		// Do nothing
	}
}
