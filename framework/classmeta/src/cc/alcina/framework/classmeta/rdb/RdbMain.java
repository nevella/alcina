package cc.alcina.framework.classmeta.rdb;

public class RdbMain {
    public static void main(String[] args) {
        ThreadGroup tg = new ThreadGroup("group2");
        new Thread(tg, "t2") {
            int fi1;

            public void run() {
                while (true) {
                    System.out.println("Ho:" + (fi1++));
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
        }.start();
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
