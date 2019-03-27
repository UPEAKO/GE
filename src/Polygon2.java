import java.util.ArrayList;
import java.util.Vector;

public class Polygon2 {
    public String name;
    public ArrayList<Point> boundarys;
    public Vector<Info> points;
    Polygon2(String name, ArrayList<Point> boundarys, Vector<Info> points) {
        this.name = name;
        this.boundarys = boundarys;
        this.points = points;
    }
    Polygon2() {
        name = "";
        boundarys = new ArrayList<>();
        points = new Vector<>();
    }
}
