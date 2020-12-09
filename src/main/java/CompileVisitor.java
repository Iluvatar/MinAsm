import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompileVisitor extends MinAsmBaseVisitor<List<String>> {

    private final String MEMORY_BANK = "bank1";
    private final String MESSAGE = "message1";
    private final String DISPLAY = "display1";
    private int uidCounter = 0;

    private final Map<String, List<String>> functions = new HashMap<>();

    private int uid() {
        return uidCounter++;
    }

    private List<String> cat(String... strings) {
        return Arrays.asList(strings);
    }

    private List<String> cat(List<String> base, String... strings) {
        List<String> out = new ArrayList<>(base);
        out.addAll(Arrays.asList(strings));
        return out;
    }

    private List<String> cat(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>();
        out.addAll(a);
        out.addAll(b);
        return out;
    }

    private List<String> setupInstructions() {
        return cat(
                "set bp 0",
                "set eax 0",
                "set ebx 0",
                "set ecx 0",
                "set edx 0");
    }

    private List<String> pushInstructions(String reg) {
        return cat(
                String.format("write %s %s bp", reg, MEMORY_BANK),
                "op add bp bp 1");
    }

    private List<String> popInstructions(String reg) {
        return cat(
                "op sub bp bp 1",
                String.format("read %s %s bp", reg, MEMORY_BANK));
    }

    private MinAsmParser.AtomContext getAtomExpr(MinAsmParser.ExprContext ctx) {
        return ctx.getChild(MinAsmParser.AtomContext.class, 0);
    }

    private String convertOperation(int lexerOp) {
        switch (lexerOp) {
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
                throw new RuntimeException("unknown lexer operator: " + lexerOp);
        }
    }

    @Override
    public List<String> visitProgram(MinAsmParser.ProgramContext ctx) {
        List<String> output = new ArrayList<>(setupInstructions());

        for (var ec : ctx.stmt()) {
            output.addAll(visit(ec));
        }

        output.add("end");

        return output;
    }

    @Override
    public List<String> visitBlock(MinAsmParser.BlockContext ctx) {
        List<String> output = new ArrayList<>();

        for (var ec : ctx.stmt()) {
            output.addAll(visit(ec));
        }

        return output;
    }

    @Override
    public List<String> visitStmt(MinAsmParser.StmtContext ctx) {
        if (ctx.expr() != null) {
            return visit(ctx.expr());
        } else if (ctx.print() != null) {
            return visit(ctx.print());
        } else if (ctx.draw() != null) {
            return visit(ctx.draw());
        } else if (ctx.drawflush() != null) {
            return visit(ctx.drawflush());
        } else if (ctx.asm() != null) {
            return visit(ctx.asm());
        } else if (ctx.function() != null) {
            return visit(ctx.function());
        } else if (ctx.ifStmt() != null) {
            return visit(ctx.ifStmt());
        } else if (ctx.labelStmt() != null) {
            return visit(ctx.labelStmt());
        } else if (ctx.gotoStmt() != null) {
            return visit(ctx.gotoStmt());
        } else if (ctx.whileLoop() != null) {
            return visit(ctx.whileLoop());
        } else {
            throw new RuntimeException("invalid statement found: " + ctx.getText());
        }
    }

    @Override
    public List<String> visitFuncCallExpr(MinAsmParser.FuncCallExprContext ctx) {
        String funcName = ctx.ID(0).getText();
        if (!functions.containsKey(funcName)) {
            throw new RuntimeException("unknown function: " + funcName);
        }

        return functions.get(funcName);
    }

    @Override
    public List<String> visitParenExpr(MinAsmParser.ParenExprContext ctx) {
        return visit(ctx.expr());
    }

    @Override
    public List<String> visitUnaryExpr(MinAsmParser.UnaryExprContext ctx) {
        MinAsmParser.AtomContext e = getAtomExpr(ctx.expr());

        List<String> instructions;
        String arg;

        if (e != null) {
            arg = e.getText();
            instructions = cat();
        } else {
            arg = "eax";
            instructions = visit(ctx.expr());
        }

        switch (ctx.op.getType()) {
            case MinAsmParser.MINUS:
                return cat(instructions,
                        String.format("op mul eax -1 %s", arg));
            case MinAsmParser.BNOT:
                return cat(instructions,
                        String.format("op not eax %s null", arg));
            default:
                throw new RuntimeException("unknown operator: " + MinAsmParser.VOCABULARY.getSymbolicName(ctx.op.getType()));
        }
    }

    private List<String> visitBinExprAbst(MinAsmParser.BinExprContext ctx, String endStore) {
        MinAsmParser.AtomContext leftAtom = getAtomExpr(ctx.expr(0));
        MinAsmParser.AtomContext rightAtom = getAtomExpr(ctx.expr(1));

        String leftArg;
        String rightArg;
        List<String> instructions;

        if (leftAtom != null && rightAtom != null) {
            leftArg = leftAtom.getText();
            rightArg = rightAtom.getText();
            instructions = cat();
        } else if (leftAtom != null) {
            leftArg = leftAtom.getText();
            rightArg = "eax";
            instructions = visit(ctx.expr(1));
        } else if (rightAtom != null) {
            leftArg = "eax";
            rightArg = rightAtom.getText();
            instructions = visit(ctx.expr(0));
        } else {
            leftArg = "ebx";
            rightArg = "eax";
            List<String> left = visit(ctx.expr(0));
            left = cat(left, pushInstructions("eax"));
            List<String> right = visit(ctx.expr(1));
            right = cat(right, popInstructions("ebx"));
            instructions = cat(left, right);
        }

        String op = convertOperation(ctx.op.getType());
        return cat(instructions,
                String.format("op %s %s %s %s", op, endStore, leftArg, rightArg));
    }

    @Override
    public List<String> visitBinExpr(MinAsmParser.BinExprContext ctx) {
        return visitBinExprAbst(ctx, "eax");
    }

    @Override
    public List<String> visitAssignExpr(MinAsmParser.AssignExprContext ctx) {
        MinAsmParser.AtomContext e = getAtomExpr(ctx.expr());
        MinAsmParser.BinExprContext b = ctx.getChild(MinAsmParser.BinExprContext.class, 0);

        String id = ctx.ID().getText();

        if (e != null) {
            return cat(String.format("set %s %s", id, e.getText()));
        }

        if (b != null) {
            return visitBinExprAbst(b, id);
        }

        List<String> instructions = visit(ctx.expr());
        return cat(instructions, String.format("set %s eax", id));
    }

    @Override
    public List<String> visitSelfAssignExpr(MinAsmParser.SelfAssignExprContext ctx) {
        MinAsmParser.AtomContext e = getAtomExpr(ctx.expr());
        String id = ctx.ID().getText();
        String arg;
        List<String> instructions;

        if (e != null) {
            arg = e.getText();
            instructions = cat();
        } else {
            arg = "eax";
            instructions = visit(ctx.expr());
        }

        switch (ctx.op.getType()) {
            case MinAsmParser.PEQAS:
                return cat(instructions,
                        String.format("op add %s %s %s", id, id, arg));
            case MinAsmParser.MEQAS:
                return cat(instructions,
                        String.format("op sub %s %s %s", id, id, arg));
            default:
                throw new RuntimeException("unknown operator: " + MinAsmParser.VOCABULARY.getSymbolicName(ctx.op.getType()));
        }
    }

    @Override
    public List<String> visitLitExpr(MinAsmParser.LitExprContext ctx) {
        String value = ctx.getText();
        return cat("set eax " + value);
    }

    @Override
    public List<String> visitSensorExpr(MinAsmParser.SensorExprContext ctx) {
        String blockName = ctx.sensor().ID(0).getText();
        String attr = ctx.sensor().ID(1).getText();
        return cat(String.format("sensor eax %s @%s", blockName, attr));
    }

    @Override
    public List<String> visitNakedIf(MinAsmParser.NakedIfContext ctx) {
        List<String> instructions = visit(ctx.expr());
        String ifLabel = ".ifLbl" + uid();
        instructions = cat(instructions,
                "jump " + ifLabel + " equal eax 0");
        instructions = cat(instructions, visit(ctx.block()));
        instructions = cat(instructions,
                "label " + ifLabel);
        return instructions;
    }

    @Override
    public List<String> visitIfElse(MinAsmParser.IfElseContext ctx) {
        List<String> instructions = visit(ctx.expr());
        int uid = uid();
        String ifLabel = ".ifLbl" + uid;
        String contLabel = ".contLbl" + uid;
        instructions = cat(instructions,
                "jump " + ifLabel + " equal eax 0");
        instructions = cat(instructions, visit(ctx.block(0)));
        instructions = cat(instructions,
                "jump " + contLabel + " always null null",
                "label " + ifLabel);
        instructions = cat(instructions, visit(ctx.block(1)));
        instructions = cat(instructions,
                "label " + contLabel);
        return instructions;
    }

    @Override
    public List<String> visitWhileLoop(MinAsmParser.WhileLoopContext ctx) {
        int uid = uid();
        List<String> instructions;
        String whileLabel = ".whileLbl" + uid;
        String contLabel = ".contLbl" + uid;
        instructions = cat("label " + whileLabel);
        instructions = cat(instructions, visit(ctx.expr()));
        instructions = cat(instructions,
                "jump " + contLabel + " equal eax 0");
        instructions = cat(instructions, visit(ctx.block()));
        instructions = cat(instructions,
                "jump " + whileLabel + " always null null",
                "label " + contLabel);
        return instructions;
    }

    @Override
    public List<String> visitLabelStmt(MinAsmParser.LabelStmtContext ctx) {
        String label = ctx.ID().getText();
        return cat("label " + label);
    }

    @Override
    public List<String> visitGotoStmt(MinAsmParser.GotoStmtContext ctx) {
        String label = ctx.ID().getText();
        return cat("jump " + label + " always null null");
    }

    @Override
    public List<String> visitFunction(MinAsmParser.FunctionContext ctx) {
        List<String> instructions = visit(ctx.block());
        functions.put(ctx.ID(0).getText(), instructions);
        return cat();
    }

    @Override
    public List<String> visitPrint(MinAsmParser.PrintContext ctx) {
        List<String> out = new ArrayList<>();
        for (MinAsmParser.ExprContext ec : ctx.expr()) {
            MinAsmParser.AtomContext atom = getAtomExpr(ec);
            if (atom != null) {
                out.add("print " + atom.getText());
            } else {
                out = cat(out, visit(ec));
                out.add("print eax");
            }
        }
        out.add("printflush " + MESSAGE);
        return out;
    }

    @Override
    public List<String> visitDraw(MinAsmParser.DrawContext ctx) {
        String e1 = ctx.atom(0).getText();
        String e2 = ctx.atom(1).getText();
        String e3 = ctx.atom(2).getText();
        String e4 = ctx.atom(3).getText();
        String e5 = ctx.atom(4).getText();
        String e6 = ctx.atom(5).getText();
        String e7 = ctx.atom(6).getText();
        String instruction = String.format("draw %s %s %s %s %s %s %s", e1, e2, e3, e4, e5, e6, e7);
        return cat(instruction);
    }

    @Override
    public List<String> visitDrawflush(MinAsmParser.DrawflushContext ctx) {
        return cat("drawflush " + DISPLAY);
    }

    @Override
    public List<String> visitAsm(MinAsmParser.AsmContext ctx) {
        String stringText = ctx.STRING().getText();
        return cat(stringText.substring(1, stringText.length() - 1));
    }
}