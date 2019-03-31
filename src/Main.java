import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.io.File;

/**
 * created by YuPeng Wen;
 * compile on jdk8;
 * third-party parsing library:Jcommander1.72(http://www.jcommander.org/)
 */
public class Main {
    @Parameter(names = "-F",description = "choose a function(1 for province,2 for plot) first",order = 0,required = true)
    private int function = 1;
    @Parameter(names = "-p",description = "path of source file",order = 1)
    private String pointFile = null;
    @Parameter(names = "-o",description = "path of a new kml file",order = 2)
    private String newFile = null;
    @Parameter(names = "-C",description = "color value of arrow under hexadecimal(aabbggrr)",order = 3)
    private String color = null;
    @Parameter(names = "-I",description = "index num of arrow icon",order = 4)
    private String innerLabelNum = null;
    @Parameter(names = "-O",description = "url of arrow icon",order = 5)
    private String labelUrl = null;
    @Parameter(names = "-a",description = "add a new point(L,B,Ve,Vn,sigVe,sigVn,sigVen,id),separated by comma",order = 6)
    private String newPointInfos = null;
    @Parameter(names = "-f",description = "path of old kml file",order = 7)
    private String oldFie = null;
    @Parameter(names = "-d",description = "delete a point by coordinate(separated by comma) or name",order = 8)
    private String deleteSign = null;
    @Parameter(names = "-H",description = "highLight by coordinate(separated by comma) or name",order = 9)
    private String highLight = null;
    @Parameter(names = "-h",description = "cancel highLight by coordinate(separated by comma) or name",order = 10)
    private String cHighLight = null;
    @Parameter(names = "-c",description = "change arrow color by coordinate(separated by comma) or name",order = 11)
    private String changeColor = null;
    @Parameter(names = "-l",description = "change Icon by coordinate(separated by comma) or name",order = 12)
    private String changeLabel = null;
    @Parameter(names = "-b",description = "choose a block(1 for block1,2 for block2) to highLight or not",order = 13)
    private int block = 2;
    @Parameter(names = "--time",description = "the time consumption",order = 14,hidden = true)
    private boolean time;
    @Parameter(names = "--help",description = "show this information",help = true)
    private boolean help;

    /**
     *解析参数,调用相应function
     */
    public static void main(String[] args) {
        Main main = new Main();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(main)
                .build();
        jCommander.parse(args);
        jCommander.setProgramName("java -jar kml.jar");
        if (main.help)
            jCommander.usage();

        long oldTime = System.currentTimeMillis();

        //预设某些参数
        boolean changeLabel = false,changeColor = false,isNew = false,isInner = false;
        if (main.color != null)
            changeColor = true;
        if (main.innerLabelNum != null) {
            changeLabel = true;
            isInner = true;
        }
        if (main.labelUrl != null)
            changeLabel = true;
        if (main.newFile != null)
            isNew = true;

        if (main.function == 1 || main.function == 2) {
            Plot plot = new Plot();
            if (main.function == 1) {
                plot = new GE();
                //确保边界数据存在
                File file = new File("china.kml");
                if (!file.exists()) {
                    System.out.println("china.kml is not exists!");
                    System.exit(1);
                }
            } else {
                //确保边界数据存在
                File file = new File("CN-block.dat");
                if (!file.exists()) {
                    System.out.println("CN-block.dat is not exists!");
                    System.exit(1);
                }
            }
            //new
            if (main.pointFile != null) {
                if (main.newFile == null)
                    main.newFile = main.pointFile.split("\\.")[0] + ".kml";
                plot.createNew(main.pointFile,
                        main.newFile,
                        changeColor,
                        main.color,
                        changeLabel,
                        isInner,
                        isInner?main.innerLabelNum:main.labelUrl);
            }
            else if (main.oldFie != null) {
                //add
                if (main.newPointInfos != null) {
                    String[] ss = main.newPointInfos.split(",");
                    if (isNew)
                        plot.addOnNew(Double.valueOf(ss[0]),
                                Double.valueOf(ss[1]),
                                Double.valueOf(ss[2]),
                                Double.valueOf(ss[3]),
                                Double.valueOf(ss[4]),
                                Double.valueOf(ss[5]),
                                Double.valueOf(ss[6]),
                                ss[7],
                                main.oldFie,
                                main.newFile
                        );
                    else
                        plot.addOnOld(Double.valueOf(ss[0]),
                                Double.valueOf(ss[1]),
                                Double.valueOf(ss[2]),
                                Double.valueOf(ss[3]),
                                Double.valueOf(ss[4]),
                                Double.valueOf(ss[5]),
                                Double.valueOf(ss[6]),
                                ss[7],
                                main.oldFie
                        );
                }
                //delete
                else if(main.deleteSign != null) {
                    String[] ss = main.deleteSign.split(",");
                    plot.delete(ss.length == 1,
                            ss[0],
                            ss.length > 1?ss[1]:"",
                            main.oldFie,
                            main.newFile,
                            isNew
                    );
                }
                else if(main.color != null) {
                    //Highlight
                    if (main.highLight != null) {
                        String[] ss = main.highLight.split(",");
                        plot.highLight(ss.length == 1,
                                ss[0],
                                ss.length > 1?ss[1]:"",
                                main.oldFie,
                                true,
                                main.color,
                                main.newFile,
                                isNew,
                                main.block == 2
                        );
                    }
                    //changeColor
                    else if (main.changeColor != null) {
                        String[] ss = main.changeColor.split(",");
                        plot.changeColor(ss.length == 1,
                                ss[0],
                                ss.length > 1?ss[1]:"",
                                main.oldFie,
                                main.color,
                                main.newFile,
                                isNew
                        );
                    }
                }
                //changeLabel
                else if(main.changeLabel != null) {
                    String [] ss = main.changeLabel.split(",");
                    plot.changeLabel(ss.length == 1,
                            ss[0],
                            ss.length > 1?ss[1]:"",
                            main.oldFie,
                            isInner?main.innerLabelNum:main.labelUrl,
                            isInner,
                            main.newFile,
                            isNew
                    );
                }
                //not highLight
                else if (main.cHighLight != null) {
                    String[] ss = main.cHighLight.split(",");
                    plot.highLight(ss.length == 1,
                            ss[0],
                            ss.length > 1?ss[1]:"",
                            main.oldFie,
                            false,
                            "",
                            main.newFile,
                            isNew,
                            main.block ==2
                    );
                }
            }
        }
        if (main.time)
            System.out.println("the time consumption: " + (System.currentTimeMillis() - oldTime) / 1000.0 + "s");
    }
}
