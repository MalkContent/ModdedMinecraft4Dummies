package mm4d.jsonhelpers;

import com.google.gson.internal.LinkedTreeMap;

public class LauncherProfile {
	public LauncherVersion launcherVersion;
	public String clientToken;
	public LinkedTreeMap<String, Profile> profiles;
	public String selectedUser;
	public String selectedProfile;
	public Object authenticationDatabase; 
}
