package org.sotorrent.ComparatorAppOfGroundTruthAndComputedSimilarity;

import org.sotorrent.posthistoryextractor.diffs.LineDiff;
import org.sotorrent.posthistoryextractor.diffs.diff_match_patch;

import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Util {

    static String computeDiffs(BlockPair blockPair) {

        String string1 = blockPair.leftBlock.postBlock.getContent();
        String string2 = blockPair.rightBlock.postBlock.getContent();

        if(string1.trim().equals(string2.trim())){
            return "<html><head></head><body>" +
                    Controller.convertMarkdownToHTMLViaCommonmarkMark(
                            string2
                    )
                    + "</body></html>";
        }

        final String uniqueLineDiffSeparator_left_start = "§§§§§§1";
        final String uniqueLineDiffSeparator_right_start = "§§§§§§2";
        final String uniqueLineDiffSeparator_left_end = "§§§§§§3";
        final String uniqueLineDiffSeparator_right_end = "§§§§§§4";

        LineDiff lineDiff = new LineDiff();
        List<diff_match_patch.Diff> diffs = lineDiff.diff_lines_only(string1, string2);
        StringBuilder outputRightSb = new StringBuilder();

        for (diff_match_patch.Diff diff : diffs) {

            if (diff.operation == diff_match_patch.Operation.EQUAL) {
                if (!outputRightSb.toString().endsWith("\n"))
                    outputRightSb.append("\n");
                outputRightSb.append(diff.text);

            } else if (diff.operation == diff_match_patch.Operation.DELETE || diff.operation == diff_match_patch.Operation.INSERT) {
                String lineColor = (diff.operation == diff_match_patch.Operation.DELETE) ? "red" : (diff.operation == diff_match_patch.Operation.INSERT) ? "green" : "";

                StringTokenizer tokens = new StringTokenizer(diff.text, "\n");
                while (tokens.hasMoreTokens()) {
                    if (!outputRightSb.toString().endsWith("\n"))
                        outputRightSb.append("\n");
                    String tmpToken = tokens.nextToken();
                    int j = 0;
                    tmpToken = tmpToken.replace("\t", "    ");
                    while (j < tmpToken.length() && (tmpToken.charAt(j) == ' ' || tmpToken.charAt(j) == '\t')) {
                        outputRightSb.append(" ");
                        j++;
                    }
                    outputRightSb
                            .append(uniqueLineDiffSeparator_left_start)
                            .append("<span style=\"color:")
                            .append(lineColor).append("\">")
                            .append(uniqueLineDiffSeparator_left_end)
                            .append(tmpToken.substring(j))
                            .append(uniqueLineDiffSeparator_right_start)
                            .append("</span>")
                            .append(uniqueLineDiffSeparator_right_end)
                    ;
                }
            }

            if (!outputRightSb.toString().endsWith("\n")){
                if(blockPair.leftBlock.postBlock.getPostBlockTypeId() == 1) {
                    outputRightSb.append("\n\n");
                } else if (blockPair.rightBlock.postBlock.getPostBlockTypeId() == 1) {
                    outputRightSb.append("\n");
                }
            }
        }


        outputRightSb = new StringBuilder("<html><head></head><body>" +
                Controller.convertMarkdownToHTMLViaCommonmarkMark(
                        outputRightSb.toString()
                )
                + "</body></html>");



        String outputRight = outputRightSb.toString();


        Pattern pattern_left = Pattern.compile(uniqueLineDiffSeparator_left_start + ".*" + uniqueLineDiffSeparator_left_end);
        Matcher matcher_left = pattern_left.matcher(outputRight);

        outputRight = replaceAngleBracketsAndQuotationMarks(outputRight, matcher_left);


        Pattern pattern_right = Pattern.compile(uniqueLineDiffSeparator_right_start + ".*" + uniqueLineDiffSeparator_right_end);
        Matcher matcher_right = pattern_right.matcher(outputRight);

        outputRight = replaceAngleBracketsAndQuotationMarks(outputRight, matcher_right);


        outputRight = outputRight.replace(uniqueLineDiffSeparator_left_start, "");
        outputRight = outputRight.replace(uniqueLineDiffSeparator_left_end, "");
        outputRight = outputRight.replace(uniqueLineDiffSeparator_right_start, "");
        outputRight = outputRight.replace(uniqueLineDiffSeparator_right_end, "");

        return outputRight;
    }

    private static String replaceAngleBracketsAndQuotationMarks(String output, Matcher matcher_left) {
        while(matcher_left.find()) {
            String newStringLeft = matcher_left.group();
            newStringLeft = newStringLeft.replace("&lt;", "<");
            newStringLeft = newStringLeft.replace("&gt;", ">");
            newStringLeft = newStringLeft.replace("&quot;", "\"");

            output = output.replace(matcher_left.group(), newStringLeft);
        }
        return output;
    }
}
