public class Info {
    public double L;
    public double B;
    public double Ve;
    public double Vn;
    public double sigVe;
    public double sigVn;
    public double sigVen;
    public String ID;
    Info (double L, double B, double Ve, double Vn, double sigVe, double sigVn,double sigVen,String ID) {
        this.L = L;
        this.B = B;
        this.Ve = Ve;
        this.Vn = Vn;
        this.sigVe = sigVe;
        this.sigVn = sigVn;
        this.sigVen = sigVen;
        this.ID = ID;
    }
}