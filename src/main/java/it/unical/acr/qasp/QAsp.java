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
import java.util.logging.Level;
import java.util.logging.Logger;

import it.unical.mat.dlv.parser.Builder;
import it.unical.mat.dlv.parser.Director;
import it.unical.mat.dlv.parser.ParseException;
import it.unical.mat.dlv.program.Comment;
import it.unical.mat.dlv.program.Program;
import it.unical.mat.dlv.program.ProgramPredicate;
import it.unical.mat.dlv.program.SimpleProgramBuilder;

public class QAsp {

	public final static String EXISTS = "@exists";
	public final static String FORALL = "@forall";
	public final static String CONSTRAINT = "@constraint";

	private static final Logger LOGGER = Logger.getLogger(QAsp.class.getName());
	public static final Level DEBUG_LEVEL = Level.INFO;

	public List<String> ground(String program) {

		// TODO fix
		program = program.replaceAll(" v ", " | ");

		File tempFile = Utilities.writeToTempFile(program);
		// Run a shell command
		String command = "./gringo --output=SMODELS " + tempFile;

		return Utilities.runAndGetList(command);
	}

	private Program readAspProgram(String filename) throws FileNotFoundException, ParseException {
		File file = new File(filename);
		FileInputStream fis = new FileInputStream(file);
		Director d = new Director(fis);
		Builder b = new SimpleProgramBuilder();
		d.configureBuilder(b);
		d.start();
		Program p = (Program) b.getProductHandler();
		return p;
	}

	public QuantifiedProgram readQuantifiedProgram(String filename) {

		Program p = null;
		try {
			p = readAspProgram(filename);
		} catch (FileNotFoundException e) {
			LOGGER.severe("Unable to find file " + filename);
			System.exit(0);
		} catch (ParseException e) {
			LOGGER.severe("Unable to parse file " + filename);
			LOGGER.severe(e.toString());
			System.exit(0);
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
			}
			if (programs.size() == 0) {
				throw new RuntimeException("Unexpected error while parsing program, was expecting "
						+ validQuantifiersString() + " but got" + p.get(i));
			}
			programs.get(programs.size() - 1).add(p.get(i));
		}
		return new QuantifiedProgram(programs, quantifiers);

	}

	private String validQuantifiersString() {
		return FORALL + ", " + EXISTS + ", or " + CONSTRAINT;
	}

	public static void main(String[] args) {
		if (args.length != 1) {
			throw new IllegalArgumentException("Expected exactly one argument: a Q ASP input file");
		}
		boolean result = new QAsp().solve(args[0]);
		System.out.println(result ? "SAT" : "UNSAT");
	}

	private boolean solve(String programFile) {
		QuantifiedProgram qp = readQuantifiedProgram(programFile);

		Set<String> predicates = new HashSet<>();
		Set<GroundAtom> groundAtoms = new HashSet<>();
		Set<Integer> allVars = new HashSet<>();

		Map<String, Integer> atom2var = new HashMap<>();
		Map<Integer, String> var2atom = new HashMap<>();

		QCIRProgram qcirProgram = new QCIRProgram();

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
			List<String> vars = new ArrayList<>();
			int clauseCount = 0;
			for (List<Integer> clause : cnfProgram.getClauses()) {
				List<String> qcirClause = new ArrayList<>();
				for (Integer var : clause) {
					int sign = Integer.signum(var);
					int unsignedVar = var * sign;
					String prefix = sign > 0 ? "" : "-";
					qcirClause.add(prefix + "x" + unsignedVar);
					if (!allVars.contains(unsignedVar)) {
						vars.add("x" + unsignedVar);
						allVars.add(unsignedVar);
					}
				}
				qcirProgram.addGateDefinition(new GateDefinition("c" + i + "_" + clauseCount, "or", qcirClause));
				clauseCount++;
			}

			List<String> clausesList = new ArrayList<>();
			for (int j = 0; j < cnfProgram.getClauses().size(); j++) {
				clausesList.add("c" + i + "_" + j);
			}

			if (quantifier.equals(CONSTRAINT)) {
				qcirProgram.addGateDefinition(new GateDefinition("psi_" + i, "and", clausesList));
				qcirProgram.addQuantifiedLayer(vars, EXISTS.substring(1));
			} else {
				qcirProgram.addGateDefinition(new GateDefinition("phi_" + i, "and", clausesList));
				qcirProgram.addQuantifiedLayer(vars, quantifier.substring(1));
			}

		}

		// merge formulas together
		for (int i = qp.getPrograms().size() - 2; i >= 0; i--) {
			String current = "phi_" + i;
			String quantifier = qp.getQuantifiers().get(i);
			if (quantifier.equals(EXISTS)) {
				qcirProgram.addGateDefinition(
						new GateDefinition("psi_" + i, "and", Arrays.asList(current, "psi_" + (i + 1))));
			} else if (quantifier.equals(FORALL)) {
				qcirProgram.addGateDefinition(
						new GateDefinition("psi_" + i, "or", Arrays.asList("-" + current, "psi_" + (i + 1))));
			} else {
				throw new IllegalArgumentException("invalid quantifier order (constraint must be the last)");
			}
		}

		LOGGER.log(DEBUG_LEVEL, "\n" + qcirProgram.toString());
		return solveQCIRProgram(qcirProgram);

	}

	private boolean solveQCIRProgram(QCIRProgram qcirProgram) {
		File tempFile = Utilities.writeToTempFile(qcirProgram.toString());
		String command = "./rareqs-nn-beta.sh " + tempFile;
		List<String> output = Utilities.runAndGetList(command);
		String lastLine = output.get(output.size() - 1);
		int result = Integer.parseInt(lastLine.substring(lastLine.length() - 1));
		return result == 1;
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
		LOGGER.log(DEBUG_LEVEL, "ground program transofrming to sat");
		LOGGER.log(DEBUG_LEVEL, ground.toString());
		String command = "cat " + tempFile + " | ./lpshift-1.4 | ./lp2normal-2.27 | ./lp2lp2-1.23 | ./lp2sat-1.24";
		List<String> satProgram = Utilities.runAndGetList(command);
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
