import java.io.*;
import java.util.ArrayList;

public class Plot {
    private ArrayList< ArrayList<Point> > boundarys = new ArrayList<>();
    private int block2 = 0;

    private void getBoundary() {
        File file = new File("E:\\work\\not_use_1\\GE\\CN-block-L1.dat");
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


        File file2 = new File("E:\\work\\not_use_1\\GE\\CN-block-L1-deduced.dat");
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

        File file1 = new File("E:\\work\\not_use_1\\GE\\CN-block-L2.dat");
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
                    block2++;
                    tempBoundary = new ArrayList<>();
                }
                else {
                    String[] ss = s.split(" ");
                    tempBoundary.add(new Point(Double.valueOf(ss[0]), Double.valueOf(ss[1])));
                }
            }
            boundarys.add(tempBoundary);
            block2++;
            bufferedReader.close();
            fileReader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createNew() {
        //暂时绘制边界
        File file = new File("E:\\work\\not_use_1\\GE\\CN-block.kml");
        try {
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            bufferedWriter.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                    "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                    "<Document id=\"root_doc\">\n");

            for (int i = 0; i < boundarys.size(); i++) {
                String color =  "ff0000ff";
                if (i >= (boundarys.size() - block2))
                    color = "ff00ff00";
                ArrayList<Point> tempBoundarys = boundarys.get(i);
                StringBuilder temp = new StringBuilder("<Folder>\n" +
                        "<name>" +
                        i +
                        "</name>\n" +
                        "<Placemark>\n" +
                        "\t<Style id=\"oval\">\n" +
                        "\t<LineStyle>\n" +
                        "\t\t<color>" +
                        color +
                        "</color>\n" +
                        "\t\t<width>2.0</width>\n" +
                        "\t</LineStyle>\n" +
                        "\t<PolyStyle>\n" +
                        "\t\t<fill>0</fill>\n" +
                        "\t</PolyStyle>\n" +
                        "\t</Style>\n" +
                        "\t<Polygon>\n" +
                        "\t\t<outerBoundaryIs>\n" +
                        "\t\t\t<LinearRing>\n" +
                        "\t\t\t\t<coordinates>\n");
                for (int j = 0; j < tempBoundarys.size(); j++) {
                    temp.append(tempBoundarys.get(j).x);
                    temp.append(',');
                    temp.append(tempBoundarys.get(j).y);
                    temp.append(' ');
                }
                temp.append("\n\t\t\t\t</coordinates>\n" +
                        "\t\t\t</LinearRing>\n" +
                        "\t\t </outerBoundaryIs>\n" +
                        "\t</Polygon>\n" +
                        "</Placemark>\n");

                for (int j = 0; j < tempBoundarys.size(); j++) {
                    temp.append("<Placemark>\n" +
                            "\t<name>");
                    temp.append(i);
                    temp.append('-');
                    temp.append(j);
                    temp.append("</name>\n" +
                            "\t<Point>\n" +
                            "\t\t<coordinates>\n");
                    temp.append(tempBoundarys.get(j).x);
                    temp.append(',');
                    temp.append(tempBoundarys.get(j).y);
                    temp.append("\n\t\t</coordinates>\n" +
                            "\t</Point>\n" +
                            "</Placemark>\n");
                }
                temp.append("</Folder>\n");
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
