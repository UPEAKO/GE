import java.io.*;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 注意这里的颜色为aabbggrr
 */
public class GE {
    //存储每一个省的数据
    private Vector<Province> provinces = new Vector<>();

    //内置Label
    private static String [] innerLables = {
            "http://104.224.134.83/other/cug.png",
            "http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png",
            "http://maps.google.com/mapfiles/kml/shapes/target.png",
            "http://maps.google.com/mapfiles/kml/shapes/shaded_dot.png"
    };


    //读取边界数据
    private void getBoundary() {
        File file = new File("china.kml");
        String s, ss;
        Pattern pattern = Pattern.compile("<SimpleData name=\"NAME_1\">(.+?)</SimpleData>");
        Pattern pattern1 = Pattern.compile("<LinearRing><coordinates>(.+?)</coordinates></LinearRing>");
        Matcher matcher, matcher1;
        boolean isName = true;
        Province tempProvince = new Province();
        boolean isWrite = false;
        Vector<Point> tempBoundary = new Vector<>();
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            while ((s = bufferedReader.readLine()) != null) {
                if (isName) {
                    matcher = pattern.matcher(s);
                    if (matcher.find()) {
                        tempProvince.name = matcher.group(1);
                        isName = false;
                    }
                } else {
                    matcher1 = pattern1.matcher(s);
                    while (matcher1.find()) {
                        isWrite = true;
                        ss = matcher1.group(1);
                        String [] Points = ss.split(" ");
                        for (String each_point : Points) {
                            String [] XY = each_point.split(",");
                            tempBoundary.add(new Point(Double.valueOf(XY[0]),Double.valueOf(XY[1])));
                        }
                        tempProvince.boundarys.add(tempBoundary);
                        tempBoundary = new Vector<>();
                        isName = true;
                    }
                    if (isWrite) {
                        isWrite = false;
                        provinces.add(tempProvince);
                        tempProvince = new Province();
                    }
                }
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //对一个点进行分类
    private int segment(double L1, double B1) {
        //省级
        for (int i = 0; i < provinces.size(); i++) {
            //包括岛
            Province tempProvince = provinces.elementAt(i);
            //boolean signIOver = false;
            for (int j = 0; j < tempProvince.boundarys.size(); j++) {
                Vector<Point> tempBoundary = tempProvince.boundarys.elementAt(j);
                //取前两点中点
                double L1_known = tempBoundary.elementAt(0).x;
                double B1_known = tempBoundary.elementAt(0).y;
                double L2_known = tempBoundary.elementAt(1).x;
                double B2_known = tempBoundary.elementAt(1).y;
                double L2 = (L1_known + L2_known) / 2;
                double B2 = (B1_known + B2_known) / 2;
                double vecBaseX = L2 - L1;
                double vecBaseY = B2 - B1;
                int num = 1;
                //k不存在
                if(Math.abs(L2 - L1) < 1.0E-20) {
                    for (int k = 1; k < tempBoundary.size() - 1; k++) {
                        if ((tempBoundary.elementAt(k).x - L1) * (tempBoundary.elementAt(k + 1).x - L1) < 0 &&
                                (tempBoundary.elementAt(k).x > B1 | tempBoundary.elementAt(k+1).x > B1)) {
                            num++;
                        }
                    }
                }
                //k = 0
                else if(Math.abs(B2 - B1) < 1.0E-20) {
                    for (int k = 1; k < tempBoundary.size() - 1; k++) {
                        if ((tempBoundary.elementAt(k).y - B1) * (tempBoundary.elementAt(k + 1).y - B1) < 0 &&
                                (tempBoundary.elementAt(k).y > L1 | tempBoundary.elementAt(k+1).y > L1)) {
                            num++;
                        }
                    }
                }
                else {
                    double A = B1 - B2;
                    double B = L2 - L1;
                    double C = L1 * B2 - L2 * B1;
                    for (int k = 1; k < tempBoundary.size() - 1; k++) {
                        //以当前线段为base直线判断
                        boolean isFront = false;
                        double tX1 = tempBoundary.elementAt(k).x;
                        double tY1 = tempBoundary.elementAt(k).y;
                        double tX2 = tempBoundary.elementAt(k + 1).x;
                        double tY2 = tempBoundary.elementAt(k + 1).y;
                        double tA = tY1 - tY2;
                        double tB = tX2 - tX1;
                        double tC = tX1 * tY2 - tX2 * tY1;
                        double signTwoSide = (A * tX1 + B * tY1 + C) *
                                (A * tX2 + B * tY2 + C);
                        if (Math.abs(tX1 - tX2) <  1.0E-20) {
                            if ((L2 - L1) * (tX1 - L1) > 0)
                                isFront = true;
                        }
                        else if (Math.abs(tY1 - tY2) < 1.0E-20) {
                            if ((B2 - B1) * (tY1 - B1) > 0)
                                isFront = true;
                        } else {
                            double tK1 = (tY2 - tY1) / (tX2 - tX1);
                            double tK2 = -1 / tK1;
                            double b = B1 - tK2 * L1;
                            double verticalX = - (tB * b + tC) / (tA + tB * tK2);
                            double verticalY = tK2 * verticalX + b;
                            double vecVerX = verticalX - L1;
                            double vecVerY = verticalY - B1;
                            if (vecBaseX * vecVerX + vecBaseY * vecVerY > 0)
                                isFront = true;
                        }
                        if (signTwoSide < 0 && isFront) {
                            num++;
                        }
                    }
                }
                if (num % 2 != 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private String oval(double L2,double B2,double sigVe,double sigVn,double sigVen,double signNum,String baseColor) {
        //误差椭圆
        ArrayList<Point> ovalPoints = new ArrayList<>();
        int NUM = 50;
        //右上完整
        double step = sigVe / NUM;
        double current = -step;
        for (int i = 0; i < NUM ; i++) {
            current += step;
            //double currentX = current * signNum / Math.cos(B2 * Math.PI / 180);
            //double currentY = sigVn * Math.sqrt(1 - (current * current) / (sigVe * sigVe)) * signNum;
            double currentX = current;
            double currentY = sigVn * Math.sqrt(1 - (current * current) / (sigVe * sigVe));
            ovalPoints.add(new Point(currentX,currentY));
        }
        ovalPoints.add(new Point(sigVe,0));
        StringBuilder oval = new StringBuilder("\t\t\t\t<Placemark>\n" +
                "\t\t\t\t\t<Style id=\"oval\">\n" +
                "\t\t\t\t\t\t<LineStyle>\n" +
                baseColor +
                "\t\t\t\t\t\t\t<width>0.5</width>\n" +
                "\t\t\t\t\t\t</LineStyle>\n" +
                "\t\t\t\t\t\t<PolyStyle>\n" +
                "\t\t\t\t\t\t\t<fill>0</fill>\n" +
                "\t\t\t\t\t\t</PolyStyle>\n" +
                "\t\t\t\t\t</Style>\n" +
                "\t\t\t\t\t<Polygon>\n" +
                "\t\t\t\t\t\t<outerBoundaryIs>\n" +
                "\t\t\t\t\t\t\t<LinearRing>\n" +
                "\t\t\t\t\t\t\t\t<coordinates>\n" +
                "\t\t\t\t\t\t\t\t");

        //协方差对应旋转的角度
        double K = Math.sqrt((sigVe - sigVn) * (sigVe - sigVn) + 4 * sigVen * sigVen);
        double Qee = (sigVe + sigVn + K) / 2;
        double alpha = 0;
        if (sigVen > 0)
            alpha = Math.atan((Qee - sigVe) / sigVen);

        double X,Y;

        //右上
        for (int i = 0; i < NUM; i++) {
            double tempX = ovalPoints.get(i).x;
            double tempY = ovalPoints.get(i).y;
            if (sigVen > 0) {
                X = L2 + (tempX * Math.cos(alpha) - tempY * Math.sin(alpha)) * signNum / Math.cos(B2 * Math.PI / 180);
                Y = B2 + (tempX * Math.sin(alpha) + tempY * Math.cos(alpha)) * signNum;
            } else {
                X = L2 + (tempX * Math.cos(alpha) - tempY * Math.sin(alpha)) * signNum / Math.cos(B2 * Math.PI / 180);
                Y = B2 + (tempX * Math.sin(alpha) + tempY * Math.cos(alpha)) * signNum;
            }
            oval.append(X);
            oval.append(',');
            oval.append(Y);
            oval.append(' ');
        }
        //右下
        for (int i = NUM; i > 0; i--) {
            double tempX = ovalPoints.get(i).x;
            double tempY = -ovalPoints.get(i).y;
            if (sigVen > 0) {
                X = L2 + (tempX * Math.cos(alpha) - tempY * Math.sin(alpha)) * signNum / Math.cos(B2 * Math.PI / 180);
                Y = B2 + (tempX * Math.sin(alpha) + tempY * Math.cos(alpha)) * signNum;
            } else {
                X = L2 + (tempX * Math.cos(alpha) - tempY * Math.sin(alpha)) * signNum / Math.cos(B2 * Math.PI / 180);
                Y = B2 + (tempX * Math.sin(alpha) + tempY * Math.cos(alpha)) * signNum;
            }
            oval.append(X);
            oval.append(',');
            oval.append(Y);
            oval.append(' ');
        }
        //左下
        for (int i = 0; i < NUM; i++) {
            double tempX = -ovalPoints.get(i).x;
            double tempY = -ovalPoints.get(i).y;
            if (sigVen > 0) {
                X = L2 + (tempX * Math.cos(alpha) - tempY * Math.sin(alpha)) * signNum / Math.cos(B2 * Math.PI / 180);
                Y = B2 + (tempX * Math.sin(alpha) + tempY * Math.cos(alpha)) * signNum;
            } else {
                X = L2 + (tempX * Math.cos(alpha) - tempY * Math.sin(alpha)) * signNum / Math.cos(B2 * Math.PI / 180);
                Y = B2 + (tempX * Math.sin(alpha) + tempY * Math.cos(alpha)) * signNum;
            }
            oval.append(X);
            oval.append(',');
            oval.append(Y);
            oval.append(' ');
        }
        //左上
        for (int i = NUM; i >= 0; i--) {
            double tempX = -ovalPoints.get(i).x;
            double tempY = ovalPoints.get(i).y;
            if (sigVen > 0) {
                X = L2 + (tempX * Math.cos(alpha) - tempY * Math.sin(alpha)) * signNum / Math.cos(B2 * Math.PI / 180);
                Y = B2 + (tempX * Math.sin(alpha) + tempY * Math.cos(alpha)) * signNum;
            } else {
                X = L2 + (tempX * Math.cos(alpha) - tempY * Math.sin(alpha)) * signNum / Math.cos(B2 * Math.PI / 180);
                Y = B2 + (tempX * Math.sin(alpha) + tempY * Math.cos(alpha)) * signNum;
            }
            oval.append(X);
            oval.append(',');
            oval.append(Y);
            oval.append(' ');
        }

        oval.append("\n\t\t\t\t\t\t\t\t</coordinates>\n" +
                "\t\t\t\t\t\t\t</LinearRing>\n" +
                "\t\t\t\t\t\t</outerBoundaryIs>\n" +
                "\t\t\t\t\t</Polygon>\n" +
                "\t\t\t\t</Placemark>\n");
        return oval.toString();
    }

    private void write(double L1, double B1, double Ve, double Vn,double sigVe,double sigVn, double sigVen,String ID,
                       BufferedWriter bufferedWriter,boolean changeColor,String color,boolean changeLabel,boolean isInner,String labelUrl) {
        //另一端点处经度度增加值 1:100万 1度->111km
        double signNum = (1000 * Math.PI) / (111 * 180);
        //另一端点处 L B
        double L2 = L1 + Ve * signNum / Math.cos(B1 * Math.PI / 180);
        double B2 = B1 + Vn * signNum;
        //所需两点的基点（过渡点）
        double multi = 0.9;
        double tempL = L1 + Ve * signNum / Math.cos(B1 * Math.PI / 180) * multi;
        double tempB = B1 + Vn * signNum * multi;
        //所需两点
        double multi1 = 0.1;
        double arrowL1 = tempL + signNum * Vn * multi1 / Math.cos(tempB * Math.PI / 180);
        double arrowB1 = tempB - signNum * Ve * multi1;
        double arrowL2 = tempL - signNum * Vn * multi1 / Math.cos(tempB * Math.PI / 180);
        double arrowB2 = tempB + signNum * Ve * multi1;

        //颜色与label预处理
        String basecolor = "\t\t\t\t\t\t\t<color>ff0000ff</color>\n";
        String baseurl = "\t\t\t\t\t\t\t\t<href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png</href>\n";
        if (changeColor)
            basecolor = basecolor.replaceAll("<color>\\w{6,8}</color>","<color>" + color + "</color>");
        if (changeLabel) {
            if (isInner) {
                int location = Integer.valueOf(labelUrl);
                if (location > innerLables.length | location < 1) {
                    System.out.println("无此内置Label!");
                    System.exit(1);
                } else {
                    baseurl = baseurl.replaceAll("<href>.+</href>",innerLables[location]);
                }
            } else {
                baseurl = baseurl.replaceAll("<href>.+</href>","<href>" + labelUrl + "</href>");
            }
        }


        try {
            bufferedWriter.write("\t\t\t<Folder id=\"arrow\">\n" + "\t\t\t\t<name>" + ID + "</name>\n" +
                    "\t\t\t\t<description>" + ID + "</description>\n");
            //起点图标
            bufferedWriter.write("\t\t\t\t<Placemark>\n" +
                    "\t\t\t\t\t<Style id=\"point\">\n" +
                    "\t\t\t\t\t\t<IconStyle>\n" +
                    "\t\t\t\t\t\t\t<Icon>\n" +
                    baseurl +
                    "\t\t\t\t\t\t\t</Icon>\n" +
                    "\t\t\t\t\t\t</IconStyle>\n" +
                    "\t\t\t\t\t</Style>\n" +
                    "\t\t\t\t\t<Point>\n" +
                    "\t\t\t\t\t\t<coordinates>\n");
            bufferedWriter.write("\t\t\t\t\t\t" + Double.toString(L1) + "," + Double.toString(B1) +"\n");
            bufferedWriter.write("\t\t\t\t\t\t</coordinates>\n" +
                    "\t\t\t\t\t</Point>\n" +
                    "\t\t\t\t</Placemark>\n");
            //线段
            bufferedWriter.write("\t\t\t\t<Placemark>\n" +
                    "\t\t\t\t\t<Style id=\"arrow\">\n" +
                    "\t\t\t\t\t\t<LineStyle>\n" +
                    basecolor +
                    "\t\t\t\t\t\t\t<width>0.5</width>\n" +
                    "\t\t\t\t\t\t</LineStyle>\n" +
                    "\t\t\t\t\t</Style>\n"  +
                    "\t\t\t\t\t<LinearRing>\n" +
                    "\t\t\t\t\t\t<coordinates>\n");
            String line = "\t\t\t\t\t\t\t" + Double.toString(L1) + "," + Double.toString(B1) + " " +
                    Double.toString(L2) + "," + Double.toString(B2) + "\n";
            bufferedWriter.write(line);
            bufferedWriter.write("\t\t\t\t\t\t</coordinates>\n" +
                    "\t\t\t\t\t</LinearRing>\n" +
                    "\t\t\t\t</Placemark>\n" +
                    "\t\t\t\t<Placemark>\n" +
                    "\t\t\t\t\t<Style id=\"arrow\">\n" +
                    "\t\t\t\t\t\t<LineStyle>\n" +
                    basecolor +
                    "\t\t\t\t\t\t\t<width>0.5</width>\n" +
                    "\t\t\t\t\t\t</LineStyle>\n" +
                    "\t\t\t\t\t</Style>\n"  +
                    "\t\t\t\t\t<LinearRing>\n" +
                    "\t\t\t\t\t\t<coordinates>\n");
            //箭头
            String arrow = "\t\t\t\t\t\t\t" + Double.toString(arrowL1) + "," + Double.toString(arrowB1) + " " +
                    Double.toString(L2) + "," + Double.toString(B2) + " " +
                    Double.toString(arrowL2) + "," + Double.toString(arrowB2) + "\n";
            bufferedWriter.write(arrow);
            bufferedWriter.write("\t\t\t\t\t\t</coordinates>\n" +
                    "\t\t\t\t\t</LinearRing>\n" +
                    "\t\t\t\t</Placemark>\n");
            //误差椭圆
            bufferedWriter.write(oval(L2,B2,sigVe,sigVn,sigVen,signNum,basecolor));
            bufferedWriter.write("\t\t\t</Folder>\n");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void createNew(String pointFile,String newFile,boolean changeColor,String color,boolean changeLabel,boolean isInner,String labelUrl) {
        String s, ss;
        Pattern pattern = Pattern.compile("<SimpleData name=\"NAME_1\">(.+?)</SimpleData>");
        Pattern pattern1 = Pattern.compile("<LinearRing><coordinates>(.+?)</coordinates></LinearRing>");
        Pattern patternDocument = Pattern.compile("</Document>");
        Matcher matcher, matcher1, matcherDocument;
        boolean isName = true;
        Province tempProvince = new Province();
        boolean isWrite = false;
        Vector<Point> tempBoundary = new Vector<>();
        try {
            //1.读取边界并将原数据添加到目标kml
            FileReader fileReader = new FileReader("china.kml");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            FileWriter fileWriter = new FileWriter(newFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            while ((s = bufferedReader.readLine()) != null) {
                //每个placemark一个style标签，便于修改；未到</Document>，每一行都写入newFile
                matcherDocument = patternDocument.matcher(s);
                if (!matcherDocument.find()) {
                    bufferedWriter.write(s + "\n");
                }
                if (isName) {
                    matcher = pattern.matcher(s);
                    if (matcher.find()) {
                        tempProvince.name = matcher.group(1);
                        isName = false;
                    }
                } else {
                    matcher1 = pattern1.matcher(s);
                    while (matcher1.find()) {
                        isWrite = true;
                        ss = matcher1.group(1);
                        String [] Points = ss.split(" ");
                        for (String each_point : Points) {
                            String [] XY = each_point.split(",");
                            tempBoundary.add(new Point(Double.valueOf(XY[0]),Double.valueOf(XY[1])));
                        }
                        tempProvince.boundarys.add(tempBoundary);
                        tempBoundary = new Vector<>();
                        isName = true;
                    }
                    if (isWrite) {
                        isWrite = false;
                        provinces.add(tempProvince);
                        tempProvince = new Province();
                    }
                }
            }
            bufferedReader.close();
            fileReader.close();
            //2.读取GPS点
            fileReader = new FileReader(pointFile);
            bufferedReader = new BufferedReader(fileReader);
            String sp;
            //读一个处理一个
            while ((sp = bufferedReader.readLine()) != null) {
                String []info = sp.split(" ");
                double L = Double.valueOf(info[0]);
                double B = Double.valueOf(info[1]);
                double Ve = Double.valueOf(info[2]);
                double Vn = Double.valueOf(info[3]);
                double sigVe = Double.valueOf(info[4]);
                double sigVn = Double.valueOf(info[5]);
                //info[6]协方差暂时忽略
                double sigVen = Double.valueOf(info[6]);
                String ID = info[7];
                int location = segment(L,B);
                if (location >= 0)
                    provinces.elementAt(location).infos.add(new Info(L,B,Ve,Vn,sigVe,sigVn,sigVen,ID));
                else {
                    System.out.println(L + ", " + B + ": 分类发生错误！");
                }
            }
            bufferedReader.close();
            fileReader.close();
            //添加徽标
            bufferedWriter.write("\t\t<Folder>\n" +
                    "\t\t\t<name>LABEL</name>\n" +
                    "\t\t\t<ScreenOverlay id=\"label\">\n" +
                    "\t\t\t\t<name>label</name>\n" +
                    "\t\t\t\t<Icon>\n" +
                    "\t\t\t\t\t<href>http://104.224.134.83/other/sign.jpg</href>\n" +
                    "\t\t\t\t</Icon>\n" +
                    "\t\t\t\t<overlayXY x=\"0.0\" y=\"0.5\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
                    "\t\t\t\t<screenXY x=\"0.0\" y=\"0.5\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
                    "\t\t\t\t<size x=\"50\" y=\"50\" xunits=\"pixels\" yunits=\"pixels\"/>\n" +
                    "\t\t\t</ScreenOverlay>\n" +
                    "\t\t</Folder>\n");
            //3.写文件
            for (int i = 0; i < provinces.size(); i++) {
                if (provinces.elementAt(i).infos.size() > 0) {
                    bufferedWriter.write("\t\t<Folder>\n");
                    bufferedWriter.write("\t\t\t<name>" + provinces.elementAt(i).name + "</name>\n");
                    for (int j = 0; j < provinces.elementAt(i).infos.size(); j++) {
                        double L1 = provinces.elementAt(i).infos.elementAt(j).L;
                        double B1 = provinces.elementAt(i).infos.elementAt(j).B;
                        double Ve = provinces.elementAt(i).infos.elementAt(j).Ve;
                        double Vn = provinces.elementAt(i).infos.elementAt(j).Vn;
                        double sigVe = provinces.elementAt(i).infos.elementAt(j).sigVe;
                        double sigVn = provinces.elementAt(i).infos.elementAt(j).sigVn;
                        double sigVen = provinces.elementAt(i).infos.elementAt(j).sigVen;
                        String ID = provinces.elementAt(i).infos.elementAt(j).ID;
                        write(L1,B1,Ve,Vn,sigVe,sigVn,sigVen,ID,bufferedWriter,changeColor,color,changeLabel,isInner,labelUrl);
                    }
                    bufferedWriter.write("\t\t</Folder>\n");
                }
            }
            bufferedWriter.write("\t</Document>\n</kml>\n");
            System.out.println("处理完成！");
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addOnNew(double L, double B,double Ve,double Vn,double sigVe,double sigVn,double sigVen,String ID, String oldFile, String newFile) {
        /*
        //provinces序列化测试
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream("E:/work/not_use_1/GE/provices.data"));
            objectOutputStream.writeObject(provinces);
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //provinces反序列化测试
        try {
            ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream("E:/work/not_use_1/GE/provices.data"));
            System.out.println("开始反序列化");
            provinces = (Vector<Province>) objectInputStream.readObject();
            System.out.println("反序列化完成");
            objectInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        */
        getBoundary();
        int location = segment(L,B);
        boolean hasNotThisPro = true;
        String oldS = "";
        if (location >= 0) {
            String signForAdd = "<name>" + provinces.elementAt(location).name + "</name>";
            Pattern pattern = Pattern.compile(signForAdd);
            Matcher matcher;
            Pattern pattern1 = Pattern.compile("</Folder>");
            Matcher matcher1;
            Pattern pattern2 = Pattern.compile("</Document>");
            Matcher matcher2;
            String s;
            try {
                FileReader fileReader = new FileReader(oldFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                FileWriter fileWriter = new FileWriter(newFile);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                while ((s = bufferedReader.readLine()) != null) {
                    matcher = pattern.matcher(s);
                    if (matcher.find()) {
                        bufferedWriter.write(s + "\n");
                        write(L,B,Ve,Vn,sigVe,sigVn,sigVen,ID,bufferedWriter,false,"",false,true,"");
                        hasNotThisPro = false;
                    } else {
                        matcher1 = pattern1.matcher(oldS);
                        matcher2 = pattern2.matcher(s);
                        if (matcher1.find() && matcher2.find() && hasNotThisPro) {
                            bufferedWriter.write("\t\t<Folder>\n" +
                                    "\t\t\t<name>" + provinces.elementAt(location).name + "</name>\n");
                            write(L,B,Ve,Vn,sigVe,sigVn,sigVen,ID,bufferedWriter,false,"",false,true,"");
                            bufferedWriter.write("\t\t</Folder>\n" +
                                    "\t</Document>\n" +
                                    "</kml>\n");
                            break;
                        }
                        else
                            bufferedWriter.write(s + "\n");
                    }
                    oldS = s;
                }
                bufferedReader.close();
                bufferedWriter.close();
                fileReader.close();
                fileWriter.close();
                System.out.println("添加成功！");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void addOnOld(double L, double B,double Ve,double Vn,double sigVe,double sigVn,double sigVen,String ID, String oldFile) {
        String tempFile = oldFile+".temp";
        addOnNew(L,B,Ve,Vn,sigVe,sigVn,sigVen,ID,oldFile,tempFile);
        if (new File(oldFile).delete() && new File(tempFile).renameTo(new File(oldFile))) {
            System.out.println("添加成功！");
        }
    }

    private void delete(String L,String B,String oldFile,String newFile,boolean isNew) {
        //每次将一个完整的箭头存储起来，当符合条件，不写入新文件，否则写入新文件
        String sign = L + "," + B;
        String s;
        ArrayList<String> tempArrow = new ArrayList<>();
        Pattern pattern = Pattern.compile("<Folder id=\"arrow\">");
        Pattern pattern1 = Pattern.compile("</Folder>");
        Pattern pattern2 = Pattern.compile(sign);
        Matcher matcher,matcher1,matcher2;
        boolean continusAdd = false;
        boolean store = false;
        try {
            FileReader fileReader = new FileReader(oldFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String tempFile = oldFile + ".temp";
            if (isNew) {
                tempFile = newFile;
            }
            FileWriter fileWriter = new FileWriter(tempFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            while ((s = bufferedReader.readLine()) != null) {
                matcher = pattern.matcher(s);
                matcher1 = pattern1.matcher(s);
                if (matcher.find()) {
                    tempArrow.add(s + "\n");
                    continusAdd = true;
                    store = true;
                } else if (matcher1.find() && continusAdd) {
                    tempArrow.add(s + "\n");
                    continusAdd = false;
                    if (store) {
                        for (String each:tempArrow) {
                            bufferedWriter.write(each);
                        }
                    }
                    tempArrow.clear();
                } else if (continusAdd){
                    tempArrow.add(s + "\n");
                    matcher2 = pattern2.matcher(s);
                    if (matcher2.find())
                        store = false;
                } else {
                    bufferedWriter.write(s + "\n");
                }
            }
            bufferedReader.close();
            bufferedWriter.close();
            fileReader.close();
            fileWriter.close();
            if (isNew)
                System.out.println("删除成功！");
            else {
                if (new File(oldFile).delete() && new File(tempFile).renameTo(new File(oldFile))) {
                    System.out.println("删除成功！");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //高亮或取消高亮
    private void highLight(double L,double B,String oldFile,boolean isHighLight,String color,String newFile,boolean isNew) {
        getBoundary();
        int location = segment(L,B);
        String s;
        if (location >= 0) {
            String signForHL = "<SimpleData name=\"NAME_1\">" + provinces.elementAt(location).name + "</SimpleData>";
            Pattern pattern = Pattern.compile(signForHL);
            Pattern pattern1 = Pattern.compile("<Placemark>");
            Matcher matcher,matcher1;
            ArrayList<String> temp = new ArrayList<>();
            boolean hasDone = false;
            boolean isContinus = false;
            try {
                FileReader fileReader = new FileReader(oldFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                String tempFile = oldFile + ".temp";
                if (isNew)
                    tempFile = newFile;
                FileWriter fileWriter = new FileWriter(tempFile);
                BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
                while ((s = bufferedReader.readLine()) != null) {
                    if (!hasDone) {
                        matcher = pattern.matcher(s);
                        matcher1 = pattern1.matcher(s);
                        if (matcher1.find()) {
                            for (String each:temp) {
                                bufferedWriter.write(each);
                            }
                            temp.clear();
                            temp.add(s + "\n");
                            isContinus = true;
                        } else if (matcher.find()) {
                            //改写，退出
                            temp.add(s + "\n");
                            if (isHighLight) {
                                if(color.equals("")) {
                                    temp.set(1,temp.get(1).replaceAll("</color><fill>\\d</fill>",
                                            "</color><fill>1</fill>"));
                                } else {
                                    temp.set(1,temp.get(1).replaceAll("<color>\\w{6,8}</color><fill>\\d</fill>",
                                            "<color>"+color+"</color><fill>1</fill>"));
                                }
                            }
                            else {
                                temp.set(1,temp.get(1).replaceAll("<fill>\\d</fill>",
                                        "<fill>0</fill>"));
                            }
                            for (String each:temp)
                                bufferedWriter.write(each);
                            hasDone = true;
                        } else if(isContinus/*第一次找到之前顺利写入*/) {
                            temp.add(s + "\n");
                        } else {
                            bufferedWriter.write(s + "\n");
                        }
                    }
                    else
                        bufferedWriter.write(s + "\n");
                }
                bufferedReader.close();
                bufferedWriter.close();
                fileWriter.close();
                fileReader.close();
                if (isNew) {
                    if (isHighLight)
                        System.out.println("高亮显示！");
                    else
                        System.out.println("取消高亮显示！");
                }
                else {
                    if (new File(oldFile).delete() && new File(tempFile).renameTo(new File(oldFile))) {
                        if (isHighLight)
                            System.out.println("高亮显示！");
                        else
                            System.out.println("取消高亮显示！");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //改变箭头的颜色
    private void changeColor(String L,String B,String oldFile,String color,String newFile,boolean isNew) {
        String s;
        Pattern pattern = Pattern.compile(L + "," + B);
        Matcher matcher;
        Pattern pattern1 = Pattern.compile("<color>\\w{6,8}</color>");
        Matcher matcher1;
        int sign = 0;
        try {
            FileReader fileReader = new FileReader(oldFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String tempfile = oldFile + ".temp";
            if (isNew)
                tempfile = newFile;
            FileWriter fileWriter = new FileWriter(tempfile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            while ((s = bufferedReader.readLine()) != null) {
                matcher = pattern.matcher(s);
                if (matcher.find()) {
                    bufferedWriter.write(s + "\n");
                    while ((s = bufferedReader.readLine()) != null) {
                        matcher1 = pattern1.matcher(s);
                        if (matcher1.find()) {
                            s = s.replaceAll("\\w{6,8}",color);
                            bufferedWriter.write(s + "\n");
                            sign++;
                            if (sign == 3)
                                break;
                        } else {
                            bufferedWriter.write(s + "\n");
                        }
                    }
                    break;
                } else {
                    bufferedWriter.write(s + "\n");
                }
            }
            //写剩余部分
            while ((s = bufferedReader.readLine()) != null) {
                bufferedWriter.write(s + "\n");
            }
            bufferedReader.close();
            bufferedWriter.close();
            fileWriter.close();
            fileReader.close();
            if (isNew)
                System.out.println("修改颜色成功！");
            else {
                if (new File(oldFile).delete() && new File(tempfile).renameTo(new File(oldFile))) {
                    System.out.println("修改颜色成功！");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //改变GPS点的图标
    private void changeLabel(String L,String B,String oldFile,String label,boolean inner,String newFile,boolean isNew) {
        String labelUrl = label;
        if (inner) {
            int location = Integer.valueOf(label);
            if (location <= innerLables.length && location > 0)
                labelUrl = innerLables[location -1];
            else {
                System.out.println("内置标签序号小于等于" + innerLables.length);
                System.exit(1);
            }
        }
        //改变labelurl
        String s;
        Pattern pattern = Pattern.compile("<Style id=\"point\">");
        Pattern pattern1 = Pattern.compile("<href>.+</href>");
        Pattern pattern2 = Pattern.compile(L + "," + B);
        Pattern pattern3 = Pattern.compile("</coordinates>");
        Matcher matcher,matcher1,matcher2,matcher3;
        ArrayList<String> temps = new ArrayList<>();
        boolean hasDone = false;
        try {
            FileReader fileReader = new FileReader(oldFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String tempFile = oldFile + ".temp";
            if (isNew)
                tempFile = newFile;
            FileWriter fileWriter = new FileWriter(tempFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            while ((s = bufferedReader.readLine()) != null) {
                matcher = pattern.matcher(s);
                if (matcher.find() && !hasDone) {
                    temps.add(s + "\n");
                    while ((s = bufferedReader.readLine()) != null) {
                        matcher2 = pattern2.matcher(s);
                        //L,B对应时
                        if (matcher2.find()) {
                            temps.add(s + "\n");
                            for (String each:temps) {
                                matcher1 = pattern1.matcher(each);
                                if (matcher1.find()) {
                                    each = each.replaceAll("<href>.+</href>","<href>" + labelUrl + "</href>");
                                    bufferedWriter.write(each);
                                } else
                                    bufferedWriter.write(each);
                            }
                            hasDone = true;
                            break;
                        } else {
                            matcher3 = pattern3.matcher(s);
                            if (matcher3.find()) {
                                temps.add(s + "\n");
                                for (String each:temps) {
                                    bufferedWriter.write(each);
                                }
                                temps.clear();
                                break;
                            } else
                                temps.add(s + "\n");
                        }
                    }
                } else {
                    bufferedWriter.write(s + "\n");
                }
            }
            bufferedReader.close();
            bufferedWriter.close();
            fileReader.close();
            fileWriter.close();
            if (isNew)
                System.out.println("修改图标成功！");
            else {
                if (new File(oldFile).delete() && new File(tempFile).renameTo(new File(oldFile))) {
                    System.out.println("修改图标成功！");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public static void main(String args[]) {
        //用法
        String Tutorial = "NOTE!!!china.kml and kml.jar must be in the same path!\n\n" +
                "1.create\n" +
                "java -jar kml.jar -p pointsPath [-C aabbggrr] [-I innerLabelNum / -O labelUrl] [-o kmlFilePath]\n\n" +
                "2.add\n" +
                "java -jar kml.jar -a L B Ve Vn sigVe sigVn sigVen ID -f oldKmlFilePath [-o newKmlFilePath]\n\n" +
                "3.delete\n" +
                "java -jar kml.jar -d L B -f oldKmlFilePath [-o newKmlFilePaht]\n\n" +
                "4.highLight\n" +
                "java -jar kml.jar -H L B -f oldKmlFilePath [-C aabbggrr] [-o newKmlFilePath]\n\n" +
                "5.cancel highLight\n" +
                "java -jar kml.jar -h L B -f oldKmlFilePath [-o newKmlFilePath]\n\n" +
                "6.change arrow color\n" +
                "java -jar kml.jar -c L B -f oldKmlFilePath -C aabbggrr [-o newKmlFilePath]\n\n" +
                "7.change icon\n" +
                "java -jar kml.jar -l L B -f oldKmlFilePath -I innerLabelNum [-o newKmlFilePath]\n" +
                "java -jar kml.jar -l L B -f oldKmlFilePath -O labelUrl [-o newKmlFilePath]\n\n" +
                "8.show this information\n" +
                "java -jar kml.jar --help\n";
        //确保当前路径下有china.kml
        File file = new File("china.kml");
        if (!file.exists()) {
            System.out.println(Tutorial);
            System.exit(1);
        }
        GE ge = new GE();
        //必须确保传入合法参数！以免程序崩溃！
        int len = args.length;
        if (len == 0) {
            System.out.println(Tutorial);
            System.exit(1);
        }
        else if (args[0].equals("-a")) {
            if (len < 11) {
                System.out.println("输入参数有误！");
                System.exit(1);
            } else if(len == 11 && args[9].equals("-f")) {
                ge.addOnOld(Double.valueOf(args[1]),
                        Double.valueOf(args[2]),
                        Double.valueOf(args[3]),
                        Double.valueOf(args[4]),
                        Double.valueOf(args[5]),
                        Double.valueOf(args[6]),
                        Double.valueOf(args[7]),
                        args[8],args[10]);
            } else if (len == 13 && args[9].equals("-f") && args[11].equals("-o")){
                ge.addOnNew(Double.valueOf(args[1]),
                        Double.valueOf(args[2]),
                        Double.valueOf(args[3]),
                        Double.valueOf(args[4]),
                        Double.valueOf(args[5]),
                        Double.valueOf(args[6]),
                        Double.valueOf(args[7]),
                        args[8],args[10],args[12]);
            } else if (len == 13 && args[9].equals("-o") && args[11].equals("-f")) {
                ge.addOnNew(Double.valueOf(args[1]),
                        Double.valueOf(args[2]),
                        Double.valueOf(args[3]),
                        Double.valueOf(args[4]),
                        Double.valueOf(args[5]),
                        Double.valueOf(args[6]),
                        Double.valueOf(args[7]),
                        args[8],args[12],args[10]);
            }
            else {
                System.out.println("输入参数有误！");
                System.exit(1);
            }
        }
        else {
            String pointFile = "";
            String newFile = "";
            String color = "";
            String labelUrl = "";
            String L = "";
            String B = "";
            String oldFile = "";
            boolean changeColor = false;
            boolean changeLabel = false;
            boolean isInner = false;
            boolean isNew = false;
            switch (args[0]) {
                //String pointFile,String newFile,boolean changeColor,String color,boolean changeLabel,String labelUrl,boolean isInner
                case "-p" :
                    pointFile = args[1];
                    for (int i = 2; i < len; i+=2) {
                        String currentParam = args[i];
                        switch (currentParam) {
                            case "-C":
                                changeColor = true;
                                color = args[i+1];
                                break;
                            case "-I":
                                changeLabel = true;
                                isInner = true;
                                labelUrl = args[i+1];
                                break;
                            case "-O":
                                changeLabel = true;
                                isInner = false;
                                labelUrl = args[i+1];
                                break;
                            case "-o":
                                newFile = args[i+1];
                                break;
                                default:
                                  System.out.println("输入参数有误！");
                                  System.exit(1);
                        }
                    }
                    if (newFile.equals(""))
                        newFile = pointFile.split("\\.")[0] + ".kml";
                    ge.createNew(pointFile,newFile,changeColor,color,changeLabel,isInner,labelUrl);
                    break;
                case "-d":
                    L = args[1];
                    B = args[2];
                    for (int i = 3; i < len; i+=2) {
                        switch (args[i]) {
                            case "-f":
                                oldFile = args[i+1];
                                break;
                            case  "-o":
                                newFile = args[i+1];
                                isNew = true;
                                break;
                                default:
                                    System.out.println("输入参数有误!");
                                    System.exit(1);
                        }
                    }
                    ge.delete(L,B,oldFile,newFile,isNew);
                    break;
                case "-H":
                    L = args[1];
                    B = args[2];
                    for (int i = 3; i < len; i+=2) {
                        switch (args[i]) {
                            case "-f":
                                oldFile = args[i+1];
                                break;
                            case  "-o":
                                newFile = args[i+1];
                                isNew = true;
                                break;
                            case "-C":
                                color = args[i+1];
                                break;
                                default:
                                    System.out.println("输入参数有误！");
                                    System.exit(1);
                        }
                    }
                    ge.highLight(Double.valueOf(L),Double.valueOf(B),oldFile,true,color,newFile,isNew);
                    break;
                case "-h":
                    L = args[1];
                    B = args[2];
                    for (int i = 3; i < len; i+=2) {
                        switch (args[i]) {
                            case "-f":
                                oldFile = args[i+1];
                                break;
                            case "-o":
                                newFile = args[i+1];
                                isNew = true;
                                break;
                                default:
                                    System.out.println("输入参数有误！");
                                    System.exit(1);
                        }
                    }
                    ge.highLight(Double.valueOf(L),Double.valueOf(B),oldFile,false,color,newFile,isNew);
                    break;
                case "-c":
                    L = args[1];
                    B = args[2];
                    for(int i = 3; i < len; i+=2) {
                        switch (args[i]) {
                            case "-f":
                                oldFile = args[i+1];
                                break;
                            case "-o":
                                newFile = args[i+1];
                                isNew = true;
                                break;
                            case "-C":
                                color = args[i+1];
                                break;
                                default:
                                    System.out.println("输入参数有误!");
                                    System.exit(1);
                        }
                    }
                    ge.changeColor(L,B,oldFile,color,newFile,isNew);
                    break;
                case "-l":
                    L = args[1];
                    B = args[2];
                    for (int i = 3; i < len; i+=2) {
                        switch (args[i]) {
                            case "-f":
                                oldFile = args[i+1];
                                break;
                            case "-o":
                                newFile = args[i+1];
                                isNew = true;
                                break;
                            case "-I":
                                labelUrl = args[i+1];
                                isInner = true;
                                break;
                            case "-O":
                                labelUrl = args[i+1];
                                isInner = false;
                                break;
                                default:
                                    System.out.println("输入参数有误！");
                                    System.exit(1);
                        }
                    }
                    ge.changeLabel(L,B,oldFile,labelUrl,isInner,newFile,isNew);
                    break;
                case "--help":
                    System.out.println(Tutorial);
                    default:
                        System.out.println(Tutorial);
                }
            }
        }
    }