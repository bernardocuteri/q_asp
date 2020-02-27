package it.unical.acr.qasp;

public class GroundAtom {

	private String atom;
	private String predicate;

	public GroundAtom(String atom, String predicate) {
		super();
		this.atom = atom;
		this.predicate = predicate;
	}

	public String getAtom() {
		return atom;
	}

	public void setAtom(String atom) {
		this.atom = atom;
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((atom == null) ? 0 : atom.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GroundAtom other = (GroundAtom) obj;
		if (atom == null) {
			if (other.atom != null)
				return false;
		} else if (!atom.equals(other.atom))
			return false;
		return true;
	}

}
