import java.util.ArrayList;
import java.util.List;

public class JavaCanary {
    public int triple(int x) {


        //Just making sure I have use of streams
        List<Integer> t = new ArrayList<Integer>();
        t.add(x);
        return t.stream().mapToInt(q -> q * 3).findFirst().getAsInt();
    }
}
