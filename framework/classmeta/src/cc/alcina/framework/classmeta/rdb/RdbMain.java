package cc.alcina.framework.classmeta.rdb;

public class RdbMain {
    public static void main(String[] args) {
        while (true) {
            System.out.println("Hi");
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
