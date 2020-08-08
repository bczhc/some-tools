package pers.zhc.u.math.util;

import pers.zhc.u.ComplexDefinite;

/**
 * @author bczhc
 */
@SuppressWarnings({"UnusedReturnValue", "unused"})
public class ComplexValue {
    public double re, im;

    public ComplexValue(double re, double im) {
        this.re = re;
        this.im = im;
    }

    public static void main(String[] args) {
        ComplexFunctionInterface f1 = t -> new ComplexValue(Math.cos(t), Math.sin(t)).selfMultiply(10, 0);
        double o = 2 * Math.PI / -50;
        ComplexFunctionInterface f = t -> new ComplexValue(0, 0).setValue(f1.x(t)).selfMultiply(
                Math.cos(0 * o * t)
                , Math.sin(0 * o * t)
        );
        final ComplexDefinite complexDefinite = new ComplexDefinite();
        complexDefinite.n = 10000;
        final ComplexValue r = complexDefinite.getDefiniteIntegralByTrapezium(0, 2 * Math.PI
                , f);
        System.out.println("r = " + r);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ComplexValue that = (ComplexValue) o;
        return that.re == this.re && that.im == this.im;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(re);
        result = (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(im);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public double getComplexModule() {
        return Math.sqrt(Math.pow(re, 2D) + Math.pow(im, 2D));
    }

    public ComplexValue add(ComplexValue cv) {
        return new ComplexValue(this.re + cv.re, this.im + cv.im);
    }

    public ComplexValue add(double re, double im) {
        return new ComplexValue(this.re + re, this.im + im);
    }

    public ComplexValue selfAdd(ComplexValue cv) {
        this.re += cv.re;
        this.im += cv.im;
        return this;
    }

    public ComplexValue selfAdd(double re, double im) {
        this.re += re;
        this.im += im;
        return this;
    }

    public ComplexValue subtract(ComplexValue cv) {
        return new ComplexValue(this.re - cv.re, this.im - cv.im);
    }

    public ComplexValue subtract(double re, double im) {
        return new ComplexValue(this.re - re, this.im - im);
    }

    public ComplexValue selfSubtract(ComplexValue cv) {
        this.re -= cv.re;
        this.im -= cv.im;
        return this;
    }

    public ComplexValue selfSubtract(double re, double im) {
        this.re -= re;
        this.im -= im;
        return this;
    }

    public ComplexValue multiply(ComplexValue cv) {
        return new ComplexValue(this.re * cv.re - this.im * cv.im, this.re * cv.im + cv.re * this.im);
    }

    public ComplexValue multiply(double re, double im) {
        return new ComplexValue(
                this.re * re - this.im * im
                , this.re * im + re * this.im
        );
    }

    public ComplexValue selfMultiply(ComplexValue cv) {
        final double re1 = this.re * cv.re - this.im * cv.im;
        final double im1 = this.re * cv.im + cv.re * this.im;
        this.re = re1;
        this.im = im1;
        return this;
    }

    public ComplexValue selfMultiply(double re, double im) {
        final double re1 = this.re * re - this.im * im;
        final double im1 = this.re * im + re * this.im;
        this.re = re1;
        this.im = im1;
        return this;
    }

    public ComplexValue divide(ComplexValue cv) {
        double a = Math.pow(cv.re, 2D) + Math.pow(cv.im, 2D);
        return new ComplexValue((this.re * cv.re + this.im * cv.im) / a, (cv.re * this.im - this.re * cv.im) / a);
    }

    public ComplexValue divide(double re, double im) {
        double a = Math.pow(re, 2D) + Math.pow(im, 2D);
        return new ComplexValue((this.re * re + this.im * im) / a, (re * this.im - this.re * im) / a);
    }

    public ComplexValue selfDivide(ComplexValue cv) {
        double a = Math.pow(cv.re, 2D) + Math.pow(cv.im, 2D);
        final double re1 = (this.re * cv.re + this.im * cv.im) / a;
        final double im1 = (cv.re * this.im - this.re * cv.im) / a;
        this.re = re1;
        this.im = im1;
        return this;
    }

    public ComplexValue selfDivide(double re, double im) {
        double a = Math.pow(re, 2D) + Math.pow(im, 2D);
        final double re1 = (this.re * re + this.im * im) / a;
        final double im1 = (re * this.im - this.re * im) / a;
        this.re = re1;
        this.im = im1;
        return this;
    }

    @Override
    public String toString() {
        if (this.re == 0 && this.im == 0) {
            return String.valueOf(0);
        } else if (this.re == 0) {
            return this.im + "i";
        } else if (this.im == 0) {
            return String.valueOf(re);
        } else if (im > 0) {
            return re + "+" + im + "i";
        } else if (im < 0) {
            return re + "" + im + "i";
        }
        return String.valueOf(0);
    }

    public ComplexValue setValue(ComplexValue cv) {
        this.re = cv.re;
        this.im = cv.im;
        return this;
    }

    public ComplexValue setValue(double re, double im) {
        this.re = re;
        this.im = im;
        return this;
    }
}