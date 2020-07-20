package it.unical.acr.qasp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class Utilities {

	private static final Logger LOGGER = Logger.getLogger(Utilities.class.getName());

	public static String runAndGetString(String command) {

		StringBuilder sb = new StringBuilder();
		for (String out : runAndGetList(command)) {
			sb.append(out);
		}
		return sb.toString();
	}

	private static List<String> runAndGetList(String command) {
		return runAndGetList(new String[] { "/bin/bash", "-c", command });
	}

	// TODO handle returning error codes
	public static List<String> runAndGetList(String[] command) {

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
				String commandString = "";
				for (String c : command) {
					commandString += c + " ";
				}
//				System.out.println(
//						"Failed to execute the following command: " +  commandString + " due to the following error(s):");
				LOGGER.log(QAsp.DEBUG_LEVEL, "Command: " + commandString + " returned code " + exitVal);
				try (final BufferedReader b = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
					String errLine;
					if ((errLine = b.readLine()) != null)
						LOGGER.log(QAsp.DEBUG_LEVEL, errLine);
				} catch (final IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}

			}
			return output;
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return null;

	}

	public static List<String> executeBinaries(String commandTemplate, String... binaries) {

		 
		List<File> resolvedBinaries = new ArrayList<>();
		for (String binary : binaries) {			
			try {
				URI jarURI = getJarURI(); 
				URI exe = getFile(jarURI, binary);
				File exeFile = new File(exe);
				exeFile.setExecutable(true);
				resolvedBinaries.add(exeFile);

			} catch (ZipException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return runAndGetList(String.format(commandTemplate, resolvedBinaries.toArray()));

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

	private static URI getJarURI() throws URISyntaxException {
		final ProtectionDomain domain;
		final CodeSource source;
		final URL url;
		final URI uri;

		domain = QAsp.class.getProtectionDomain();
		source = domain.getCodeSource();
		url = source.getLocation();
		uri = url.toURI();

		return (uri);
	}

	private static URI getFile(final URI where, final String fileName) throws ZipException, IOException {
		final File location;
		final URI fileURI;

		location = new File(where);

		// not in a JAR, just return the path on disk
		if (location.isDirectory()) {
			fileURI = URI.create(where.toString() + fileName);
		} else {
			final ZipFile zipFile;

			zipFile = new ZipFile(location);

			try {
				fileURI = extract(zipFile, fileName);
			} finally {
				zipFile.close();
			}
		}

		return (fileURI);
	}

	private static URI extract(final ZipFile zipFile, final String fileName) throws IOException {
		final File tempFile;
		final ZipEntry entry;
		final InputStream zipStream;
		OutputStream fileStream;

		tempFile = File.createTempFile(fileName, Long.toString(System.currentTimeMillis()));
		tempFile.deleteOnExit();
		entry = zipFile.getEntry(fileName);

		if (entry == null) {
			throw new FileNotFoundException("cannot find file: " + fileName + " in archive: " + zipFile.getName());
		}

		zipStream = zipFile.getInputStream(entry);
		fileStream = null;

		try {
			final byte[] buf;
			int i;

			fileStream = new FileOutputStream(tempFile);
			buf = new byte[1024];
			i = 0;

			while ((i = zipStream.read(buf)) != -1) {
				fileStream.write(buf, 0, i);
			}
		} finally {
			close(zipStream);
			close(fileStream);
		}

		return (tempFile.toURI());
	}

	private static void close(final Closeable stream) {
		if (stream != null) {
			try {
				stream.close();
			} catch (final IOException ex) {
				ex.printStackTrace();
			}
		}
	}

}
