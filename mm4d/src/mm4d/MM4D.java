package mm4d;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import mm4d.jsonhelpers.LauncherProfile;
import mm4d.jsonhelpers.Profile;

public class MM4D {

	private static String cfgName = "mm4d.cfg";
	private static String forgeFolder = "forge";
	private static String mcPath = System.getenv("APPDATA") + "\\.minecraft\\";
	private static String lpjson = "launcher_profiles.json";
	private static String profileDir = "";
	private static Properties cfg;

	public static void main(String[] args) {
		cfg = new Properties();
		setupConfig(cfg);

		String[] installStrings = cfg.getProperty("install").split(",\\s*");
		boolean installForge = false;
		boolean installMods = false;
		boolean installConfig = false;

		for (int i = 0; i < installStrings.length; i++) {
			switch (installStrings[i].toLowerCase()) {
			case "forge":
				installForge = true;
				break;
			case "mods":
				installMods = true;
				break;
			case "config":
				installConfig = true;
				break;
			default:
				return;
			}
		}

		profileDir = (Boolean.parseBoolean(cfg.getProperty("dirRelative")) ? mcPath : "")
				+ cfg.getProperty("profileDir") + "\\";

		if (installForge)
			forge();
		if (installMods)
			copyFolder("mods");
		if (installConfig)
			copyFolder("config");
	}

	private static void copyFolder(String dirName) {
		File curDir = new File(profileDir + dirName);

		if (curDir.exists() && curDir.isDirectory()) {
			if (Boolean.parseBoolean(cfg.getProperty("makeBackups"))) {
				try {
					ZipFileUtil.zipDirectory(curDir, new File(dirName + ".zip"));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			deleteDir(curDir.getPath());
		} else
			curDir.mkdirs();
		copyDir(dirName, curDir.getPath());
	}

	private static void deleteDir(String dirName) {
		File delDir = new File(dirName);
		File[] files = delDir.listFiles();
		if (files != null) {
			File curFile;
			for (int i = 0; i < files.length; i++) {
				curFile = files[i];
				if (curFile.exists()) {
					if (curFile.isDirectory())
						deleteDir(dirName + "\\" + curFile.getName());
				}
			}
		}
	}

	private static void copyDir(String dirName, String destination) {
		File sourceDir = new File(dirName);
		File[] files = sourceDir.listFiles();
		if (files != null) {
			File curFile;
			for (int i = 0; i < files.length; i++) {
				curFile = files[i];
				if (curFile.exists()) {
					String destPath = destination + "\\" + curFile.getName();
					try {
						Files.copy(Paths.get(curFile.getPath()), Paths.get(destPath),
								StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
					} catch (IOException e) {
						e.printStackTrace();
					}
					if (curFile.isDirectory())
						copyDir(dirName + "\\" + curFile.getName(), destPath);
				}

			}
		}
	}

	private static void forge() {
		// TODO: cleanup try/catch, add returns, add dialogue, beautify json
		// output, remove useHopperCrashService if true
		String forgejar = cfg.getProperty("forgejar", "");
		File[] files = new File(forgeFolder).listFiles();
		if (forgejar.isEmpty() || !filePresent(forgejar, files)) {
			if (files.length > 0)
				forgejar = files[0].getName();
			else
				return;
		}
		try {
			Runtime.getRuntime().exec("java -jar " + forgeFolder + "\\" + forgejar).waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return;
		}

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		JsonReader reader = null;
		try {
			reader = new JsonReader(new FileReader(mcPath + lpjson));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		if (reader == null)
			return;
		LauncherProfile lp = gson.fromJson(reader, LauncherProfile.class);
		try {
			reader.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		LinkedTreeMap<String, Profile> profiles = lp.profiles;
		String profileName = cfg.getProperty("profileName");
		Profile newProfile = profiles.get(profileName);
		if (newProfile == null) {
			newProfile = new Profile();
			newProfile.name = profileName;
		}
		Matcher m = Pattern.compile("forge-([^-]*)").matcher(forgejar);
		m.find();
		newProfile.lastVersionId = m.group(1) + "-forge" + forgejar.substring(6, forgejar.length() - 14);

		newProfile.gameDir = profileDir;
		profiles.put(profileName, newProfile);
		if (Boolean.parseBoolean(cfg.getProperty("deleteForgeProfile")))
			profiles.remove("forge");
		lp.selectedProfile = profileName;

		JsonWriter writer = null;
		try {
			writer = new JsonWriter(new FileWriter(mcPath + lpjson));
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (writer == null)
			return;
		gson.toJson(lp, LauncherProfile.class, writer);
		try {
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		File curDir = new File(profileDir);
		if (!curDir.exists() || !curDir.isDirectory())
			curDir.mkdirs();
		curDir = new File(profileDir + "\\config");
		if (!curDir.exists() || !curDir.isDirectory())
			curDir.mkdir();
		curDir = new File(profileDir + "\\mods");
		if (!curDir.exists() || !curDir.isDirectory())
			curDir.mkdir();
	}

	private static boolean filePresent(String filename, File[] files) {
		File curfile;
		for (int i = 0; i < files.length; i++) {
			curfile = files[i];
			if (curfile.isFile() && curfile.getName().equals(filename))
				return true;
		}
		return false;
	}

	private static void setupConfig(Properties cfg) {
		InputStream inStream = null;
		OutputStream outStream = null;
		try {
			inStream = new FileInputStream(cfgName);
			cfg.load(inStream);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (inStream != null)
					inStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		{
			HashMap<String, String> defaultVals = new HashMap<String, String>();
			defaultVals.put("profileName", "moddedMC");
			defaultVals.put("install", "forge, mods, config");
			defaultVals.put("profileDir", "moddedMC");
			defaultVals.put("dirRelative", "true");
			defaultVals.put("makeBackups", "true");
			defaultVals.put("deleteForgeProfile", "true");
			defaultVals.put("forgejar", "");

			for (Iterator<String> iterator = defaultVals.keySet().iterator(); iterator.hasNext();) {
				String key = iterator.next();
				if (cfg.getProperty(key) == null)
					cfg.setProperty(key, defaultVals.get(key));
			}
		}

		try {
			outStream = new FileOutputStream(cfgName);
			cfg.store(outStream, "Config for MM4D");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (outStream != null)
					outStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
