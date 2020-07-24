package it.unical.acr.qasp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import it.unical.acr.qasp.QCIRProgram.QCIRFormatType;
import it.unical.mat.dlv.parser.Builder;
import it.unical.mat.dlv.parser.Director;
import it.unical.mat.dlv.parser.ParseException;
import it.unical.mat.dlv.program.Comment;
import it.unical.mat.dlv.program.Program;
import it.unical.mat.dlv.program.ProgramPredicate;
import it.unical.mat.dlv.program.SimpleProgramBuilder;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "java -jar qasp.jar", version = "qasp 0.1", description = "A solver of ASP(Q) programs", mixinStandardHelpOptions = true)
public class QAsp implements Callable<Integer> {

	public final static String EXISTS = "@exists";
	public final static String FORALL = "@forall";
	public final static String CONSTRAINT = "@constraint";
	public final static ShellCommand RARE_QS_COMMAND_TEMPLATE = new ShellCommand(
			"%s $file -read-qcir -write-gq | %s - -prenex -write-gq | %s -",
			new String[] { "fmla", "qcir-conv.py", "rareqs-nn" });
	public final static ShellCommand QUABS_COMMAND_TEMPLATE = new ShellCommand("%s --partial-assignment $file",
			new String[] { "quabs" });
	public final static QCIRFormatType QCIR_FORMAT = QCIRFormatType.CLEANSED;
	public final static ShellCommand QCIR_SOLVER = QUABS_COMMAND_TEMPLATE;
	private static final Logger LOGGER = Logger.getLogger(QAsp.class.getName());
	public static final Level DEBUG_LEVEL = Level.INFO;

	@Parameters(index = "0", description = "ASP(Q) input file")
	private File inputFile;

//    @Option(names = { "-m", "--get-models" }, description = "Print models of the most external quantified program.")
//    private boolean printExistentialModels;

	@Option(names = { "-n",
			"--n-models" }, description = "Number of models of the most external quantified program, if existentially quantified. (default = 0, -1 to get all models).")
	private int numberOfModels = 0;

	public Integer call() {
		if (!inputFile.exists()) {
			throw new IllegalArgumentException("Input file does not exists " + inputFile);
		}
		QAspResult result = solve(inputFile);

		System.out.println(result);
		return 0;
	}

	public static void main(String[] args) {
		int exitCode = new CommandLine(new QAsp()).execute(args);
		System.exit(exitCode);
	}

	public List<String> ground(String program) {

		// TODO fix
		program = program.replaceAll(" v ", " | ");

		File tempFile = Utilities.writeToTempFile(program);
		// Run a shell command
		String command = "%s --output=SMODELS " + tempFile;
		return Utilities.executeBinaries(command, "gringo");
	}

	private Program readAspProgram(File file) throws FileNotFoundException, ParseException {
		FileInputStream fis = new FileInputStream(file);
		Director d = new Director(fis);
		Builder b = new SimpleProgramBuilder();
		d.configureBuilder(b);
		d.start();
		Program p = (Program) b.getProductHandler();
		return p;
	}

	public QuantifiedProgram readQuantifiedProgram(File filename) {

		Program p = null;
		try {
			p = readAspProgram(filename);
		} catch (FileNotFoundException e) {
			LOGGER.severe("Unable to find file " + filename);
			System.exit(-1);
		} catch (ParseException e) {
			LOGGER.severe("Unable to parse file " + filename);
			LOGGER.severe(e.toString());
			System.exit(-1);
		}

		LOGGER.log(DEBUG_LEVEL, "Printing of parsed program:");
		List<String> validQuantifiers = Arrays.asList(EXISTS, FORALL, CONSTRAINT);
		LOGGER.log(DEBUG_LEVEL, "\n" + p.toString());
		LOGGER.log(DEBUG_LEVEL, "" + p.size());
		List<Program> programs = new ArrayList<>();
		List<String> quantifiers = new ArrayList<>();
		for (int i = 0; i < p.size(); i++) {
			if (p.get(i) instanceof Comment) {
				String comment = ((Comment) p.get(i)).getContent().trim();
				if (validQuantifiers.contains(comment)) {
					programs.add(new Program());
					quantifiers.add(comment);
				}
			} else if (programs.size() == 0) {
				throw new RuntimeException("Unexpected error while parsing program, was expecting "
						+ validQuantifiersString() + " but got " + p.get(i));
			}
			if (!programs.isEmpty()) {
				programs.get(programs.size() - 1).add(p.get(i));
			}
		}
		return new QuantifiedProgram(programs, quantifiers);

	}

	private String validQuantifiersString() {
		return FORALL + ", " + EXISTS + ", or " + CONSTRAINT;
	}

	private QAspResult solve(File programFile) {
		QuantifiedProgram qp = readQuantifiedProgram(programFile);
		
		if(qp.getQuantifiers().get(0).equals(FORALL))
		{
			if(numberOfModels!=0) {
				System.out.println("Warning: ignoring argument number of models (n) since the most external program is universally quantified");
			}
			numberOfModels = 0;
		}

		Set<String> predicates = new HashSet<>();
		Set<GroundAtom> groundAtoms = new HashSet<>();
		Set<Integer> allVars = new HashSet<>();

		Map<String, Integer> atom2var = new HashMap<>();
		Map<Integer, String> var2atom = new HashMap<>();

		QCIRProgramBuilder qcirPB = new QCIRProgramBuilder(QCIR_FORMAT);
		ArrayList<String> formulas = new ArrayList<>();
		for (int i = 0; i < qp.getPrograms().size(); i++) {
			Program current = qp.getPrograms().get(i);
			String quantifier = qp.getQuantifiers().get(i);

			// get predicates appearing in bodies, so that they will be subject to choice
			Set<String> predicatesInBodyPrevDef = new HashSet<>();
			for (ProgramPredicate pred : current.getPredicatesInBody()) {
				if (predicates.contains(pred.getName())) {
					predicatesInBodyPrevDef.add(pred.getName());
				}
			}

			// create program and add choice rules
			StringBuilder programWithChoiceSB = new StringBuilder(current.toString());
			for (GroundAtom atom : groundAtoms) {
				if (predicatesInBodyPrevDef.contains(atom.getPredicate())) {
					programWithChoiceSB.append("{" + atom.getAtom() + "}.");
				}
			}
			LOGGER.log(DEBUG_LEVEL, "\nprogram with choices: \n" + programWithChoiceSB);
			// get ground program, which will be converted to sat
			LParseProgram ground = getGroundProgram(programWithChoiceSB.toString());
			// add constraints to support strong negation
			// ground.addStrongNegationConstraints();
			LOGGER.log(DEBUG_LEVEL, quantifier);
			LOGGER.log(DEBUG_LEVEL, "\n" + ground.toString());

			// compute sat formula
			CNFProgram cnfProgram = ground2Sat(ground);

			LOGGER.log(DEBUG_LEVEL, "\nformula\n" + cnfProgram.toString());

			// remap variables and ids
			remapIdsAndAddNewAtoms(cnfProgram, atom2var, var2atom);

			LOGGER.log(DEBUG_LEVEL, "\nremapped formula\n" + cnfProgram.toString());

			// update groundAtoms set with new atoms
			for (String literal : ground.getLiteralToVar().keySet()) {
				String predicate = literal.split("\\(")[0];
				if (predicate.startsWith("-")) {
					predicate = predicate.substring(1);
				}
				groundAtoms.add(new GroundAtom(literal, predicate));
			}

			// update predicate set with new predicates
			for (ProgramPredicate pred : current.getPredicatesInHead()) {
				predicates.add(pred.getName());
			}

			// update qcir program
			formulas.add(updateQcirProgram(qcirPB, allVars, i, quantifier, cnfProgram));
		}
		if(!qp.getQuantifiers().get(qp.getQuantifiers().size()-1).equals(CONSTRAINT)) {
			System.err.println("Invalid input: the last quantifier must be "+CONSTRAINT+". Note that the constraint program can be empty though.");
			System.exit(-1);
		}
		// merge formulas together
		String previous = formulas.get(formulas.size() - 1);
		for (int i = qp.getPrograms().size() - 2; i >= 0; i--) {
			String current = formulas.get(i);
			String quantifier = qp.getQuantifiers().get(i);
			if (quantifier.equals(EXISTS)) {
				previous = qcirPB.addFormula(i, QCIRProgram.AND, Arrays.asList(current, previous));
			} else if (quantifier.equals(FORALL)) {
				previous = qcirPB.addFormula(i, QCIRProgram.OR, Arrays.asList("-" + current, previous));
			} else {
				System.err.println("Invalid quantifier order (constraint must be the last)");
				System.exit(-1);
			}
		}
		qcirPB.setOutput(previous);

		LOGGER.log(DEBUG_LEVEL, "\n" + qcirPB.getProgramString());
		return solveQCIRProgram(qcirPB.getProgram(), qcirPB, var2atom, qcirPB.litToVar);
	}

	private String updateQcirProgram(QCIRProgramBuilder qcirPB, Set<Integer> allVars, int i, String quantifier,
			CNFProgram cnfProgram) {

		qcirPB.startQuantifiedVars();
		qcirPB.startClausesList();
		for (List<Integer> clause : cnfProgram.getClauses()) {
			qcirPB.startClause();
			for (Integer var : clause) {
				int sign = Integer.signum(var);
				int unsignedVar = var * sign;
				boolean quantify = !allVars.contains(unsignedVar);
				if (quantify) {
					allVars.add(unsignedVar);
				}
				qcirPB.addLit(sign, unsignedVar, quantify);
			}
			qcirPB.endClause();
			qcirPB.onClauseGateDefinition(i, QCIRProgram.OR);
		}
		String formula = qcirPB.groupClauses(i);
		if (quantifier.equals(CONSTRAINT)) {
			qcirPB.addQuantifiedLayer(EXISTS.substring(1));
		} else {
			qcirPB.addQuantifiedLayer(quantifier.substring(1));
		}
		return formula;
	}

	private File writeQcirToTempFile(QCIRProgram qcirProgram) {
		return Utilities.writeToTempFile(QCIRProgramBuilder.getProgramString(qcirProgram));
	}

	private List<String> solveQCIRProgram(QCIRProgram qcirProgram) {
		File tempFile = writeQcirToTempFile(qcirProgram);
		return Utilities.executeBinaries(QCIR_SOLVER, tempFile.toString());
	}

	private QAspResult quabsOutputToRes(List<String> output, Map<Integer, String> var2atom,
			Map<Integer, Integer> qcirToDimacs) {
		String lastLine = output.get(output.size() - 1);
		boolean sat = lastLine.split(" ")[1].equals("SAT");
		QAspResult res = new QAspResult(sat);
		if (output.size() > 1) {
			// we have an assignment
			ArrayList<String> qaspAssignment = new ArrayList<>();
			ArrayList<String> qcirAssignment = new ArrayList<>();
			String[] outAssignment = output.get(0).split(" ");
			for (int i = 1; i < outAssignment.length - 1; i++) {
				int qcirLit = Integer.parseInt(outAssignment[i]);
				if (qcirLit > 0) {
					Integer var = qcirToDimacs.get(Math.abs(qcirLit));
					if (var != null && var > 0 && var2atom.get(var) != null) {
						String atom = var2atom.get(var);
						qaspAssignment.add(atom);
						//correct?
						qcirAssignment.add(qcirLit+"");
					}
				}
			}
			
			res.addQaspAssignment(qaspAssignment);
			res.addQcirAssignment(qcirAssignment);
		}
		return res;
	}

	private QAspResult solveQCIRProgram(QCIRProgram qcirProgram, QCIRProgramBuilder qcirPB, Map<Integer, String> var2atom,
			Map<Integer, Integer> qcirToDimacs) {

		List<String> output = solveQCIRProgram(qcirProgram);
		if (QCIR_SOLVER == RARE_QS_COMMAND_TEMPLATE) {
			String lastLine = output.get(output.size() - 1);
			int result = Integer.parseInt(lastLine.substring(lastLine.length() - 1));
			return new QAspResult(result == 1);
		} else if (QCIR_SOLVER == QUABS_COMMAND_TEMPLATE) {
			QAspResult currentRes = quabsOutputToRes(output, var2atom, qcirToDimacs);
			if (!currentRes.isSat()) {
				return new QAspResult(currentRes.isSat());
			}
			QAspResult res = new QAspResult(currentRes.isSat());
			int models = 0;
			while (currentRes.isSat() && (models < numberOfModels || numberOfModels == -1)) {

				List<String> qaspAssignment = currentRes.getQaspAssignments().get(0);
				res.addQaspAssignment(new ArrayList<>(qaspAssignment));
				// add constraint to change model
				qcirPB.addAssignmentConstraint(currentRes.getQcirAssignments().get(0));
				currentRes = quabsOutputToRes(solveQCIRProgram(qcirProgram), var2atom, qcirToDimacs);
				models++;
			}
			return res;

		}
		throw new IllegalArgumentException("Invalid solver " + Utilities.getCommand(QCIR_SOLVER));

	}

	private void remapIdsAndAddNewAtoms(CNFProgram cnfProgram, Map<String, Integer> atom2var,
			Map<Integer, String> var2atom) {

		Map<Integer, Integer> mappedVars = new HashMap<>();
		// assuming also unknown vars are mapped to null strings in var2atom
		for (ArrayList<Integer> clause : cnfProgram.getClauses()) {
			for (int i = 0; i < clause.size(); i++) {
				int varWithSign = clause.get(i);
				int sign = Integer.signum(varWithSign);
				int var = varWithSign * sign;
				if (!mappedVars.containsKey(var)) {
					// variable not already mapped
					String atom = cnfProgram.getVarToLiteral().get(var);
					if (atom == null) {
						// aux variable
						int mappedVar = var2atom.size() + 1;
						var2atom.put(mappedVar, null);
						mappedVars.put(var, mappedVar);
					} else {
						if (atom2var.containsKey(atom)) {
							// atom already exists
							int mappedVar = atom2var.get(atom);
							mappedVars.put(var, mappedVar);
						} else {
							// atom is new
							int mappedVar = var2atom.size() + 1;
							mappedVars.put(var, mappedVar);
							var2atom.put(mappedVar, atom);
							atom2var.put(atom, mappedVar);
						}
						LOGGER.log(DEBUG_LEVEL, "mapping lit " + atom + " to " + atom2var.get(atom));
					}
				}
				// set mapped value with sign
				clause.set(i, mappedVars.get(var) * sign);
			}
		}
	}

	private CNFProgram ground2Sat(LParseProgram ground) {
		File tempFile = Utilities.writeToTempFile(ground.toString());
		LOGGER.log(DEBUG_LEVEL, "transforming ground program to sat");
		// LOGGER.log(DEBUG_LEVEL, ground.toString());
		String command = "cat " + tempFile + " | %s | %s | %s | %s";
		List<String> satProgram = Utilities.executeBinaries(command, "lpshift-1.4", "lp2normal-2.27", "lp2lp2-1.23",
				"lp2sat-1.24");
		CNFProgramBuilder builder = new CNFProgramBuilder();
		LOGGER.log(DEBUG_LEVEL, "sat formula");
		for (String s : satProgram) {
			builder.addRow(s);
			LOGGER.log(DEBUG_LEVEL, s);
		}
		return builder.getProgram();

	}

	private LParseProgram getGroundProgram(String programString) {
		List<String> ground = ground(programString);
		LParseProgramBuilder lppb = new LParseProgramBuilder();
		for (String g : ground) {
			lppb.addRow(g);
		}
		return lppb.getProgram();
	}
}
