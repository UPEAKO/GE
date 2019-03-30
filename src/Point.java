import java.io.Serializable;

class Point implements Serializable {
    double x;
    double y;
    private static final long serialVersionUID = -6849794470754667734L;
    Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
}