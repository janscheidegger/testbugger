package ch.janscheidegger;

public class Example {

    boolean myBool = false;

    public static void main(String[] args) {
        new Example().start();
        new Example().start();

    }

    private void start() {

        int x = 1;
        int y = 2;
        myBool = true;

        mabybeThrowException("asf");
        System.out.println(x+y);
    }

    private void mabybeThrowException(String value) {
        if (true) {
            System.out.println("Exception");
            throw new RuntimeException();
        }
    }
}
