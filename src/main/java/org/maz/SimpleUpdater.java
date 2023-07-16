/*
 * Copyright (c) 2023.
 * M. Maz.
 */

package org.maz;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;


public class SimpleUpdater {

	/**
	 * Reads data represented by URL e.g. a website and searches for an arbitrary HTML element with the given
	 * id. E.g. &#x3c;div id="version"&#x3e;1.0.4&#x3c;/div&#x3e;
	 *
	 * @return the inner HTML text of this element, which should represent a version string.
	 */
	public static String getRemoteVersionText(URL url, String elementId) throws IOException, ElementNotFoundException {
		URLConnection connection = url.openConnection();
		String encoding = connection.getContentEncoding();
		try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), encoding != null ? encoding : "UTF-8"))) {
			Optional<String> versionLine = br.lines().filter(line -> line.matches(".*<.*id=\"" + elementId + "\".*>[.\\d]+</.*>")).findFirst();
			if (versionLine.isPresent()) {
				return versionLine.get().strip().split("<|>")[2];
			} else
				throw new ElementNotFoundException("Element with id '" + elementId + "' not found in '" + url + "'.");
		} catch (IOException exception) {
			throw new IOException("Could not open URL '" + url + "'.");
		}
	}

	/**
	 * Compares two version strings in the form of "number[.number.number...]"
	 *
	 * @return true if remoteVersionString is higher.
	 */
	public static boolean isRemoteVersionNumberHigher(String thisVersion, String remoteVersion) throws ParseException {
		if (!thisVersion.matches("(\\d?\\.?)+\\d+"))
			throw new ParseException("(This) version string '" + thisVersion + "' does not match criteria 'number[.number.number...]'", 0);
		if (!remoteVersion.matches("(\\d?\\.?)+\\d+"))
			throw new ParseException("(Remote) version string '" + remoteVersion + "' does not match criteria 'number[.number.number...]'", 0);
		ArrayList<String> thisVers = new ArrayList<>(Arrays.asList(thisVersion.split("\\.")));
		ArrayList<String> remoteVers = new ArrayList<>(Arrays.asList(remoteVersion.split("\\.")));
		// Make list of version tokens the same size for save comparison.
		int maxLength = Math.max(thisVers.size(), remoteVers.size());
		for (int i = thisVers.size(); i < maxLength; i++) {
			thisVers.add("0");
		}
		for (int i = remoteVers.size(); i < maxLength; i++) {
			remoteVers.add("0");
		}
		for (int i = 0; i < maxLength; i++) {
			if (Integer.parseInt(remoteVers.get(i)) > Integer.parseInt(thisVers.get(i)))
				return true;
			else if (Integer.parseInt(remoteVers.get(i)) < Integer.parseInt(thisVers.get(i)))
				return false;
		}
		return false;
	}

	/**
	 * After downloading and extracting the new version contained in a temporary directory, start this method and exit your application.
	 * The current version directory will be deleted and the temporary directory will be renamed to the name of the latter.
	 *
	 * @param newVersion The temporary directory containing all files of the new version incl. an executable.
	 * @param executable Name of the executable to be started after updating.
	 */
	public static void updateAndRestart(File newVersion, File executable) throws IOException {
//		if (!(newVersion.exists() && newVersion.isDirectory() && executable.exists() && executable.isFile()))
//			throw new IOException("Provided paths are not existent or not a directory.");
		File rootDir = new File(System.getProperty("user.dir"));
		File restartBat = new File("restart.bat");
		System.out.println("xxxm: " + System.getProperty("user.dir"));
		//xxxm jetzt sollte es funzn
		System.out.printf("xxxm4 " + restartBat.getAbsolutePath() + "   " + restartBat.getName());
		BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(restartBat.getAbsolutePath()));
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(SimpleUpdater.class.getClassLoader().getResourceAsStream("restartTemplate.bat"), StandardCharsets.UTF_8));
		String filesToDelete = Arrays.stream(rootDir.list()).filter(
				  not(name -> name.equals(newVersion.getName()))
							 .and(not(name -> name.equals(restartBat.getName())))).collect(Collectors.joining(" "));
		for (String line : bufferedReader.lines().toList()) {
			bufferedWriter.write(line
					                       .replace("$CURRENT_CONTENT", filesToDelete)
					                       .replace("$NEW_VERSION", newVersion.getPath())
					                       .replace("$EXECUTABLE_TO_START", executable.getName()));
			bufferedWriter.newLine();
		}
		bufferedWriter.close();

		ProcessBuilder processBuilder = new ProcessBuilder("cmd", "/C", restartBat.getName());
		//processBuilder.directory(new File(""));
		processBuilder.inheritIO();
		processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD);
		processBuilder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
		//processBuilder.start();

	}
}
