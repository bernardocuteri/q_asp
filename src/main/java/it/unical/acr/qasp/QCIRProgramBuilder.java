package it.unical.acr.qasp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import it.unical.acr.qasp.QCIRProgram.QCIRFormatType;

public class QCIRProgramBuilder {

	private QCIRProgram program;
	List<String> qcirClause = new ArrayList<>();
	List<String> quantifiedVars = new ArrayList<>();
	Set<String> vars = new HashSet<>();
	List<String> clausesList = new ArrayList<>();
	Map<Integer, Integer> varToLit = new HashMap<>();
	Map<Integer, Integer> litToVar = new HashMap<>();

	public QCIRProgramBuilder(QCIRFormatType format) {
		super();
		this.program = new QCIRProgram(format);
	}

	public QCIRProgram getProgram() {
		return program;
	}

	void addGateDefinition(GateDefinition gd) {
		program.getGateDefinitions().add(gd);
	}

	public String getProgramString() {
		return getProgramString(program);
	}

	public static String getProgramString(QCIRProgram program) {
		StringBuilder sb = new StringBuilder();
		// use QCIR version G14
		sb.append("#QCIR-G14\n");

		// append quantifications
		for (int i = 0; i < program.getQuantifiers().size(); i++) {
			sb.append(program.getQuantifiers().get(i) + "(");
			for (int j = 0; j < program.getQuantifiedVariables().get(i).size(); j++) {
				if (j > 0) {
					sb.append(",");
				}
				sb.append(program.getQuantifiedVariables().get(i).get(j));
			}

			sb.append(")\n");
		}

		sb.append("output(" + program.getOutput() + ")\n");

		for (GateDefinition gd : program.getGateDefinitions()) {
			sb.append(gd.toString() + "\n");
		}

		return sb.toString();
	}

	public void startClause() {
		qcirClause.clear();
	}

	public void endClause() {

	}

	private boolean isStandardFormat() {
		return program.getFormat() == QCIRFormatType.STANDARD;
	}

//	private boolean isCleansedFormat() {
//		return !isStandardFormat();
//	}

	private String freshVar() {
		String fresh = "" + (vars.size() + 1);
		vars.add(fresh);
		return fresh;
	}

	private String varToLit(int unsignedVar) {
		if (isStandardFormat()) {
			return "x" + unsignedVar;
		}
		Integer lit = varToLit.get(unsignedVar);
		if (lit != null) {
			return lit+"";
		}
		//System.out.println("mapping " + unsignedVar +" to "+(vars.size() + 1));
		varToLit.put(unsignedVar, vars.size() + 1);
		litToVar.put(vars.size() + 1, unsignedVar);
		return "" + (vars.size() + 1);
	}

	private String varToLit(int sign, int unsignedVar) {
		return (sign > 0 ? "" : "-") + varToLit(unsignedVar);
	}

	public void addLit(int sign, int unsignedVar, boolean quantify) {
		String lit = varToLit(sign, unsignedVar);
		if (quantify) {
			quantifiedVars.add(lit.startsWith("-") ? lit.substring(1) : lit);
		}
		vars.add(lit);
		qcirClause.add(lit);
	}

	public void startQuantifiedVars() {
		quantifiedVars.clear();
	}

	public void onClauseGateDefinition(int i, String operator) {
		String clauseVar = isStandardFormat() ? ("c" + i + "_" + clausesList.size()) : "" + (vars.size() + 1);
		vars.add(clauseVar);
		List<String> clauseCopy = new ArrayList<>(qcirClause);
		program.getGateDefinitions().add(new GateDefinition(clauseVar, operator, clauseCopy));
		clausesList.add(clauseVar);

	}

	public void addQuantifiedLayer(String quantifier) {
		List<String> quantifiedVarsCopy = new ArrayList<>(quantifiedVars);
		program.getQuantifiedVariables().add(quantifiedVarsCopy);
		program.getQuantifiers().add(quantifier);
	}

	private String clauseGroupToVar(int i) {
		String clauseVar = isStandardFormat() ? ("phi_" + i) : "" + (vars.size() + 1);
		vars.add(clauseVar);
		return clauseVar;
	}

	private void addClausesListGateDefinition(String var, String operator) {
		List<String> clausesListCopy = new ArrayList<>(clausesList);
		addGateDefinition(new GateDefinition(var, operator, clausesListCopy));
	}

	public String groupClauses(int i) {
		String var = clauseGroupToVar(i);
		addClausesListGateDefinition(var, QCIRProgram.AND);
		return var;
	}

	public void startClausesList() {
		clausesList.clear();
	}

	public void groupAndOutputClauses(int i) {
		String groupVar = clauseGroupToVar(i);
		addClausesListGateDefinition(groupVar, QCIRProgram.AND);
		program.setOutput(groupVar);

	}

	public void setOutput(String out) {
		program.setOutput(out);

	}

	public String addFormula(int i, String operator, List<String> list) {
		String f = isStandardFormat() ? "psi_" + i : freshVar();
		addGateDefinition(new GateDefinition(f, operator, list));
		return f;
	}

	private String negate(String lit) {
		return lit.startsWith("-") ? lit.substring(1) : "-"+lit;
	}
	
	public void addAssignmentConstraint(List<String> qcirAssignment) {
		List<String> flippedAssignment = qcirAssignment.stream().map(lit -> negate(lit)).collect(Collectors.toList());;
		String f = freshVar();
		addGateDefinition(new GateDefinition(f, QCIRProgram.OR, flippedAssignment));
		String newOut = freshVar();
		addGateDefinition(new GateDefinition(newOut, QCIRProgram.AND, Arrays.asList(f, program.getOutput())));
		program.setOutput(newOut);
		
	}
}
