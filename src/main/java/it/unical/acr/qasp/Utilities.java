package it.unical.acr.qasp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class Utilities {

	public static String runAndGetString(String command) {

		StringBuilder sb = new StringBuilder();
		for(String out: runAndGetList(command)) {
			sb.append(out);
		}
		return sb.toString();
	}
	
	public static List<String> runAndGetList(String command) {
		return runAndGetList(new String[ ] {"/bin/bash", "-c", command});
	}

	public static List<String> runAndGetList(String [] command) {

		Process process;
		ProcessBuilder builder = new ProcessBuilder("/bin/bash");
		builder.redirectErrorStream(true);
		try {
			process = Runtime.getRuntime().exec(command);

			ArrayList<String> output = new ArrayList<>();

			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				output.add(line);
			}

			int exitVal = process.waitFor();
			if (exitVal == 0) {
//				System.out.println("Success!");
//				for (String out : output) {
//					System.out.println(out);
//				}
			} else {
//				String commandString="";
//				for(String c: command) {
//					commandString+=c+" ";
//				}
//				System.out.println(
//						"Failed to execute the following command: " +  commandString + " due to the following error(s):");
				try (final BufferedReader b = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
					String errLine;
					if ((errLine = b.readLine()) != null)
						System.err.println(errLine);
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
			return output;
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return null;

	}
	
	public static File writeToTempFile(String text) {
		File tempFile = null;
		try {
			tempFile = File.createTempFile("tempfile", ".tmp");

			tempFile.deleteOnExit();
			BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile));
			bw.write(text);
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tempFile;
	}
	
}
