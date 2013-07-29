package biz.bokhorst.xprivacy;

import java.lang.reflect.Field;
import java.util.ArrayList;

import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;

import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import static de.robv.android.xposed.XposedHelpers.findField;

public class XWifiManager extends XHook {

	private enum WifiTargets implements XHook.HookInfo {
		NETWORKS("getConfiguredNetworks", PrivacyManager.cNetwork, new String[]{ "ACCESS_WIFI_STATE" }),
		CONN_INFO("getConnectionInfo", PrivacyManager.cNetwork, new String[]{ "ACCESS_WIFI_STATE" }),
		// This is to fake "offline", no permission required
		CONN_INFO_INET("getConnectionInfo", PrivacyManager.cInternet, null),
		DHCP_INFO("getDhcpInfo", PrivacyManager.cNetwork, new String[]{ "ACCESS_WIFI_STATE" }),
		SCAN_RESULTS("getScanResults", PrivacyManager.cNetwork, new String[]{ "ACCESS_WIFI_STATE" }),
		SCAN_RESULTS_LOC("getScanResults", PrivacyManager.cLocation, new String[]{ "ACCESS_WIFI_STATE" });
		
		private String methodName;
		private String restrictionName;
		private String[] permissions;

		private WifiTargets(String methodName, String restrictionName, String[] permissions) {
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

	WifiTargets hookTarget;
	
	private static Field fldSupplicantState;
	private static Field fldBSSID;
	private static Field fldIpAddress;
	private static Field fldMacAddress;
	private static Field fldSSID;
	private static Field fldWifiSsid;

	public static void installHooks() {
		try {
			fldSupplicantState = findField(WifiInfo.class, "mSupplicantState");
		} catch (Throwable ex) {
			Util.bug(XWifiManager.class, ex);
		}
		try {
			fldBSSID = findField(WifiInfo.class, "mBSSID");
		} catch (Throwable ex) {
			Util.bug(XWifiManager.class, ex);
		}
		try {
			fldIpAddress = findField(WifiInfo.class, "mIpAddress");
		} catch (Throwable ex) {
			Util.bug(XWifiManager.class, ex);
		}
		try {
			fldMacAddress = findField(WifiInfo.class, "mMacAddress");
		} catch (Throwable ex) {
			Util.bug(XWifiManager.class, ex);
		}
		try {
			fldSSID = findField(WifiInfo.class, "mSSID");
		} catch (Throwable ex) {
			try {
				fldWifiSsid = findField(WifiInfo.class, "mWifiSsid");
			} catch (Throwable exex) {
				Util.bug(XWifiManager.class, ex);
			}
		}

		for (WifiTargets target : WifiTargets.values()) {
			XPrivacy.hook(new XWifiManager(target), "android.net.wifi.WifiManager");
		}
	}

	private XWifiManager(WifiTargets target) {
		super(target);
		hookTarget = target;
	}

	// public List<WifiConfiguration> getConfiguredNetworks()
	// public WifiInfo getConnectionInfo()
	// public DhcpInfo getDhcpInfo()
	// public List<ScanResult> getScanResults()
	// frameworks/base/wifi/java/android/net/wifi/WifiManager.java

	@Override
	protected void before(MethodHookParam param) throws Throwable {
		switch (hookTarget) {
		case NETWORKS:
			if (isRestricted())
				param.setResult(new ArrayList<WifiConfiguration>());
			break;
		case SCAN_RESULTS:
		case SCAN_RESULTS_LOC:
			if (isRestricted())
				param.setResult(new ArrayList<ScanResult>());
			break;
		default:
		}
	}

	@Override
	protected void after(MethodHookParam param) throws Throwable {
		switch (hookTarget) {
		case CONN_INFO:
		case CONN_INFO_INET:
			// frameworks/base/wifi/java/android/net/wifi/WifiInfo.java
			WifiInfo wInfo = (WifiInfo) param.getResult();
			if (wInfo != null)
				if (isRestricted())
					if (hookTarget.equals(WifiTargets.CONN_INFO_INET)) {
						// Supplicant state
						if (fldSupplicantState != null)
							fldSupplicantState.set(wInfo, SupplicantState.DISCONNECTED);
					} else {
						// BSSID
						if (fldBSSID != null)
							fldBSSID.set(wInfo, PrivacyManager.getDefacedProp("MAC"));
						// IP address
						if (fldIpAddress != null)
							fldIpAddress.set(wInfo, PrivacyManager.getDefacedInetAddress());
						// MAC address
						if (fldMacAddress != null)
							fldMacAddress.set(wInfo, PrivacyManager.getDefacedProp("MAC"));
						// SSID
						if (fldSSID != null)
							fldSSID.set(wInfo, PrivacyManager.getDefacedProp("SSID"));
						else
							if (fldWifiSsid != null)
								fldWifiSsid.set(wInfo, PrivacyManager.getDefacedProp("SSID"));
					}
			break;
		case DHCP_INFO:
			// frameworks/base/core/java/android/net/DhcpInfo.java
			DhcpInfo dInfo = (DhcpInfo) param.getResult();
			if (dInfo != null)
				if (isRestricted()) {
					dInfo.ipAddress = PrivacyManager.getDefacedIPInt();
					dInfo.gateway = PrivacyManager.getDefacedIPInt();
					dInfo.dns1 = PrivacyManager.getDefacedIPInt();
					dInfo.dns2 = PrivacyManager.getDefacedIPInt();
					dInfo.serverAddress = PrivacyManager.getDefacedIPInt();
				}
			break;
		default:
		}
	}
}
