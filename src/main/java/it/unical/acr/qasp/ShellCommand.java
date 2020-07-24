package it.unical.acr.qasp;

public class ShellCommand {
	public final static String FILE = "$file";
	private String commandTemplate;
	private String[] binaries;
	

	public ShellCommand(String commandTemplate, String[] binaries) {
		super();
		this.commandTemplate = commandTemplate;
		this.binaries = binaries;
	}

	public String getCommandTemplate() {
		return commandTemplate;
	}

	public String[] getBinaries() {
		return binaries;
	}
	
}

