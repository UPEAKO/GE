import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class GE extends Plot{
    //存储每一个省的数据
    private ArrayList<Province> provinces = new ArrayList<>();

    //arrayList for multi thread
    private ArrayList<Info> tempInfos = new ArrayList<>();
    private ArrayList<Integer> splitPoints = new ArrayList<>();


    /**
     * 获取边界数据
     */
    @Override
    void getBoundary() {
        File file = new File("china.kml");
        String s, ss;
        Pattern pattern = Pattern.compile("<SimpleData name=\"NAME_1\">(.+?)</SimpleData>");
        Pattern pattern1 = Pattern.compile("<LinearRing><coordinates>(.+?)</coordinates></LinearRing>");
        Matcher matcher, matcher1;
        boolean isName = true;
        Province tempProvince = new Province();
        boolean isWrite = false;
        ArrayList<Point> tempBoundary = new ArrayList<>();
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
                        tempBoundary = new ArrayList<>();
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

    /**
     *对一个点进行分类
     */
    private int segment(double L1, double B1) {
        int num;
        Province tempProvince;
        ArrayList<Point> tempBoundary;
        //省级
        for (int i = provinces.size() - 1; i >= 0; i--) {
            //包括岛
            tempProvince = provinces.get(i);
            for (int j = 0; j < tempProvince.boundarys.size(); j++) {
                tempBoundary = tempProvince.boundarys.get(j);
                num = 0;
                for (int k = 0,len2 = tempBoundary.size() - 1; k < len2; k++) {
                    //取竖直的射线;在左右两边;一半空间
                    double tX1 = tempBoundary.get(k).x;
                    double tX2 = tempBoundary.get(k + 1).x;
                    //满足左右(tX1 - L1)*(tX2 - L1) < 0
                    if ((tX1 - L1)*(tX2 - L1) < 0) {
                        double tY1 = tempBoundary.get(k).y;
                        double tY2 = tempBoundary.get(k + 1).y;
                        double tA = tY1 - tY2;
                        //分为k=0与k!=0讨论满足射线的一半空间
                        if (Math.abs(tA) < 1.0E-15) {
                            if (tY1 < B1)
                                num++;
                        } else {
                            double tB = tX2 - tX1;
                            double tC = tX1 * tY2 - tX2 * tY1;
                            double tK1 = -tA / tB;
                            double tK2 = -1 / tK1;
                            double b = B1 - tK2 * L1;
                            double verticalX = -(tB * b + tC) / (tA + tB * tK2);
                            double verticalY = tK2 * verticalX + b;
                            double vecVerY = verticalY - B1;
                            //基础向量(0,-1)
                            if (vecVerY < 0) {
                                num++;
                            }
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

    /**
     * 创建新文件
     * @param pointFile 点文件路径
     * @param newFile 新文件路径
     * @param changeColor 是否修改箭头颜色
     * @param color 颜色值
     * @param changeLabel 是否修改点图标
     * @param isInner 内置点图标序号
     * @param labelUrl 点图标超链接
     */
    @Override
    void createNew(String pointFile,String newFile,boolean changeColor,String color,boolean changeLabel,boolean isInner,String labelUrl) {
        getBoundary();
        try {
            //读取GPS点
            FileReader fileReader = new FileReader(pointFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            FileWriter fileWriter = new FileWriter(newFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            String sp;
            while ((sp = bufferedReader.readLine()) != null) {
                String[] info = sp.split(" ");
                double L = Double.valueOf(info[0]);
                double B = Double.valueOf(info[1]);
                double Ve = Double.valueOf(info[2]);
                double Vn = Double.valueOf(info[3]);
                double sigVe = Double.valueOf(info[4]);
                double sigVn = Double.valueOf(info[5]);
                double sigVen = Double.valueOf(info[6]);
                String ID = info[7];
                tempInfos.add(new Info(L, B, Ve, Vn, sigVe, sigVn, sigVen, ID));
            }
            bufferedReader.close();
            fileReader.close();
            final int numOfThread = 8;
            CountDownLatch countDownLatch = new CountDownLatch(numOfThread);
            for (int i = 0; i < numOfThread; i++) {
                splitPoints.add(i * tempInfos.size() / numOfThread);
            }
            splitPoints.add(tempInfos.size());
            //线程类
            class MyThread implements Runnable {
                private int num;
                private CountDownLatch countDownLatch;
                private MyThread(int num, CountDownLatch countDownLatch) {
                    this.num = num;
                    this.countDownLatch = countDownLatch;
                }
                @Override
                public void run() {
                    for (int i = splitPoints.get(num); i < splitPoints.get(num+1); i++) {
                        int location = segment(tempInfos.get(i).L,tempInfos.get(i).B);
                        if (location >= 0) {
                            provinces.get(location).infos.add(new Info(tempInfos.get(i).L,
                                    tempInfos.get(i).B,
                                    tempInfos.get(i).Ve, tempInfos.get(i).Vn,
                                    tempInfos.get(i).sigVe,
                                    tempInfos.get(i).sigVn,
                                    tempInfos.get(i).sigVen,
                                    tempInfos.get(i).ID));
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


            //添加文件头及徽标
            bufferedWriter.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n" +
                    "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
                    "<Document id=\"root_doc\">\n" +
                    "\t\t<Folder>\n" +
                    "\t\t\t<name>LABEL</name>\n" +
                    "\t\t\t<ScreenOverlay id=\"label\">\n" +
                    "\t\t\t\t<name>label</name>\n" +
                    "\t\t\t\t<Icon>\n" +
                    "\t\t\t\t\t<href>http://www.wypmk.xyz/other/sign.png</href>\n" +
                    "\t\t\t\t</Icon>\n" +
                    "\t\t\t\t<overlayXY x=\"0.0\" y=\"1.0\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
                    "\t\t\t\t<screenXY x=\"0.0\" y=\"1.0\" xunits=\"fraction\" yunits=\"fraction\"/>\n" +
                    "\t\t\t\t<size x=\"200\" y=\"176\" xunits=\"pixels\" yunits=\"pixels\"/>\n" +
                    "\t\t\t</ScreenOverlay>\n" +
                    "\t\t</Folder>\n");
            //写文件
            for (int i = 0; i < provinces.size(); i++) {
                if (provinces.get(i).infos.size() > 0) {
                    bufferedWriter.write("\t\t<Folder id=\"province\">\n");
                    bufferedWriter.write("\t\t\t<name>" + provinces.get(i).name + "</name>\n");
                    for (int j = 0; j < provinces.get(i).infos.size(); j++) {
                        double L1 = provinces.get(i).infos.get(j).L;
                        double B1 = provinces.get(i).infos.get(j).B;
                        double Ve = provinces.get(i).infos.get(j).Ve;
                        double Vn = provinces.get(i).infos.get(j).Vn;
                        double sigVe = provinces.get(i).infos.get(j).sigVe;
                        double sigVn = provinces.get(i).infos.get(j).sigVn;
                        double sigVen = provinces.get(i).infos.get(j).sigVen;
                        String ID = provinces.get(i).infos.get(j).ID;
                        write(L1,B1,Ve,Vn,sigVe,sigVn,sigVen,ID,bufferedWriter,changeColor,color,changeLabel,isInner,labelUrl);
                    }
                    bufferedWriter.write("\t\t</Folder>\n");
                }
            }
            //从边界开始读取并写入新文件
            fileReader = new FileReader("china.kml");
            bufferedReader = new BufferedReader(fileReader);
            int numToJump = 3;
            while ((sp = bufferedReader.readLine()) != null) {
                if (numToJump > 0)
                    numToJump--;
                else {
                    bufferedWriter.write(sp);
                    bufferedWriter.write('\n');
                }
            }
            System.out.println("finish！");
            bufferedWriter.close();
            fileWriter.close();
            bufferedReader.close();
            fileReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *添加点到新文件
     * @param oldFile 原始文件
     * @param newFile 新文件
     */
    @Override
    void addOnNew(double L, double B,double Ve,double Vn,double sigVe,double sigVn,double sigVen,String ID, String oldFile, String newFile) {
        getBoundary();
        int location = segment(L,B);
        boolean hasNotThisPro = true;
        String oldS = "";
        if (location >= 0) {
            String signForAdd = "<name>" + provinces.get(location).name + "</name>";
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
                                    "\t\t\t<name>" + provinces.get(location).name + "</name>\n");
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
                System.out.println("Add successfully！");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *高亮或取消高亮
     */
    @Override
    void highLight(boolean findByName,String L,String B,String oldFile,boolean isHighLight,String color,String newFile,boolean isNew,boolean noUse) {
        String s,name = "",sign = "<name>" + L + "</name>";
        if (!findByName)
            sign = L.replaceAll("\\.","\\\\.") + "," + B.replaceAll("\\.","\\\\.");
        Pattern patternFirst1 = Pattern.compile("<Folder id=\"province\">");
        Pattern patternFirst2 = Pattern.compile(sign);
        Matcher matcherFirst1,matcherFirst2;

        try {
            FileReader fileReader = new FileReader(oldFile);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String tempFile = oldFile + ".temp";
            if (isNew)
                tempFile = newFile;
            FileWriter fileWriter = new FileWriter(tempFile);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            while ((s = bufferedReader.readLine()) != null) {
                bufferedWriter.write(s);
                bufferedWriter.write('\n');
                matcherFirst1 = patternFirst1.matcher(s);
                matcherFirst2 = patternFirst2.matcher(s);
                if (matcherFirst1.find()) {
                    name = bufferedReader.readLine();
                    bufferedWriter.write(name);
                    bufferedWriter.write('\n');
                    name = name.substring(name.indexOf('>') + 1,name.lastIndexOf('<'));
                }
                if (matcherFirst2.find()) {
                    break;
                }
            }

            String signForHL = "<SimpleData name=\"NAME_1\">" + name + "</SimpleData>";
            Pattern pattern = Pattern.compile(signForHL);
            Pattern pattern1 = Pattern.compile("<Placemark>");
            Matcher matcher,matcher1;
            ArrayList<String> temp = new ArrayList<>();
            boolean hasDone = false;
            boolean isContinus = false;
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
                    } else if(isContinus) {
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
                    System.out.println("Highlight successfully！");
                else
                    System.out.println("Cancel highlight successfully！");
            }
            else {
                if (new File(oldFile).delete() && new File(tempFile).renameTo(new File(oldFile))) {
                    if (isHighLight)
                        System.out.println("Highlight successfully！");
                    else
                        System.out.println("Cancel highlight successfully！");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}