import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LabelFixerTest {

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

    @Test
    void fixLabels() {
        List<String> code, actual, expected;

        // simple test
        code = genList(
                "set a 5",
                "label test",
                "op add a a 1",
                "jump test always null null",
                "end"
        );
        expected = genList(
                "set a 5",
                "op add a a 1",
                "jump 1 always null null",
                "end"
        );
        actual = LabelFixer.fixLabels(code);
        checkResults(expected, actual);

        // jump forward test
        code = genList(
                "set eax 5",
                "jump .test equal eax 0",
                "op add a a 1",
                "label .test",
                "end"
        );
        expected = genList(
                "set eax 5",
                "jump 3 equal eax 0",
                "op add a a 1",
                "end"
        );
        actual = LabelFixer.fixLabels(code);
        checkResults(expected, actual);

        // multi label test
        code = genList(
                "set x 1",
                "label .whileLbl0",
                "op lessThanEq eax x 10",
                "jump .contLbl0 equal eax 0",
                "op add x x 1",
                "sensor eax block1 @enabled",
                "set y eax",
                "set eax y",
                "jump .ifLbl1 equal eax 0",
                "jump end always null null",
                "label .ifLbl1",
                "print \"x is \"",
                "print x",
                "printflush message1",
                "jump .whileLbl0 always null null",
                "label .contLbl0",
                "label end",
                "end"
        );
        expected = genList(
                "set x 1",
                "op lessThanEq eax x 10",
                "jump 13 equal eax 0",
                "op add x x 1",
                "sensor eax block1 @enabled",
                "set y eax",
                "set eax y",
                "jump 9 equal eax 0",
                "jump 13 always null null",
                "print \"x is \"",
                "print x",
                "printflush message1",
                "jump 1 always null null",
                "end"
        );
        actual = LabelFixer.fixLabels(code);
        checkResults(expected, actual);
    }
}