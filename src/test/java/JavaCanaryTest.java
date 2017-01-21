import com.github.javaparser.JavaParser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import org.junit.Test;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import static com.github.javaparser.ast.Modifier.STATIC;
import static com.github.javaparser.ast.Modifier.PUBLIC;
import static org.junit.Assert.*;

public class JavaCanaryTest {
    @Test
    public void basicParse() throws Exception {
        FileInputStream in = new FileInputStream("/Users/air0day/Projects/rod/jasome/src/test/resources/Hours.java");

        // parse the file
        CompilationUnit cu = JavaParser.parse(in);

        // prints the resulting compilation unit to default system output
        //System.out.println(cu.toString());

        cu.getNodesByType(FieldDeclaration.class).stream().
                filter(f -> f.getModifiers().contains(PUBLIC)).
                forEach(f -> System.out.println("Check field at line " + f.getBegin().get().line));
    }

    @Test
    public void triple() throws Exception {



        List<String> fooList = new ArrayList<String>(2);
        fooList.add("hi1");
        fooList.add("hi2");
        fooList.add("hi3");
        fooList.add("hi4");

        assertEquals(4, fooList.size());

    }

}