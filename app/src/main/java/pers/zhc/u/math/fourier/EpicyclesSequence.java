package pers.zhc.u.math.fourier;


import pers.zhc.u.math.util.ComplexValue;

import java.util.ArrayList;
import java.util.List;

public class EpicyclesSequence {
    public final List<Epicycle> epicycles;

    public EpicyclesSequence() {
        this.epicycles = new ArrayList<>();
    }

    public void put(double n, ComplexValue c) {
        this.epicycles.add(new Epicycle(n, c));
    }

    public void put(double n, double complexReP, double complexImP) {
        this.epicycles.add(new Epicycle(n, new ComplexValue(complexReP, complexImP)));
    }

    public void put(Epicycle aEpicycle) {
        this.epicycles.add(aEpicycle);
    }

    public static class Epicycle {

        public final double n;
        public final ComplexValue c;

        public Epicycle(double n, ComplexValue c) {
            this.n = n;
            this.c = c;
        }
    }
}