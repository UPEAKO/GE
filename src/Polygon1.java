import java.util.ArrayList;

public class Polygon1 {
    public String name;
    public ArrayList<Point> boundarys;
    public ArrayList<Polygon2> polygon2s;
    Polygon1(String name, ArrayList<Point> boundarys, ArrayList<Polygon2> polygon2s) {
        this.name = name;
        this.boundarys = boundarys;
        this.polygon2s = polygon2s;
    }
    Polygon1(){
        name = "";
        boundarys = new ArrayList<>();
        polygon2s = new ArrayList<>();
    }
}
