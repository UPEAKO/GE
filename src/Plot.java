import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.SplittableRandom;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Plot {
    //需要先定义好各个地块的边界线段连接关系及其名称;
    //即直接指定各线段的起始序号;
    //规则添加到CN-block.dat末尾,待所有点都读进内存(可随机读取了),然后按照了规则将边界存入相应的数据结构中
    //getBoundary function is VIP
    //规则添加示例
    //eg:(规则前面添加读取点位结束标志,eg:#,代替(s = bufferedReader.readLine()) != null--->!(s = bufferedReader.readLine()).equals("###")
    //一级地块(只有一行)
    //13 0 50;0 45 0(解释:13i,0j,50j)
    //紧接着一级地块相应的二级地块(每个二级地块一行)
    //13 0 50;0 45 0
    //13 0 50;0 45 0
    //每个地块
    //数据结构间分割符号待定

    //读取完成后即进行分类操作,注意暂时不考虑一级地块(一级地块边界只准备用来全局高亮),点实际在各个二级地块中,这与province不同

    //内置Label
    private static String [] innerLables = {
            "http://www.wypmk.xyz/other/cug.png",
            "http://maps.google.com/mapfiles/kml/shapes/placemark_circle.png",
            "http://maps.google.com/mapfiles/kml/shapes/target.png",
            "http://maps.google.com/mapfiles/kml/shapes/shaded_dot.png"
    };

    //arrayList for multi thread
    private ArrayList<Info> tempInfos = new ArrayList<>();
    private ArrayList<Integer> splitPoints = new ArrayList<>();
    //地块边界及点
    private ArrayList<Polygon1> plots = new ArrayList<>();

    private void getBoundary() {
        ArrayList< ArrayList<Point> > boundarys = new ArrayList<>();
        File file = new File("/home/ubd/work/GE/CN-block.dat");
        String s;
        try {
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            ArrayList<Point> tempBoundary = new ArrayList<>();
            //读取混乱边界
            while (!(s = bufferedReader.readLine()).equals("#")) {
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
            //读取边界连接关系,添加边界到polygon1中
            String name1 = "";
            ArrayList<Point> tempBoundary1 = new ArrayList<>();
            ArrayList<Point> tempBoundary2 = new ArrayList<>();
            ArrayList<Polygon2> tempBoundary2s = new ArrayList<>();
            boolean isPolygon1 = true;
            while ((s = bufferedReader.readLine()) != null) {
                if(s.equals("#")) {
                    //添加对应一级,二级地块
                    plots.add(new Polygon1(name1,tempBoundary1,tempBoundary2s));
                    tempBoundary1 = new ArrayList<>();
                    tempBoundary2s = new ArrayList<>();
                    isPolygon1 = true;
                }
                else {
                    //一级地块边界
                    if (isPolygon1) {
                        name1 = s;
                        String[] part1 = bufferedReader.readLine().split(";");
                        for(int i = 0; i < part1.length; i++) {
                            String[] part2 = part1[i].split(" ");
                            int locationI = Integer.valueOf(part2[0]);
                            int locationJ1 = Integer.valueOf((part2[1]));
                            int locationJ2 = Integer.valueOf((part2[2]));
                            if (locationJ1 <= locationJ2) {
                                for (;locationJ1 <= locationJ2; locationJ1++) {
                                    tempBoundary1.add(new Point(boundarys.get(locationI).get(locationJ1).x,
                                            boundarys.get(locationI).get(locationJ1).y));
                                }
                            }
                            else {
                                for (;locationJ1 >= locationJ2; locationJ1--) {
                                    tempBoundary1.add(new Point(boundarys.get(locationI).get(locationJ1).x,
                                            boundarys.get(locationI).get(locationJ1).y));
                                }
                            }
                        }
                        //边界闭合,添加起点到结尾
                        tempBoundary1.add(new Point(tempBoundary1.get(0).x, tempBoundary1.get(0).y));
                        isPolygon1 = false;
                    }
                    //二级地块边界
                    else {
                        String[] part1 = bufferedReader.readLine().split(";");
                        for(int i = 0; i < part1.length; i++) {
                            String[] part2 = part1[i].split(" ");
                            int locationI = Integer.valueOf(part2[0]);
                            int locationJ1 = Integer.valueOf((part2[1]));
                            int locationJ2 = Integer.valueOf((part2[2]));
                            if (locationJ1 <= locationJ2) {
                                for (;locationJ1 <= locationJ2; locationJ1++) {
                                    tempBoundary2.add(new Point(boundarys.get(locationI).get(locationJ1).x,
                                            boundarys.get(locationI).get(locationJ1).y));
                                }
                            }
                            else {
                                for (;locationJ1 >= locationJ2; locationJ1--) {
                                    tempBoundary2.add(new Point(boundarys.get(locationI).get(locationJ1).x,
                                            boundarys.get(locationI).get(locationJ1).y));
                                }
                            }
                        }
                        //边界闭合,添加起点到结尾
                        tempBoundary2.add(new Point(tempBoundary2.get(0).x,tempBoundary2.get(0).y));
                        tempBoundary2s.add(new Polygon2(s,tempBoundary2,new Vector<>()));
                        tempBoundary2 = new ArrayList<>();
                    }
                }
            }
            //添加最后对应一级,二级地块
            plots.add(new Polygon1(name1,tempBoundary1,tempBoundary2s));

            bufferedReader.close();
            fileReader.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int[] segment(double L1, double B1) {
        int num;
        Polygon1 tempPolygon1;
        ArrayList<Point> boundary2;
        //一级地块
        for (int i = 0, len1 = plots.size(); i < len1; i++) {
            tempPolygon1 = plots.get(i);
            //二级地块
            for (int j = 0, len2 = tempPolygon1.polygon2s.size(); j < len2; j++) {
                boundary2 = tempPolygon1.polygon2s.get(j).boundarys;
                //二级地块边界
                num = 0;
                for (int k = 0, len3 = boundary2.size() - 1; k < len3; k++) {
                    //取竖直的射线;在左右两边;一半空间
                    double tX1 = boundary2.get(k).x;
                    double tX2 = boundary2.get(k + 1).x;
                    //满足左右(tX1 - L1)*(tX2 - L1) < 0
                    if ((tX1 - L1)*(tX2 - L1) < 0) {
                        double tY1 = boundary2.get(k).y;
                        double tY2 = boundary2.get(k + 1).y;
                        //满足一半空间
                        double tA = tY1 - tY2;
                        double tB = tX2 - tX1;
                        double tC = tX1 * tY2 - tX2 * tY1;
                        double tK1 = -tA / tB;
                        double tK2 = -1 / tK1;
                        double b = B1 - tK2 * L1;
                        double verticalX = -(tB * b + tC) / (tA + tB * tK2);
                        double verticalY = tK2 * verticalX + b;
                        //double vecVerX = verticalX - L1;
                        double vecVerY = verticalY - B1;
                        //基础向量(0,-1)
                        if (vecVerY < 0) {
                            num++;
                        }
                    }
                }
                if (num % 2 != 0) {
                    return new int[]{i, j};
                }
            }
        }
        return new int[]{-1,-1};
    }

    private void createNew(String pointFile,String newFile,boolean changeColor,String color,boolean changeLabel,boolean isInner,String labelUrl) {
        ///读取GPS点
        try {
            FileReader fileReader = new FileReader(pointFile);
             BufferedReader bufferedReader = new BufferedReader(fileReader);
            String sp;
            //读一个处理一个
            while ((sp = bufferedReader.readLine()) != null) {
                String[] info = sp.split(" ");
                double L = Double.valueOf(info[0]);
                double B = Double.valueOf(info[1]);
                double Ve = Double.valueOf(info[2]);
                double Vn = Double.valueOf(info[3]);
                double sigVe = Double.valueOf(info[4]);
                double sigVn = Double.valueOf(info[5]);
                //info[6]协方差暂时忽略
                double sigVen = Double.valueOf(info[6]);
                String ID = info[7];
                tempInfos.add(new Info(L, B, Ve, Vn, sigVe, sigVn, sigVen, ID));
            }
            fileReader.close();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ///点分类,实际分入二级地块中
        final int numOfThread = 8;
        CountDownLatch countDownLatch = new CountDownLatch(numOfThread);
        for (int i = 0; i < numOfThread; i++) {
            splitPoints.add(i * tempInfos.size() / numOfThread);
        }
        splitPoints.add(tempInfos.size());
        //线程类,注意这里面传入的num和countDownLatch
        class MyThread implements Runnable {
            private int num;
            private CountDownLatch countDownLatch;

            private MyThread(int num, CountDownLatch countDownLatch) {
                this.num = num;
                this.countDownLatch = countDownLatch;
            }

            @Override
            public void run() {
                for (int i = splitPoints.get(num); i < splitPoints.get(num + 1); i++) {
                    int[] location = segment(tempInfos.get(i).L, tempInfos.get(i).B);
                    if (location[0] >= 0 && location[1] >= 0) {
                        //多个线程同时向arrayList添加元素冲突;故将infos改为vector存储
                        plots.get(location[0]).polygon2s.get(location[1]).points.add(new Info(tempInfos.get(i).L,
                                tempInfos.get(i).B,
                                tempInfos.get(i).Ve, tempInfos.get(i).Vn,
                                tempInfos.get(i).sigVe,
                                tempInfos.get(i).sigVn,
                                tempInfos.get(i).sigVen,
                                tempInfos.get(i).ID));
                    }
                    else {
                        System.out.println("分类错误: " + tempInfos.get(i).L + "," + tempInfos.get(i).B);
                    }
                }
                //主线程等待
                countDownLatch.countDown();
            }
        }
        //启动多个线程
        for (int num = 0; num < numOfThread; num++) {
            new Thread(new MyThread(num,countDownLatch)).start();
        }
        try {
            //主线程等待
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        ///一级及相应的二级边界放在同一个folder
        try {
            File file = new File(newFile);
            FileWriter fileWriter = new FileWriter(file);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            //kml头部
            bufferedWriter.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                    "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                    "<Document id=\"root_doc\">\n");
            //一二级地块边界
            ArrayList<Point> tempBoundary;
            //二级地块内的点
            Vector<Info> tempPoints;
            String s1 = "\t\t\t\t<PolyStyle>\n" +
                    "\t\t\t\t\t<color>ffb48246</color>\n" +
                    "\t\t\t\t\t<fill>0</fill>\n" +
                    "\t\t\t\t</PolyStyle>\n" +
                    "\t\t\t\t<LineStyle>\n" +
                    "\t\t\t\t\t<color>ffff0000</color>\n" +
                    "\t\t\t\t\t<width>2.0</width>\n" +
                    "\t\t\t\t</LineStyle>\n" +
                    "\t\t\t</Style>\n" +
                    "\t\t\t<Polygon>\n" +
                    "\t\t\t\t<outerBoundaryIs>\n" +
                    "\t\t\t\t\t<LinearRing>\n" +
                    "\t\t\t\t\t\t<coordinates>\n" +
                    "\t\t\t\t\t\t";
            String s2 = "\n" +
                    "\t\t\t\t\t\t</coordinates>\n" +
                    "\t\t\t\t\t</LinearRing>\n" +
                    "\t\t\t\t</outerBoundaryIs>\n" +
                    "\t\t\t</Polygon>\n" +
                    "\t\t</Placemark>\n";

            for (int i = 0,len1 = plots.size(); i < len1; i++) {
                bufferedWriter.write("\t<Folder>\n" +
                        "\t\t<name>");
                bufferedWriter.write(plots.get(i).name);
                bufferedWriter.write("</name>\n");
                //二级点及边界
                for (int j = 0,len3 = plots.get(i).polygon2s.size(); j < len3; j++) {
                    //二级地块内的所有点
                    bufferedWriter.write("\t\t<Folder>\n" +
                            "\t\t\t<name>");
                    bufferedWriter.write(plots.get(i).polygon2s.get(j).name);
                    bufferedWriter.write("_points</name>\n");
                    //每一个点
                    tempPoints = plots.get(i).polygon2s.get(j).points;
                    for (int k = 0,len5 = tempPoints.size(); k < len5; k++) {
                        double L1 = tempPoints.get(k).L;
                        double B1 = tempPoints.get(k).B;
                        double Ve = tempPoints.get(k).Ve;
                        double Vn = tempPoints.get(k).Vn;
                        double sigVe = tempPoints.get(k).sigVe;
                        double sigVn = tempPoints.get(k).sigVn;
                        double sigVen = tempPoints.get(k).sigVen;
                        String ID = tempPoints.get(k).ID;
                        write(L1,B1,Ve,Vn,sigVe,sigVn,sigVen,ID,bufferedWriter,changeColor,color,changeLabel,isInner,labelUrl);
                    }
                    bufferedWriter.write("\t\t</Folder>\n");
                    //每个二级地块
                    tempBoundary = plots.get(i).polygon2s.get(j).boundarys;
                    bufferedWriter.write("\t\t<Placemark>\n" +
                            "\t\t\t<name>");
                    bufferedWriter.write(plots.get(i).polygon2s.get(j).name);
                    bufferedWriter.write("_boundary</name>\n" +
                            "\t\t\t<Style id=\"polygon2\">\n");
                    bufferedWriter.write(s1);
                    StringBuffer stringBuffer1 = new StringBuffer();
                    for (int k = 0,len4 = tempBoundary.size(); k < len4; k++) {
                        stringBuffer1.append(tempBoundary.get(k).x);
                        stringBuffer1.append(',');
                        stringBuffer1.append(tempBoundary.get(k).y);
                        stringBuffer1.append(' ');
                    }
                    bufferedWriter.write(stringBuffer1.toString());
                    bufferedWriter.write(s2);
                }
                //一级边界
                bufferedWriter.write("\t\t<Placemark>\n" +
                        "\t\t\t<name>");
                bufferedWriter.write(plots.get(i).name);
                bufferedWriter.write("_boundary</name>\n" +
                        "\t\t\t<Style id=\"polygon1\">\n");
                bufferedWriter.write(s1);
                tempBoundary = plots.get(i).boundarys;
                StringBuffer stringBuffer = new StringBuffer();
                for (int j = 0,len2 = tempBoundary.size(); j < len2; j++) {
                    stringBuffer.append(tempBoundary.get(j).x);
                    stringBuffer.append(',');
                    stringBuffer.append(tempBoundary.get(j).y);
                    stringBuffer.append(' ');
                }
                bufferedWriter.write(stringBuffer.toString());
                bufferedWriter.write(s2);
                bufferedWriter.write("\t</Folder>\n");
            }
            bufferedWriter.write("</Document>\n" +
                    "</kml>");
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                    baseurl = baseurl.replaceAll("<href>.+</href>",innerLables[location-1]);
                }
            } else {
                baseurl = baseurl.replaceAll("<href>.+</href>","<href>" + labelUrl + "</href>");
            }
        }


        try {
            bufferedWriter.write("\t\t\t<Folder id=\"arrow\">\n" +
                    "\t\t\t\t<name>" + ID + "</name>\n");
            //起点图标
            bufferedWriter.write("\t\t\t\t<Placemark>\n" +
                    "\t\t\t\t\t<Point>\n" +
                    "\t\t\t\t\t\t<coordinates>\n");
            bufferedWriter.write("\t\t\t\t\t\t" + L1 + "," + B1 +"\n");
            bufferedWriter.write("\t\t\t\t\t\t</coordinates>\n" +
                    "\t\t\t\t\t</Point>\n" +
                    "\t\t\t\t\t<Style id=\"point\">\n" +
                    "\t\t\t\t\t\t<IconStyle>\n" +
                    "\t\t\t\t\t\t\t<Icon>\n" +
                    baseurl +
                    "\t\t\t\t\t\t\t</Icon>\n" +
                    "\t\t\t\t\t\t</IconStyle>\n" +
                    "\t\t\t\t\t</Style>\n" +
                    "\t\t\t\t</Placemark>\n");
            //线段
            bufferedWriter.write("\t\t\t\t<Placemark>\n" +
                    "\t\t\t\t\t<Style id=\"arrow\">\n" +
                    "\t\t\t\t\t\t<LineStyle>\n" +
                    basecolor +
                    "\t\t\t\t\t\t\t<width>1.0</width>\n" +
                    "\t\t\t\t\t\t</LineStyle>\n" +
                    "\t\t\t\t\t</Style>\n"  +
                    "\t\t\t\t\t<LinearRing>\n" +
                    "\t\t\t\t\t\t<coordinates>\n");
            String line = "\t\t\t\t\t\t\t" + L1 + "," + B1 + " " +
                    L2 + "," + B2 + "\n";
            bufferedWriter.write(line);
            bufferedWriter.write("\t\t\t\t\t\t</coordinates>\n" +
                    "\t\t\t\t\t</LinearRing>\n" +
                    "\t\t\t\t</Placemark>\n" +
                    "\t\t\t\t<Placemark>\n" +
                    "\t\t\t\t\t<Style id=\"arrow\">\n" +
                    "\t\t\t\t\t\t<LineStyle>\n" +
                    basecolor +
                    "\t\t\t\t\t\t\t<width>1.0</width>\n" +
                    "\t\t\t\t\t\t</LineStyle>\n" +
                    "\t\t\t\t\t</Style>\n"  +
                    "\t\t\t\t\t<LinearRing>\n" +
                    "\t\t\t\t\t\t<coordinates>\n");
            //箭头
            String arrow = "\t\t\t\t\t\t\t" + arrowL1 + "," + arrowB1 + " " +
                    L2 + "," + B2 + " " +
                    arrowL2 + "," + arrowB2 + "\n";
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

    private void addOnNew(double L, double B,double Ve,double Vn,double sigVe,double sigVn,double sigVen,String ID, String oldFile, String newFile) {
        getBoundary();
        int[] location = segment(L,B);
        if (location[0] >= 0 && location[1] >= 0) {
            String signForAdd = "<name>" + plots.get(location[0]).polygon2s.get(location[1]).name + "_points</name>";
            Pattern pattern = Pattern.compile(signForAdd);
            Matcher matcher;
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
                    }
                    else
                        bufferedWriter.write(s + "\n");

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

    //直接移植
    private void addOnOld(double L, double B,double Ve,double Vn,double sigVe,double sigVn,double sigVen,String ID, String oldFile) {
        String tempFile = oldFile+".temp";
        addOnNew(L,B,Ve,Vn,sigVe,sigVn,sigVen,ID,oldFile,tempFile);
        if (new File(oldFile).delete() && new File(tempFile).renameTo(new File(oldFile))) {
            System.out.println("添加成功！");
        }
    }

    //直接移植
    private void delete(boolean findByName,String L,String B,String oldFile,String newFile,boolean isNew) {
        //每次将一个完整的箭头存储起来，当符合条件，不写入新文件，否则写入新文件
        String sign = L;
        if (!findByName)
            sign = L + "," + B;
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


    private void highLight(boolean findByName,String L,String B,String oldFile,boolean isHighLight,String color,String newFile,boolean isNew,boolean isBlock2) {
        String sign = "<name>" + L + "</name>";
        String s;
        if (!findByName)
            sign = L+","+B;
        Pattern pattern = Pattern.compile(sign);
        Pattern pattern1 = Pattern.compile("<Style id=\"polygon1\">");
        Pattern pattern2 = Pattern.compile("<Style id=\"polygon2\">");
        Matcher matcher,matcher1or2;
        try {
            FileReader fileReader = new FileReader(oldFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            if (!isNew)
                newFile = oldFile+".temp";
            FileWriter fileWriter = new FileWriter(newFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            boolean hasDone = false;
            int jumpNum = 0;
            while ((s = bufferedReader.readLine()) != null) {
                matcher = pattern.matcher(s);
                if (!hasDone && matcher.find()) {
                    bufferedWriter.write(s+"\n");
                    while ((s = bufferedReader.readLine()) != null) {
                        if (isBlock2)
                            matcher1or2 = pattern2.matcher(s);
                        else
                            matcher1or2 = pattern1.matcher(s);
                        if (matcher1or2.find()) {
                            if (isHighLight) {
                                jumpNum = 3;
                                bufferedWriter.write(s+"\n" +
                                        "\t\t\t\t<PolyStyle>\n" +
                                        "\t\t\t\t\t<color>" + color + "</color>\n" +
                                        "\t\t\t\t\t<fill>1</fill>\n");
                                hasDone = true;
                                break;
                            } else {
                                jumpNum = 3;
                                bufferedWriter.write(s+"\n" +
                                        "\t\t\t\t<PolyStyle>\n" +
                                        "\t\t\t\t\t<color>ffb48246</color>\n" +
                                        "\t\t\t\t\t<fill>0</fill>\n");
                                hasDone =true;
                                break;
                            }
                        }
                        else {
                            bufferedWriter.write(s+"\n");
                        }
                    }
                } else {
                    if (jumpNum > 0)
                        jumpNum--;
                    else
                        bufferedWriter.write(s+"\n");
                }
            }
            if (isNew) {
                if (isHighLight)
                    System.out.println("高亮显示！");
                else
                    System.out.println("取消高亮显示！");
            }
            else {
                if (new File(oldFile).delete() && new File(newFile).renameTo(new File(oldFile))) {
                    if (isHighLight)
                        System.out.println("高亮显示！");
                    else
                        System.out.println("取消高亮显示！");
                }
            }
            bufferedReader.close();
            fileReader.close();
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //直接移植
    private void changeColor(boolean findByName,String L,String B,String oldFile,String color,String newFile,boolean isNew) {
        String s;
        String signF = L;
        if (!findByName)
            signF = L + "," + B;
        Pattern pattern = Pattern.compile(signF);
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

    //直接移植被修改后的write与oval函数后也可直接移植
    private void changeLabel(boolean findByName,String L,String B,String oldFile,String label,boolean inner,String newFile,boolean isNew) {
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
        String sign = "<name>" + L + "</name>";
        String s;
        if (!findByName)
            sign = L+","+B;
        Pattern pattern = Pattern.compile(sign);
        Pattern pattern1 = Pattern.compile("<Style id=\"point\">");
        Matcher matcher,matcher1;
        try {
            FileReader fileReader = new FileReader(oldFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            if (!isNew)
                newFile = oldFile+".temp";
            FileWriter fileWriter = new FileWriter(newFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            boolean hasDone = false;
            int jumpNum = 0;
            while ((s = bufferedReader.readLine()) != null) {
                matcher = pattern.matcher(s);
                if (!hasDone && matcher.find()) {
                    bufferedWriter.write(s+"\n");
                    while ((s = bufferedReader.readLine()) != null) {
                        matcher1 = pattern1.matcher(s);
                        if (matcher1.find()) {
                            jumpNum = 3;
                            bufferedWriter.write(s+"\n" +
                                    "\t\t\t\t\t\t<IconStyle>\n" +
                                    "\t\t\t\t\t\t\t<Icon>\n" +
                                    "\t\t\t\t\t\t\t\t<href>" + labelUrl + "</href>\n");
                            hasDone = true;
                            break;
                        }
                        else {
                            bufferedWriter.write(s+"\n");
                        }
                    }
                } else {
                    if (jumpNum > 0)
                        jumpNum--;
                    else
                        bufferedWriter.write(s+"\n");
                }
            }
            if (isNew)
                System.out.println("修改图标成功！");
            else {
                if (new File(oldFile).delete() && new File(newFile).renameTo(new File(oldFile))) {
                    System.out.println("修改图标成功！");
                }
            }
            bufferedReader.close();
            fileReader.close();
            bufferedWriter.close();
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Plot plot = new Plot();
        plot.getBoundary();
        plot.createNew("/home/ubd/work/GE/GE_TST.txt",
                "/home/ubd/work/GE/GE_TST-1.kml",
                false,
                "",
                false,
                false,
                "");
        //plot.addOnNew(131.5,41.5,3.5,4.5,0.3,0.4,0.5,"newPoint","/home/ubd/work/GE/GE_TST-1.kml","/home/ubd/work/GE/GE_TST-2.kml");
        //plot.delete(false,"131.5","41.5","/home/ubd/work/GE/GE_TST-2.kml","/home/ubd/work/GE/GE_TST-3.kml",true);
        //plot.highLight(true,"newPoint","41.5","/home/ubd/work/GE/GE_TST-2.kml",true,"ffff0000","/home/ubd/work/GE/GE_TST-3.kml",true,true);
        //plot.changeColor(true,"newPoint","41.5","/home/ubd/work/GE/GE_TST-2.kml","ffff0000","",false);
        plot.changeLabel(false,"131.5","41.5","/home/ubd/work/GE/GE_TST-2.kml","3",true,"/home/ubd/work/GE/GE_TST-3.kml",true);
    }
}
