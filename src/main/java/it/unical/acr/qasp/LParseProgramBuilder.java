package it.unical.acr.qasp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LParseProgramBuilder {

	private interface BuilderState {
		BuilderState addRow(String [] row);
	}

	private class DoneState implements BuilderState {

		@Override
		public BuilderState addRow(String [] row) {
			return this;
		}

	}
	
	private class VarTableState implements BuilderState {

		@Override
		public BuilderState addRow(String [] row) {
			if(row[0].equals("0")) {
				return new DoneState();
			}
			int var = Integer.parseInt(row[0]);
			String lit = row[1];
			program.getLiteralToVar().put(lit, var);
			program.getVarToLiteral().put(var, lit);
			return this;
		}

	}

	private class FormulaState implements BuilderState {

		@Override
		public BuilderState addRow(String [] row) {
			int type = Integer.parseInt(row[0]);
			switch(type) {
			case 0:
				return new VarTableState();
			}
			ArrayList<Integer> rule = new ArrayList<>();
			for(String lit: row) {
				rule.add(Integer.parseInt(lit));
			}
			program.addRule(rule);
			return this;
		}

	}

	private BuilderState builderState = new FormulaState();


	private LParseProgram program = new LParseProgram();

	public final int DISJUNCTION = 8;
	public final int WEAK = 6;
	public final int CHOICE = 3;
	public final int CONSTRAINT = 5;
	public final int NORMAL = 1;
	

	Map<String, Integer> literalToVar = new HashMap<>();
	Map<Integer, String> varToLiteral = new HashMap<>();

	public LParseProgram getProgram() {
		return program;
	}

	public void addRow(String row) {
		builderState = builderState.addRow(row.split(" "));
	}

}
