import java.io.Serializable;

public class Point implements Serializable {
    public double x;
    public double y;
    private static final long serialVersionUID = -6849794470754667734L;
    Point(double x, double y) {
        this.x = x;
        this.y = y;
    }
}
