package it.unical.acr.qasp;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import it.unical.mat.aspcore2.parser.AspCore2Parser;
import it.unical.mat.aspcore2.parser.ParseException;
import it.unical.mat.aspcore2.program.AspCore2Builder;
import it.unical.mat.aspcore2.program.AspCore2ProgramBuilder;
import it.unical.mat.dlv.program.Expression;
import it.unical.mat.dlv.program.Program;

public class Conversions {
	
	private static void addStringToProgram(AspCore2Builder builder, String p) throws FileNotFoundException, ParseException, UnsupportedEncodingException {
		AspCore2Parser director = new AspCore2Parser(new ByteArrayInputStream(p.getBytes("UTF-8")));
		director.configureBuilder(builder);
		director.start();
	}

	public static Program stringToProgram(String program) {
		AspCore2Builder builder = new AspCore2ProgramBuilder();
		try {
			addStringToProgram(builder, program);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (ParseException e) {
			e.printStackTrace();
			System.exit(-1);
		}		
		return (Program) builder.getProductHandler();
	}
	
	static boolean mergeExistsConstraint = true;

	public static AspQProgram qDimacsToAspQ(QDimacsProgram input, boolean useChoiceRules) {
		AspQProgram res = new AspQProgram();

		for (int i = 0; i < input.getQuantifiers().size(); i++) {
			String quantifier = input.getQuantifiers().get(i);
			ArrayList<Integer> quantifiedLayer = input.getQuantifiedLayers().get(i);

			res.quantifiers.add(quantifier.equals("a") ? QAsp.FORALL : QAsp.EXISTS);

			StringBuilder choices = new StringBuilder();
			if(!useChoiceRules) {
				for (int q : quantifiedLayer) {
					choices.append("x_" + q + " | nx_" + q + ".");
				}
			} else {
				choices.append("{");
				for (int j=0;j<quantifiedLayer.size();j++) {
					if(j>0) {
						choices.append(";");
					}
					choices.append("x_" + quantifiedLayer.get(j));
				}
				choices.append("}.");
			}
			res.getPrograms().add(stringToProgram(choices.toString()));
		}
		boolean mergingExists = mergeExistsConstraint && res.quantifiers.get(res.quantifiers.size()-1) == QAsp.EXISTS;
		if(!mergingExists) {
			res.quantifiers.add(QAsp.CONSTRAINT);
		} 
		StringBuilder clauses = new StringBuilder();
		for (ArrayList<Integer> clause : input.getClauses()) {
			clauses.append(":-");
			for (int i=0;i<clause.size();i++) {
				if(i>0) {
					clauses.append(", ");
				}
				int var = clause.get(i);
				clauses.append(var < 0 ? "x_" + Math.abs(var) : "not x_" + var);
			}
			clauses.append(".\n");
		}
		res.getPrograms().add(stringToProgram(clauses.toString()));
		
		if(mergingExists) {
			int lastIndex = res.programs.size()-1;
			for(Expression ex: res.programs.get(lastIndex)) {
				res.getPrograms().get(lastIndex-1).add(ex);
			}
			res.programs.remove(lastIndex);
			res.quantifiers.set(lastIndex-1, QAsp.CONSTRAINT);
		}
		return res;

	}

}
