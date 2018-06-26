import java.util.stream.IntStream;

public class Fanout {
}

class ClassA {

    public void doStuff() {
        IntStream.range(0, 9).forEach(ClassB::printNumber);
    }

}

class ClassB {
    public static void printNumber(int i) {
        System.out.println(i);
    }
}