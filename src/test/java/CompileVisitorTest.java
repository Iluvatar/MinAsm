import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Tests remaining:
//     functions
//     integration testing


class CompileVisitorTest {

    private final String MEMORY_BANK = "bank1";
    private final String MESSAGE = "message1";
    private final String DISPLAY = "display1";

    private void checkResults(List<String> expected, List<String> actual) {
        StringBuilder expectedSer = new StringBuilder("\n");
        StringBuilder actualSer = new StringBuilder("\n");

        for (String s : expected) {
            expectedSer.append("\t").append(s).append("\n");
        }

        for (String s : actual) {
            actualSer.append("\t").append(s).append("\n");
        }

        assertEquals(expectedSer.toString(), actualSer.toString());
    }

    private List<String> genList(String... instructions) {
        return Arrays.asList(instructions.clone());
    }

    private List<String> parseCode(String code, String node) {
        MinAsmLexer lexer = new MinAsmLexer(CharStreams.fromString(code));
        MinAsmParser parser = new MinAsmParser(new CommonTokenStream(lexer));
        ParseTree tree;
        switch (node) {
            case "program":
                tree = parser.program();
                break;
            case "block":
                tree = parser.block();
                break;
            case "stmt":
                tree = parser.stmt();
                break;
            case "expr":
                tree = parser.expr();
                break;
            case "ifStmt":
                tree = parser.ifStmt();
                break;
            case "whileLoop":
                tree = parser.whileLoop();
                break;
            case "forLoop":
                tree = parser.forLoop();
                break;
            case "labelStmt":
                tree = parser.labelStmt();
                break;
            case "gotoStmt":
                tree = parser.gotoStmt();
                break;
            case "function":
                tree = parser.function();
                break;
            case "print":
                tree = parser.print();
                break;
            case "draw":
                tree = parser.draw();
                break;
            case "drawflush":
                tree = parser.drawflush();
                break;
            case "asm":
                tree = parser.asm();
                break;
            case "sensor":
                tree = parser.sensor();
                break;
            case "atom":
                tree = parser.atom();
                break;
            default:
                throw new RuntimeException("unknown node type: " + node);
        }
        CompileVisitor visitor = new CompileVisitor();
        return visitor.visit(tree);
    }

    private String getLanguageOpString(int opId) {
        String op = MinAsmParser.VOCABULARY.getLiteralName(opId);
        return op.substring(1, op.length() - 1);
    }

    private String getAsmOpString(int opId) {
        switch (opId) {
            case MinAsmParser.EXP:
                return "pow";
            case MinAsmParser.MUL:
                return "mul";
            case MinAsmParser.DIV:
                return "div";
            case MinAsmParser.MOD:
                return "mod";
            case MinAsmParser.PLUS:
                return "add";
            case MinAsmParser.MINUS:
                return "sub";
            case MinAsmParser.LSHIFT:
                return "lshift";
            case MinAsmParser.RSHIFT:
                return "rshift";
            case MinAsmParser.LT:
                return "lessThan";
            case MinAsmParser.GT:
                return "greaterThan";
            case MinAsmParser.LTE:
                return "lessThanEq";
            case MinAsmParser.GTE:
                return "greaterThanEq";
            case MinAsmParser.EQ:
                return "equal";
            case MinAsmParser.NEQ:
                return "notEqual";
            case MinAsmParser.BAND:
                return "and";
            case MinAsmParser.BXOR:
                return "xor";
            case MinAsmParser.BOR:
                return "or";
            case MinAsmParser.LAND:
                return "land";
            default:
                throw new RuntimeException("unknown lexer operator: " + opId);
        }
    }

    @Test
    void visitProgram() {
        List<String> actual, expected;

        // test program preamble and end
        actual = parseCode("", "program");
        expected = genList(
                "set bp 0",
                "set eax 0",
                "set ebx 0",
                "set ecx 0",
                "set edx 0",
                "end");
        checkResults(expected, actual);
    }

    @Test
    void visitBlock() {
        List<String> actual, expected;

        // test single line block
        actual = parseCode("10 + 11 * 12;", "block");
        expected = genList(
                "op mul eax 11 12",
                "op add eax 10 eax");
        checkResults(expected, actual);

        // test multiline block
        actual = parseCode("{ 10 + 11 * 12; a = 5; b = 4 - a; }", "block");
        expected = genList(
                "op mul eax 11 12",
                "op add eax 10 eax",
                "set a 5",
                "op sub b 4 a");
        checkResults(expected, actual);

        // test semicolon block
        actual = parseCode(";", "block");
        expected = genList();
        checkResults(expected, actual);
    }

    @Test
    void visitStmt() {
        List<String> actual, expected;

        // test expression
        actual = parseCode("3 + 4 * 5;", "stmt");
        expected = genList(
                "op mul eax 4 5",
                "op add eax 3 eax");
        checkResults(expected, actual);

        // test if statement
        actual = parseCode("if (1 == 1) a = 5;", "stmt");
        expected = genList(
                "op equal eax 1 1",
                "jump .ifLbl0 equal eax 0",
                "set a 5",
                "label .ifLbl0");
        checkResults(expected, actual);

        // test while loop
        actual = parseCode("while (1 != 2) a = 4;", "stmt");
        expected = genList(
                "label .whileLbl0",
                "op notEqual eax 1 2",
                "jump .contLbl0 equal eax 0",
                "set a 4",
                "jump .whileLbl0 always null null",
                "label .contLbl0");
        checkResults(expected, actual);

        // test label
        actual = parseCode("label test:", "stmt");
        expected = genList("label test");
        checkResults(expected, actual);

        // test goto
        actual = parseCode("goto test;", "stmt");
        expected = genList("jump test always null null");
        checkResults(expected, actual);

        // test print
        actual = parseCode("print a;", "stmt");
        expected = genList(
                "print a",
                "printflush " + MESSAGE);
        checkResults(expected, actual);

        // test draw
        actual = parseCode("draw(line, 5, 5, 10, 10, 0, 0);", "stmt");
        expected = genList("draw line 5 5 10 10 0 0");
        checkResults(expected, actual);

        // test drawflush
        actual = parseCode("drawflush();", "stmt");
        expected = genList("drawflush " + DISPLAY);
        checkResults(expected, actual);

        // test asm
        actual = parseCode("asm(\"anything here\"", "stmt");
        expected = genList("anything here");
        checkResults(expected, actual);
    }

    @Test
    void visitFuncCallExpr() {
    }

    @Test
    void visitParenExpr() {
        List<String> actual, expected;

        // test basic expression
        actual = parseCode("(1 + 3)", "expr");
        expected = genList("op add eax 1 3");
        checkResults(expected, actual);

        // test atom
        actual = parseCode("(test)", "expr");
        expected = genList("set eax test");
        checkResults(expected, actual);

        // test order of operations
        actual = parseCode("(1 + 3) * 5", "expr");
        expected = genList(
                "op add eax 1 3",
                "op mul eax eax 5");
        checkResults(expected, actual);

        // test order of operations
        actual = parseCode("2 * (1 + 3)", "expr");
        expected = genList(
                "op add eax 1 3",
                "op mul eax 2 eax");
        checkResults(expected, actual);
    }

    @Test
    void visitUnaryExpr() {
        List<String> actual, expected;

        // test negative of atom
        actual = parseCode("-5", "expr");
        expected = genList("op mul eax -1 5");
        checkResults(expected, actual);

        // test negative of compound expression
        actual = parseCode("-(1 + 2)", "expr");
        expected = genList(
                "op add eax 1 2",
                "op mul eax -1 eax");
        checkResults(expected, actual);

        // test bitwise not of atom
        actual = parseCode("~7", "expr");
        expected = genList("op not eax 7 null");
        checkResults(expected, actual);

        // test bitwise not of compound expression
        actual = parseCode("~(3 + 4)", "expr");
        expected = genList(
                "op add eax 3 4",
                "op not eax eax null");
        checkResults(expected, actual);
    }

    private void testBinOp(int op, String associativity) {
        String langOpString = getLanguageOpString(op);
        String asmOpString = getAsmOpString(op);
        Random rand = new Random();
        int a = rand.nextInt(100);
        int b = rand.nextInt(100);
        int c = rand.nextInt(100);
        int d = rand.nextInt(100);
        List<String> actual, expected;

        // test simple two argument binary operator optimization
        actual = parseCode(String.format("%d %s %d", a, langOpString, b), "expr");
        expected = genList(String.format("op %s eax %d %d", asmOpString, a, b));
        checkResults(expected, actual);

        // test associativity
        actual = parseCode(String.format("%d %s %d %s %d", a, langOpString, b, langOpString, c), "expr");
        if (associativity.equals("left")) {
            expected = genList(
                    String.format("op %s eax %s %s", asmOpString, a, b),
                    String.format("op %s eax eax %s", asmOpString, c)
            );
        } else if (associativity.equals("right")) {
            expected = genList(
                    String.format("op %s eax %s %s", asmOpString, b, c),
                    String.format("op %s eax %s eax", asmOpString, a)
            );
        } else {
            throw new RuntimeException("unknown associativity: " + associativity);
        }
        checkResults(expected, actual);

        // test left atom optimization
        actual = parseCode(String.format("%d %s (%d %s %d)", a, langOpString, b, langOpString, c), "expr");
        expected = genList(
                String.format("op %s eax %d %d", asmOpString, b, c),
                String.format("op %s eax %d eax", asmOpString, a));
        checkResults(expected, actual);

        // test right atom optimization
        actual = parseCode(String.format("(%d %s %d) %s %d", a, langOpString, b, langOpString, c), "expr");
        expected = genList(
                String.format("op %s eax %d %d", asmOpString, a, b),
                String.format("op %s eax eax %s", asmOpString, c));
        checkResults(expected, actual);

        // test full compound expression
        actual = parseCode(String.format("(%d %s %d) %s (%d %s %d)", a, langOpString, b, langOpString, c, langOpString, d), "expr");
        expected = genList(
                String.format("op %s eax %d %d", asmOpString, a, b),
                String.format("write eax %s bp", MEMORY_BANK),
                "op add bp bp 1",
                String.format("op %s eax %d %d", asmOpString, c, d),
                "op sub bp bp 1",
                String.format("read ebx %s bp", MEMORY_BANK),
                String.format("op %s eax ebx eax", asmOpString));
        checkResults(expected, actual);
    }

    @Test
    void visitBinExpr() {
        testBinOp(MinAsmParser.EXP, "right");
        testBinOp(MinAsmParser.MUL, "left");
        testBinOp(MinAsmParser.DIV, "left");
        testBinOp(MinAsmParser.MOD, "left");
        testBinOp(MinAsmParser.PLUS, "left");
        testBinOp(MinAsmParser.MINUS, "left");
        testBinOp(MinAsmParser.LSHIFT, "left");
        testBinOp(MinAsmParser.RSHIFT, "left");
        testBinOp(MinAsmParser.LT, "left");
        testBinOp(MinAsmParser.GT, "left");
        testBinOp(MinAsmParser.LTE, "left");
        testBinOp(MinAsmParser.GTE, "left");
        testBinOp(MinAsmParser.EQ, "left");
        testBinOp(MinAsmParser.NEQ, "left");
        testBinOp(MinAsmParser.BAND, "left");
        testBinOp(MinAsmParser.BXOR, "left");
        testBinOp(MinAsmParser.BOR, "left");
        testBinOp(MinAsmParser.LAND, "left");
    }

    @Test
    void visitAssignExpr() {
        List<String> actual, expected;

        // test atom assignment optimization
        actual = parseCode("a = 5", "expr");
        expected = genList("set a 5");
        checkResults(expected, actual);

        // test binary expression optimization
        actual = parseCode("a = 2 + 4", "expr");
        expected = genList("op add a 2 4");
        checkResults(expected, actual);

        // test unary expression
        actual = parseCode("a = ~5", "expr");
        expected = genList(
                "op not eax 5 null",
                "set a eax");
        checkResults(expected, actual);
    }

    @Test
    void visitSelfAssignExpr() {
        List<String> actual, expected;

        // test atom assignment optimization for +=
        actual = parseCode("a += 6", "expr");
        expected = genList("op add a a 6");
        checkResults(expected, actual);

        // test atom assignment optimization for -=
        actual = parseCode("a -= 6", "expr");
        expected = genList("op sub a a 6");
        checkResults(expected, actual);

        // test complex assignment for +=
        actual = parseCode("a += 6 + 4", "expr");
        expected = genList(
                "op add eax 6 4",
                "op add a a eax");
        checkResults(expected, actual);

        // test complex assignment for -=
        actual = parseCode("a -= 6 + 4", "expr");
        expected = genList(
                "op add eax 6 4",
                "op sub a a eax");
        checkResults(expected, actual);
    }

    @Test
    void visitLitExpr() {
        List<String> actual, expected;

        // test number atom
        actual = parseCode("12", "expr");
        expected = genList("set eax 12");
        checkResults(expected, actual);

        // test string atom
        actual = parseCode("\"hello\"", "expr");
        expected = genList("set eax \"hello\"");
        checkResults(expected, actual);

        // test id atom
        actual = parseCode("test", "expr");
        expected = genList("set eax test");
        checkResults(expected, actual);
    }

    @Test
    void visitSensorExpr() {
        List<String> actual, expected;

        actual = parseCode("#block1.enabled", "expr");
        expected = genList("sensor eax block1 @enabled");
        checkResults(expected, actual);
    }

    @Test
    void visitNakedIf() {
        List<String> actual, expected;

        actual = parseCode("if (1 > 2) { a = 6; b = 7; }", "ifStmt");
        expected = genList(
                "op greaterThan eax 1 2",
                "jump .ifLbl0 equal eax 0",
                "set a 6",
                "set b 7",
                "label .ifLbl0");
        checkResults(expected, actual);
    }

    @Test
    void visitIfElse() {
        List<String> actual, expected;

        actual = parseCode("if (1 > 2) { a = 6; b = 7; } else { c = 8; d = 9; }", "ifStmt");
        expected = genList(
                "op greaterThan eax 1 2",
                "jump .ifLbl0 equal eax 0",
                "set a 6",
                "set b 7",
                "jump .contLbl0 always null null",
                "label .ifLbl0",
                "set c 8",
                "set d 9",
                "label .contLbl0");
        checkResults(expected, actual);
    }

    @Test
    void visitWhileLoop() {
        List<String> actual, expected;

        actual = parseCode("while (a < 4) { a += 1; }", "whileLoop");
        expected = genList(
                "label .whileLbl0",
                "op lessThan eax a 4",
                "jump .contLbl0 equal eax 0",
                "op add a a 1",
                "jump .whileLbl0 always null null",
                "label .contLbl0");
        checkResults(expected, actual);
    }

    @Test
    void visitLabelStmt() {
        List<String> actual, expected;

        actual = parseCode("label name:", "labelStmt");
        expected = genList("label name");
        checkResults(expected, actual);
    }

    @Test
    void visitGotoStmt() {
        List<String> actual, expected;

        actual = parseCode("goto name;", "gotoStmt");
        expected = genList("jump name always null null");
        checkResults(expected, actual);
    }

    @Test
    void visitFunction() {
    }

    @Test
    void visitPrint() {
        List<String> actual, expected;

        // test simple statement
        actual = parseCode("print 5", "print");
        expected = genList(
                "print 5",
                "printflush " + MESSAGE);
        checkResults(expected, actual);

        // test multi statement
        actual = parseCode("print \"hello\", a + 4, a, (1 + 2) * 3;", "print");
        expected = genList(
                "print \"hello\"",
                "op add eax a 4",
                "print eax",
                "print a",
                "op add eax 1 2",
                "op mul eax eax 3",
                "print eax",
                "printflush " + MESSAGE);
        checkResults(expected, actual);
    }

    @Test
    void visitDraw() {
        List<String> actual, expected;

        actual = parseCode("draw(rect, 10, 10, 5, 5, 0, 0);", "draw");
        expected = genList("draw rect 10 10 5 5 0 0");
        checkResults(expected, actual);
    }

    @Test
    void visitDrawflush() {
        List<String> actual, expected;

        actual = parseCode("drawflush();", "drawflush");
        expected = genList("drawflush " + DISPLAY);
        checkResults(expected, actual);
    }

    @Test
    void visitAsm() {
        List<String> actual, expected;

        actual = parseCode("asm(\"anything in here\");", "asm");
        expected = genList("anything in here");
        checkResults(expected, actual);
    }
}