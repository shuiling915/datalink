package com.datalink.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.*;

@Slf4j
@Service
public class SqlFormatService {

    private static final Set<String> KEYWORDS = new HashSet<>(Arrays.asList(
            "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "IN", "EXISTS", "BETWEEN", "LIKE",
            "IS", "NULL", "AS", "ON", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "FULL",
            "CROSS", "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET", "UNION", "ALL",
            "INSERT", "INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE", "TABLE", "ALTER",
            "DROP", "INDEX", "VIEW", "TRUNCATE", "WITH", "CASE", "WHEN", "THEN", "ELSE", "END",
            "DISTINCT", "ASC", "DESC", "COUNT", "SUM", "AVG", "MIN", "MAX", "CAST", "CONVERT"
    ));

    private static final Set<String> NEWLINE_KEYWORDS = new HashSet<>(Arrays.asList(
            "FROM", "WHERE", "AND", "OR", "GROUP", "ORDER", "HAVING", "LIMIT", "UNION",
            "LEFT", "RIGHT", "INNER", "OUTER", "FULL", "CROSS", "ON", "VALUES", "SET"
    ));

    public String format(String sql) {
        if (sql == null || sql.trim().isEmpty()) return "";

        String normalized = normalizeSpaces(sql);
        StringBuilder result = new StringBuilder();
        int indent = 0;
        int parenDepth = 0;

        List<String> tokens = tokenize(normalized);
        boolean afterSelect = false;
        int selectItemCount = 0;

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            String upper = token.toUpperCase();

            if (token.equals("(")) {
                parenDepth++;
                result.append("( ");
                continue;
            }
            if (token.equals(")")) {
                parenDepth = Math.max(0, parenDepth - 1);
                result.append(") ");
                continue;
            }
            if (token.equals(",")) {
                result.append(",\n").append(indent(indent + 1));
                continue;
            }
            if (token.equals(";")) {
                result.append(";\n");
                indent = 0;
                continue;
            }

            if (KEYWORDS.contains(upper)) {
                if (NEWLINE_KEYWORDS.contains(upper) || upper.equals("SELECT")) {
                    if (result.length() > 0 && !result.toString().endsWith("\n")) {
                        result.append("\n");
                    }
                    result.append(indent(indent)).append(upper).append(" ");
                    if (upper.equals("SELECT")) {
                        afterSelect = true;
                        selectItemCount = 0;
                    }
                } else {
                    result.append(upper).append(" ");
                }
            } else {
                if (afterSelect && selectItemCount == 0) {
                    selectItemCount++;
                }
                result.append(token).append(" ");
            }
        }

        return cleanOutput(result.toString());
    }

    public String compact(String sql) {
        if (sql == null || sql.trim().isEmpty()) return "";
        return normalizeSpaces(sql).trim();
    }

    public Map<String, Object> analyze(String sql) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (sql == null || sql.trim().isEmpty()) {
            result.put("valid", false);
            return result;
        }

        String upper = sql.trim().toUpperCase();
        String type = "UNKNOWN";
        if (upper.startsWith("SELECT")) type = "SELECT";
        else if (upper.startsWith("INSERT")) type = "INSERT";
        else if (upper.startsWith("UPDATE")) type = "UPDATE";
        else if (upper.startsWith("DELETE")) type = "DELETE";
        else if (upper.startsWith("CREATE")) type = "CREATE";
        else if (upper.startsWith("ALTER")) type = "ALTER";
        else if (upper.startsWith("DROP")) type = "DROP";
        else if (upper.startsWith("TRUNCATE")) type = "TRUNCATE";
        else if (upper.startsWith("WITH")) type = "WITH";

        result.put("type", type);
        result.put("length", sql.length());
        result.put("statementCount", countStatements(sql));
        result.put("hasSubquery", upper.contains("SELECT") && upper.indexOf("SELECT") != upper.lastIndexOf("SELECT"));
        result.put("hasJoin", upper.contains("JOIN"));
        result.put("hasUnion", upper.contains("UNION"));
        result.put("hasGroupBy", upper.contains("GROUP BY"));
        result.put("hasOrderBy", upper.contains("ORDER BY"));
        result.put("hasLimit", upper.contains("LIMIT"));
        result.put("tables", extractTables(sql));
        result.put("riskLevel", assessRisk(type, upper));
        return result;
    }

    private String normalizeSpaces(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private List<String> tokenize(String sql) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        char stringChar = 0;

        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);

            if (inString) {
                current.append(c);
                if (c == stringChar) inString = false;
                continue;
            }

            if (c == '\'' || c == '"' || c == '`') {
                inString = true;
                stringChar = c;
                current.append(c);
                continue;
            }

            if (c == ' ' || c == '(' || c == ')' || c == ',' || c == ';') {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current = new StringBuilder();
                }
                if (c != ' ') tokens.add(String.valueOf(c));
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) tokens.add(current.toString());
        return tokens;
    }

    private String indent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) sb.append("  ");
        return sb.toString();
    }

    private String cleanOutput(String sql) {
        return sql.replaceAll(" +\\n", "\n").replaceAll("\\n+", "\n").trim();
    }

    private int countStatements(String sql) {
        int count = 0;
        boolean inString = false;
        char stringChar = 0;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            if (inString) {
                if (c == stringChar) inString = false;
            } else if (c == '\'' || c == '"' || c == '`') {
                inString = true;
                stringChar = c;
            } else if (c == ';') {
                count++;
            }
        }
        if (count == 0 && sql.trim().length() > 0) count = 1;
        return count;
    }

    private List<String> extractTables(String sql) {
        List<String> tables = new ArrayList<>();
        Pattern p = Pattern.compile("(?:FROM|JOIN|INTO|UPDATE)\\s+([`\\w.]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(sql);
        while (m.find()) {
            String table = m.group(1).replace("`", "");
            if (!tables.contains(table)) tables.add(table);
        }
        return tables;
    }

    private String assessRisk(String type, String upper) {
        if (type.equals("DROP") || type.equals("TRUNCATE")) return "HIGH";
        if (type.equals("DELETE") && !upper.contains("WHERE")) return "HIGH";
        if (type.equals("UPDATE") && !upper.contains("WHERE")) return "HIGH";
        if (type.equals("DELETE") || type.equals("UPDATE")) return "MEDIUM";
        if (type.equals("INSERT") || type.equals("ALTER")) return "MEDIUM";
        return "LOW";
    }
}