package com.projectmgmt.logic;

import java.util.List;

/** Builds CSV text that opens correctly in Excel and cannot run formulas ("CSV injection"). */
public final class CsvWriter {
    private CsvWriter() {}

    public static String escape(String value) {
        if (value == null) return "";
        String v = value;
        // A cell starting with = + - @ (or tab / carriage return) can be executed as a formula by Excel.
        if (!v.isEmpty() && "=+-@\t\r".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;
        }
        if (v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r")) {
            v = "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }

    /** UTF-8 byte-order mark first (so Excel shows accents correctly), CRLF line endings. */
    public static String toCsv(List<String> headers, List<List<String>> rows) {
        StringBuilder sb = new StringBuilder("\uFEFF");
        appendRow(sb, headers);
        for (List<String> row : rows) appendRow(sb, row);
        return sb.toString();
    }

    private static void appendRow(StringBuilder sb, List<String> cells) {
        for (int i = 0; i < cells.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(cells.get(i)));
        }
        sb.append("\r\n");
    }
}
