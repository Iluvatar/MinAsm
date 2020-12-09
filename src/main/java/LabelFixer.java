import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LabelFixer {
    private static final Pattern labelRegex = Pattern.compile("label (\\S+)");
    private static final Pattern jumpRegex = Pattern.compile("jump (\\S+) (.*)");

    public static List<String> fixLabels(List<String> instructions) {
        Map<String, Integer> labels = new HashMap<>();
        List<String> processedInstructions = new ArrayList<>(instructions);

        int line = 0;
        while (line < processedInstructions.size()) {
            String instruction = processedInstructions.get(line);
            Matcher m = labelRegex.matcher(instruction);
            if (m.find()) {
                String labelName = m.group(1);
                labels.put(labelName, line);
                processedInstructions.remove(line);
            } else {
                line++;
            }
        }

        for (line = 0; line < processedInstructions.size(); line++) {
            String instruction = processedInstructions.get(line);
            Matcher m = jumpRegex.matcher(instruction);
            if (m.find()) {
                String label = m.group(1);
                String rest = m.group(2);

                if (!labels.containsKey(label)) {
                    throw new RuntimeException(String.format("invalid jump to label '%s'", label));
                }

                int jumpLine = labels.get(label);

                processedInstructions.set(line, String.format("jump %d %s", jumpLine, rest));
            }
        }

        return processedInstructions;
    }
}
