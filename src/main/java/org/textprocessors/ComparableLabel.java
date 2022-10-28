package org.textprocessors;

import edu.stanford.nlp.ling.CoreLabel;

public class ComparableLabel {
    private CoreLabel cl;
    public ComparableLabel (CoreLabel cl) {
        this.cl = cl;
    }

    public CoreLabel getCl() {
        return cl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof  ComparableLabel)) {
            return false;
        }
        CoreLabel cl2 = ((ComparableLabel) o).getCl();
        return this.cl.word().equals(cl2.word());
    }
    @Override
    public int hashCode() {
        return this.cl.word().hashCode();
    }
    @Override
    public String toString() {
        return this.cl.toString();
    }
}
