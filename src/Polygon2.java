import java.util.ArrayList;
import java.util.Vector;

class Polygon2 {
    String name;
    ArrayList<Point> boundarys;
    Vector<Info> points;
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
