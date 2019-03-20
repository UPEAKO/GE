import java.io.*;
import java.util.ArrayList;

public class Plot {
    private ArrayList< ArrayList<Point> > boundarys = new ArrayList<>();

    private void getBoundary() {
        File file = new File("/home/ubd/work/GE/CN-block-L1.dat");
        String s;
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            //文件头
            for (int i = 0; i < 4; i++) {
                bufferedReader.readLine();
            }
            ArrayList<Point> tempBoundary = new ArrayList<>();
            while ((s = bufferedReader.readLine()) != null) {
                //新地块
                if (s.equals(">")) {
                    boundarys.add(tempBoundary);
                    tempBoundary = new ArrayList<>();
                }
                else {
                    String[] ss = s.split(" ");
                    tempBoundary.add(new Point(Double.valueOf(ss[0]), Double.valueOf(ss[1])));
                }
            }
            boundarys.add(tempBoundary);
            bufferedReader.close();
            fileReader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        File file1 = new File("/home/ubd/work/GE/CN-block-L2.dat");
        try {
            FileReader fileReader = new FileReader(file1);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            //文件头
            for (int i = 0; i < 4; i++) {
                bufferedReader.readLine();
            }
            ArrayList<Point> tempBoundary = new ArrayList<>();
            while ((s = bufferedReader.readLine()) != null) {
                //新地块
                if (s.equals(">")) {
                    boundarys.add(tempBoundary);
                    tempBoundary = new ArrayList<>();
                }
                else {
                    String[] ss = s.split(" ");
                    tempBoundary.add(new Point(Double.valueOf(ss[0]), Double.valueOf(ss[1])));
                }
            }
            boundarys.add(tempBoundary);
            bufferedReader.close();
            fileReader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }


        File file2 = new File("/home/ubd/work/GE/CN-block-L1-deduced.dat");
        try {
            FileReader fileReader = new FileReader(file2);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            //文件头
            for (int i = 0; i < 4; i++) {
                bufferedReader.readLine();
            }
            ArrayList<Point> tempBoundary = new ArrayList<>();
            while ((s = bufferedReader.readLine()) != null) {
                //新地块
                if (s.equals(">")) {
                    boundarys.add(tempBoundary);
                    tempBoundary = new ArrayList<>();
                }
                else {
                    String[] ss = s.split(" ");
                    tempBoundary.add(new Point(Double.valueOf(ss[0]), Double.valueOf(ss[1])));
                }
            }
            boundarys.add(tempBoundary);
            bufferedReader.close();
            fileReader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createNew() {
        //暂时绘制边界
        File file = new File("/home/ubd/work/GE/CN-block-L1.kml");
        try {
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                    "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                    "<Document id=\"root_doc\">\n");

            for (int i = 0; i < boundarys.size(); i++) {
                ArrayList<Point> tempBoundarys = boundarys.get(i);
                StringBuilder temp = new StringBuilder("<Placemark>\n" +
                        "                                        <Style id=\"oval\">\n" +
                        "                                                <LineStyle>\n" +
                        "                                                        <color>ff0000ff</color>\n" +
                        "                                                        <width>2.0</width>\n" +
                        "                                                </LineStyle>\n" +
                        "                                                <PolyStyle>\n" +
                        "                                                        <fill>0</fill>\n" +
                        "                                                </PolyStyle>\n" +
                        "                                        </Style>\n" +
                        "                                        <Polygon>\n" +
                        "                                                <outerBoundaryIs>\n" +
                        "                                                        <LinearRing>\n" +
                        "                                                                <coordinates>\n");
                for (int j = 0; j < tempBoundarys.size(); j++) {
                    temp.append(tempBoundarys.get(j).x);
                    temp.append(',');
                    temp.append(tempBoundarys.get(j).y);
                    temp.append(' ');
                }
                temp.append("</coordinates>\n" +
                        "                                                        </LinearRing>\n" +
                        "                                                </outerBoundaryIs>\n" +
                        "                                        </Polygon>\n" +
                        "                                </Placemark>\n");
                bufferedWriter.write(temp.toString());
            }

            bufferedWriter.write("</Document>\n" +
                    "</kml>");
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Plot plot = new Plot();
        plot.getBoundary();
        plot.createNew();
    }
}
