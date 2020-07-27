package it.unical.qasp;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import it.unical.mat.random.MainGenerator;

public class RandomizedTests {

	@Test
	public void executeRandomTests() {
		List<String> args = new ArrayList<>();
		//args.add("-h");
		args.add("-o=temp/random.asp");
		args.add("-generator=CIGenerator");
		args.add("-out=MultiOutput");
		args.add("-formats=PrintQBF,PrintQCIR");
		args.add("-E=10");
		args.add("-A=10");
		args.add("-e=3");
		args.add("-a=4");
		args.add("-c=5");
		args.add("-w=4");
		MainGenerator.main(args.toArray(new String[0]));
	}
}
