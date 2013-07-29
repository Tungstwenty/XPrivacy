package biz.bokhorst.xprivacy;

import java.util.ArrayList;

import android.telephony.SmsMessage;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;

public class XSmsManager extends XHook {

	private enum SmsTargets implements XHook.HookInfo {
		GET("getAllMessagesFromIcc", PrivacyManager.cMessages, new String[]{ "RECEIVE_SMS" }),
		SEND_DATA("sendDataMessage", PrivacyManager.cCalling, new String[]{ "SEND_SMS" }),
		SEND_MPART("sendMultipartTextMessage", PrivacyManager.cCalling, new String[]{ "SEND_SMS" }),
		SEND_TEXT("sendTextMessage", PrivacyManager.cCalling, new String[]{ "SEND_SMS" });
		
		private String methodName;
		private String restrictionName;
		private String[] permissions;

		private SmsTargets(String methodName, String restrictionName, String[] permissions) {
			this.methodName = methodName;
			this.restrictionName = restrictionName;
			this.permissions = permissions;
		}

		@Override
		public String getMethodName() {
			return methodName;
		}

		@Override
		public String getRestrictionName() {
			return restrictionName;
		}

		@Override
		public String[] getPermissions() {
			return permissions;
		}

		@Override
		public String getSpecifier() {
			return null;
		}
	}

	SmsTargets hookTarget;

	public static void installHooks() {
		for (SmsTargets target : SmsTargets.values()) {
			XPrivacy.hook(new XSmsManager(target), "android.telephony.SmsManager");
		}
	}

	private XSmsManager(SmsTargets target) {
		super(target);
		hookTarget = target;
	}

	// @formatter:off

	// public static ArrayList<SmsMessage> getAllMessagesFromIcc()
	// public void sendDataMessage(String destinationAddress, String scAddress, short destinationPort, byte[] data, PendingIntent sentIntent, PendingIntent deliveryIntent)
	// public void sendMultipartTextMessage(String destinationAddress, String scAddress, ArrayList<String> parts, ArrayList<PendingIntent> sentIntents, ArrayList<PendingIntent> deliveryIntents)
	// public void sendTextMessage(String destinationAddress, String scAddress, String text, PendingIntent sentIntent, PendingIntent deliveryIntent)
	// frameworks/base/telephony/java/android/telephony/SmsManager.java

	// @formatter:on

	@Override
	protected void before(MethodHookParam param) throws Throwable {
		String methodName = param.method.getName();
		if (!methodName.equals("getAllMessagesFromIcc"))
			if (isRestricted())
				param.setResult(null);
	}

	@Override
	protected void after(MethodHookParam param) throws Throwable {
		if (param.getResult() != null)
			if (isRestricted())
				param.setResult(new ArrayList<SmsMessage>());
	}
}
