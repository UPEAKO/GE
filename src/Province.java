import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

class Province implements Serializable {
    String name;
    //边界
    ArrayList< ArrayList<Point> > boundarys;
    //GPS点相关数据
    transient Vector<Info> infos;
    private static final long serialVersionUID = -6849794470754667733L;
    Province () {
        name = "";
        boundarys = new ArrayList<>();
        infos = new Vector<>();
    }
}
