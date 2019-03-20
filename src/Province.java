import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

public class Province implements Serializable {
    public String name;
    //边界
    public ArrayList< ArrayList<Point> > boundarys;
    //GPS点相关数据
    public transient Vector<Info> infos;
    private static final long serialVersionUID = -6849794470754667733L;
    Province () {
        name = "";
        boundarys = new ArrayList<>();
        infos = new Vector<>();
    }
}
