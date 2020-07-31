package it.unical.acr.qasp;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import it.unical.mat.dlv.parser.Builder;
import it.unical.mat.dlv.parser.Director;
import it.unical.mat.dlv.parser.ParseException;
import it.unical.mat.dlv.program.Program;
import it.unical.mat.dlv.program.SimpleProgramBuilder;

public class Conversions {

	public static Program stringToProgram(String program) {
		InputStream stream = new ByteArrayInputStream(program.toString().getBytes(StandardCharsets.UTF_8));
		Director d = new Director(stream);
		Builder b = new SimpleProgramBuilder();
		d.configureBuilder(b);
		try {
			d.start();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return (Program) b.getProductHandler();
	}

	public static AspQProgram qDimacsToAspQ(QDimacsProgram input) {
		AspQProgram res = new AspQProgram();

		for (int i = 0; i < input.getQuantifiers().size(); i++) {
			String quantifier = input.getQuantifiers().get(i);
			ArrayList<Integer> quantifiedLayer = input.getQuantifiedLayers().get(i);

			res.quantifiers.add(quantifier.equals("a") ? QAsp.FORALL : QAsp.EXISTS);

			StringBuilder choices = new StringBuilder();
			for (int q : quantifiedLayer) {
				choices.append("x_" + q + " | nx_" + q + ".");
			}
			res.getPrograms().add(stringToProgram(choices.toString()));
		}
		res.quantifiers.add(QAsp.CONSTRAINT);
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
		return res;

	}

}
