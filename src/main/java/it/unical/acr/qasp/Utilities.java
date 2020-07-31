package it.unical.acr.qasp;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class Utilities {

	private static final Logger LOGGER = Logger.getLogger(Utilities.class.getName());
	private static final HashSet<Integer> ERROR_CODES = new HashSet<>(Arrays.asList(255));

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

				boolean fail = ERROR_CODES.contains(exitVal);
				if (fail) {
					System.out.println("Failed to execute the following command: " + commandString
							+ " due to the following error(s):");
				}
				LOGGER.log(QAsp.DEBUG_LEVEL, "Command: " + commandString + " returned code " + exitVal);
				try (final BufferedReader b = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
					String errLine;
					if ((errLine = b.readLine()) != null) {
						LOGGER.log(QAsp.DEBUG_LEVEL, errLine);
						if (fail) {
							System.err.println(errLine);
						}

					}
				} catch (final IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
				// lp2lp error code
				if (fail) {
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

	public static String getCommand(ShellCommand shellCommand) {
		return String.format(shellCommand.getCommandTemplate(), Arrays.asList(shellCommand.getBinaries()));
	}

	public static List<String> executeBinaries(ShellCommand shellCommand, String... files) {
		String command = shellCommand.getCommandTemplate();
		for (String file : files) {
			command = command.replace(ShellCommand.FILE, file);
		}
		return executeBinaries(command, shellCommand.getBinaries());
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
				e.printStackTrace();
				System.exit(-1);
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			} catch (URISyntaxException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		return runAndGetList(String.format(commandTemplate, resolvedBinaries.toArray()));

	}

	public static File getTempFile() {
		File tempFile = null;
		try {
			tempFile = File.createTempFile("tempfile", ".tmp");

			tempFile.deleteOnExit();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tempFile;
	}

	public static File writeToTempFile(String text) {

		File tempFile = getTempFile();
		try {
			Files.write(tempFile.toPath(), text.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return tempFile;
	}

	public static File writeToTempFile(List<String> text) {

		File tempFile = getTempFile();
		try {
			Files.write(tempFile.toPath(), text, StandardCharsets.UTF_8);
		} catch (IOException e) {
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

		File tentativeFilePath = new File("./target/classes/" + fileName);
		if (location.isDirectory()) {
			// not in a JAR, just return the path on disk
			fileURI = URI.create(where.toString() + fileName);
		} else if (tentativeFilePath.exists()) {
			// JAR, but the file exists
			fileURI = tentativeFilePath.toURI();
		} else {			
			// JAR, and need to extract file
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
		System.out.println("extracted "+tempFile.toURI());
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

	public static List<String> generateRandomQDimacsInstance() {
		// "%s -c 1600 -b 5 -bs 10 -bs 5 -bs 15 -bs 10 -bs 25 -bc 3 -bc 3 -bc 2 -bc 2-bc 1"
		ShellCommand blocksqbfCommand = new ShellCommand("%s", new String[] { "./blocksqbf" });
		List<String> qdimacs = Utilities.executeBinaries(blocksqbfCommand);
		return qdimacs;
	}

	public static boolean caqe(File inputFile) {
		ShellCommand caqeCommand = new ShellCommand("%s $file", new String[] { "./caqe" });
		List<String> caqeResult = Utilities.executeBinaries(caqeCommand, inputFile.toString());
		return caqeResult.get(caqeResult.size() - 1).equals("c Satisfiable");
	}

	public static QDimacsProgram parseQDimacs(List<String> randomQDimacs) {
		QDimacsProgramBuilder builder = new QDimacsProgramBuilder();
		for (String line : randomQDimacs) {
			builder.addRow(line);
		}
		return builder.getProgram();

	}

}
