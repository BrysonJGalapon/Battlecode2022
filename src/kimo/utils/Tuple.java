package kimo.utils;

public class Tuple<X, Y> {
    private final X x;
    private final Y y;

    private Tuple(X x, Y y) {
        this.x = x;
        this.y = y;
    }

    public static <X, Y> Tuple<X, Y> of(X x, Y y) {
        return new Tuple<>(x, y);
    }

    public X getX() {
        return this.x;
    }

    public Y getY() {
        return this.y;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Tuple)) {
            return false;
        }

        Tuple otherTuple = (Tuple) other;
        return this.getX().equals(otherTuple.getX()) && this.getY().equals(otherTuple.getY());
    }

    @Override
    public int hashCode() {
        int ret = 0;
        ret = 37 * ret + this.getX().hashCode();
        ret = 37 * ret + this.getY().hashCode();
        return ret;
    }
}
