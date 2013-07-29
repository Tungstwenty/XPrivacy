package biz.bokhorst.xprivacy;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class XCamera extends XHook {

	public XCamera(String methodName, String restrictionName, String[] permissions) {
		super(methodName, restrictionName, permissions, null);
	}

	// @formatter:off

	// public final void setPreviewCallback(PreviewCallback cb)
	// public final void setPreviewCallbackWithBuffer(PreviewCallback cb)
	// public final void setOneShotPreviewCallback(PreviewCallback cb)
	// public final void takePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback jpeg)
	// public final void takePicture(ShutterCallback shutter, PictureCallback raw, PictureCallback postview, PictureCallback jpeg)
	// frameworks/base/core/java/android/hardware/Camera.java

	// @formatter:on

	@Override
	protected void before(MethodHookParam param) throws Throwable {
		if (isRestricted()) {
			param.setResult(null);
			notifyUser(this.getClass().getSimpleName());
		}
	}

	@Override
	protected void after(MethodHookParam param) throws Throwable {
		// Do nothing
	}
}
