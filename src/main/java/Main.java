import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {
        File outFile = new File("src/main/java/output.txt");
        FileWriter fout = new FileWriter(outFile);
        BufferedWriter bw = new BufferedWriter(fout);

        MinAsmLexer lexer = new MinAsmLexer(CharStreams.fromFileName("src/main/java/input.txt"));
        MinAsmParser parser = new MinAsmParser(new CommonTokenStream(lexer));
        ParseTree tree = parser.program();
        CompileVisitor visitor = new CompileVisitor();
        List<String> instructions = visitor.visit(tree);

        instructions = LabelFixer.fixLabels(instructions);

        for (String i : instructions) {
            System.out.println(i);
            bw.write(i + "\n");
        }

        bw.close();
    }
}
