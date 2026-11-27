import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jetbrains.kotlin.com.intellij.psi.TokenType;
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType;
import org.jetbrains.kotlin.lexer.KotlinLexer;
import org.jetbrains.kotlin.lexer.KtTokens;

public final class KotlinComments {

    record Token(IElementType type, int start, int end) {}

    record Significant(String type, String text, boolean newlineBefore) {}

    static final Pattern DIRECTIVE = Pattern.compile(
        "^(//\\s*noinspection\\b|//\\s*language=|/\\*\\s*language=|//\\s*ktlint-(disable|enable)\\b"
            + "|/\\*\\s*ktlint-(disable|enable)\\b|//\\s*detekt[:.]|//\\s*@formatter:(on|off)\\b"
            + "|//\\s*(end)?region\\b|//\\s*<editor-fold|//\\s*</editor-fold|//\\s*NOSONAR\\b|//\\s*TODO\\b.*\\bSTOPSHIP\\b)");

    static final Pattern KEEP = Pattern.compile(
        "^(//\\s*noinspection\\b|//\\s*language=|/\\*\\s*language=|//\\s*ktlint-(disable|enable)\\b"
            + "|/\\*\\s*ktlint-(disable|enable)\\b|//\\s*detekt[:.]|//\\s*@formatter:(on|off)\\b)");

    static final Pattern TODO = Pattern.compile("\\b(TODO|FIXME|XXX|HACK)\\b");

    static final Pattern LICENCE = Pattern.compile("(?i)copyright|licen[sc]e");

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            usage();
        }
        List<String> rest = new ArrayList<>(List.of(args).subList(1, args.length));
        switch (args[0]) {
            case "inventory" -> inventory(rest);
            case "strip" -> strip(rest);
            case "verify" -> verify(rest);
            case "check" -> check(rest);
            default -> usage();
        }
    }

    static void usage() {
        System.err.println("usage: KotlinComments inventory <file>...");
        System.err.println("       KotlinComments strip [--keep-headers] <file>...");
        System.err.println("       KotlinComments verify <before-root> <after-root> <relative-file>...");
        System.err.println("       KotlinComments check [--keep-headers] <file>...");
        System.exit(2);
    }

    static List<Token> lex(String text) {
        KotlinLexer lexer = new KotlinLexer();
        lexer.start(text);
        List<Token> tokens = new ArrayList<>();
        while (lexer.getTokenType() != null) {
            tokens.add(new Token(lexer.getTokenType(), lexer.getTokenStart(), lexer.getTokenEnd()));
            lexer.advance();
        }
        int covered = tokens.isEmpty() ? 0 : tokens.get(tokens.size() - 1).end();
        if (covered != text.length()) {
            throw new IllegalStateException("lexer stopped at " + covered + " of " + text.length());
        }
        return tokens;
    }

    static boolean isComment(IElementType type) {
        return KtTokens.COMMENTS.contains(type);
    }

    static boolean isWhitespace(IElementType type) {
        return type == TokenType.WHITE_SPACE;
    }

    static String kind(IElementType type) {
        if (type == KtTokens.EOL_COMMENT) {
            return "line";
        }
        if (type == KtTokens.BLOCK_COMMENT) {
            return "block";
        }
        if (type == KtTokens.DOC_COMMENT) {
            return "kdoc";
        }
        if (type == KtTokens.SHEBANG_COMMENT) {
            return "shebang";
        }
        return type.toString();
    }

    static boolean isHeader(List<Token> tokens, int index, String text) {
        for (int i = 0; i < index; i++) {
            if (!isWhitespace(tokens.get(i).type())) {
                return false;
            }
        }
        Token token = tokens.get(index);
        return isComment(token.type()) && LICENCE.matcher(text.substring(token.start(), token.end())).find();
    }

    static String classify(List<Token> tokens, int index, String text) {
        Token token = tokens.get(index);
        String body = text.substring(token.start(), token.end());
        if (isHeader(tokens, index, text)) {
            return "licence-header";
        }
        if (token.type() == KtTokens.DOC_COMMENT && describesGeneratedOutput(tokens, index, text)) {
            return "kdoc-generated-output";
        }
        if (DIRECTIVE.matcher(body).find()) {
            return "directive";
        }
        if (TODO.matcher(body).find()) {
            return "todo";
        }
        if (token.type() == KtTokens.DOC_COMMENT) {
            return "kdoc";
        }
        return "plain";
    }

    static int lineOf(String text, int offset) {
        int line = 1;
        for (int i = 0; i < offset; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    static void inventory(List<String> files) throws IOException {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (String file : files) {
            String text = read(Path.of(file));
            List<Token> tokens = lex(text);
            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                if (!isComment(token.type())) {
                    continue;
                }
                String body = text.substring(token.start(), token.end());
                String kind = kind(token.type());
                String cls = classify(tokens, i, text);
                counts.merge(kind + "/" + cls, 1, Integer::sum);
                String flat = body.replace("\\", "\\\\").replace("\t", "\\t").replace("\r", "").replace("\n", "\\n");
                System.out.println(file + "\t" + lineOf(text, token.start()) + "\t" + kind + "\t" + cls + "\t" + flat);
            }
        }
        counts.forEach((key, value) -> System.err.println(value + "\t" + key));
    }

    record Options(boolean keepHeaders, List<String> files) {}

    static Options options(List<String> args) {
        boolean keepHeaders = false;
        List<String> files = new ArrayList<>();
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i);
            if (arg.equals("--keep-headers")) {
                keepHeaders = true;
            } else {
                files.add(arg);
            }
        }
        return new Options(keepHeaders, files);
    }

    static boolean keep(Options options, String file, List<Token> tokens, int index, String text) {
        Token token = tokens.get(index);
        String body = text.substring(token.start(), token.end());
        if (KEEP.matcher(body).find()) {
            return true;
        }
        if (options.keepHeaders() && isHeader(tokens, index, text)) {
            return true;
        }
        return token.type() == KtTokens.DOC_COMMENT && describesGeneratedOutput(tokens, index, text);
    }

    static final List<IElementType> DECLARATION_KEYWORDS = List.of(
        KtTokens.FUN_KEYWORD, KtTokens.CLASS_KEYWORD, KtTokens.INTERFACE_KEYWORD, KtTokens.OBJECT_KEYWORD,
        KtTokens.VAL_KEYWORD, KtTokens.VAR_KEYWORD, KtTokens.TYPE_ALIAS_KEYWORD);

    static boolean describesGeneratedOutput(List<Token> tokens, int index, String text) {
        List<Token> ahead = new ArrayList<>();
        int depth = 0;
        for (int i = index + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (isWhitespace(token.type()) || isComment(token.type())) {
                continue;
            }
            if (token.type() == KtTokens.LPAR) {
                depth++;
            } else if (token.type() == KtTokens.RPAR) {
                depth--;
            } else if (depth == 0 && DECLARATION_KEYWORDS.contains(token.type())) {
                break;
            }
            ahead.add(token);
        }
        for (int i = 0; i + 2 < ahead.size(); i++) {
            if (text.substring(ahead.get(i).start(), ahead.get(i).end()).equals("isDescribedByKDoc")
                && ahead.get(i + 1).type() == KtTokens.EQ
                && ahead.get(i + 2).type() == KtTokens.TRUE_KEYWORD) {
                return true;
            }
        }
        return false;
    }

    static String stripText(Options options, String file, String text) {
        List<Token> tokens = lex(text);
        boolean[] remove = new boolean[text.length()];
        List<Integer> spaces = new ArrayList<>();
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (!isComment(token.type()) || keep(options, file, tokens, i, text)) {
                continue;
            }
            int lineStart = token.start();
            while (lineStart > 0 && (text.charAt(lineStart - 1) == ' ' || text.charAt(lineStart - 1) == '\t')) {
                lineStart--;
            }
            boolean nothingBefore = lineStart == 0 || text.charAt(lineStart - 1) == '\n';
            int lineEnd = token.end();
            while (lineEnd < text.length() && (text.charAt(lineEnd) == ' ' || text.charAt(lineEnd) == '\t' || text.charAt(lineEnd) == '\r')) {
                lineEnd++;
            }
            boolean nothingAfter = lineEnd == text.length() || text.charAt(lineEnd) == '\n';
            if (nothingBefore && nothingAfter) {
                int end = lineEnd < text.length() ? lineEnd + 1 : lineEnd;
                mark(remove, lineStart, end);
            } else if (nothingAfter) {
                mark(remove, lineStart, token.end());
            } else if (nothingBefore) {
                mark(remove, token.start(), lineEnd);
            } else {
                boolean spaceBefore = lineStart < token.start();
                boolean spaceAfter = lineEnd > token.end();
                if (spaceBefore && spaceAfter) {
                    mark(remove, token.start(), lineEnd);
                } else if (spaceBefore || spaceAfter) {
                    mark(remove, token.start(), token.end());
                } else {
                    mark(remove, token.start(), token.end());
                    spaces.add(token.start());
                }
            }
        }
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            if (spaces.contains(i)) {
                out.append(' ');
            }
            if (!remove[i]) {
                out.append(text.charAt(i));
            }
        }
        return out.toString();
    }

    static void mark(boolean[] remove, int from, int to) {
        for (int i = from; i < to; i++) {
            remove[i] = true;
        }
    }

    static void strip(List<String> args) throws IOException {
        Options options = options(args);
        int changed = 0;
        for (String file : options.files()) {
            Path path = Path.of(file);
            String before = read(path);
            String after = stripText(options, file, before);
            if (!after.equals(before)) {
                Files.writeString(path, after, StandardCharsets.UTF_8);
                changed++;
            }
        }
        System.err.println("stripped " + changed + " of " + options.files().size() + " files");
    }

    static List<Significant> significant(String text) {
        List<Significant> result = new ArrayList<>();
        boolean newline = false;
        for (Token token : lex(text)) {
            String body = text.substring(token.start(), token.end());
            if (isWhitespace(token.type()) || isComment(token.type())) {
                newline |= body.indexOf('\n') >= 0;
                continue;
            }
            result.add(new Significant(token.type().toString(), body, newline));
            newline = false;
        }
        return result;
    }

    static void verify(List<String> args) throws IOException {
        if (args.size() < 2) {
            usage();
        }
        Path beforeRoot = Path.of(args.get(0));
        Path afterRoot = Path.of(args.get(1));
        int failures = 0;
        long tokens = 0;
        int files = 0;
        for (String file : args.subList(2, args.size())) {
            Path beforePath = beforeRoot.resolve(file);
            Path afterPath = afterRoot.resolve(file);
            if (!Files.exists(beforePath) || !Files.exists(afterPath)) {
                System.out.println("MISSING\t" + file);
                failures++;
                continue;
            }
            List<Significant> before = significant(read(beforePath));
            List<Significant> after = significant(read(afterPath));
            files++;
            tokens += before.size();
            if (!before.equals(after)) {
                failures++;
                int at = 0;
                while (at < before.size() && at < after.size() && before.get(at).equals(after.get(at))) {
                    at++;
                }
                System.out.println("DIFFERENT\t" + file + "\tfirst difference at significant token " + at + ": "
                    + (at < before.size() ? before.get(at) : "<end>") + " vs " + (at < after.size() ? after.get(at) : "<end>"));
            }
        }
        System.out.println("verified " + files + " files, " + tokens + " significant tokens, " + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }

    static void check(List<String> args) throws IOException {
        Options options = options(args);
        int found = 0;
        for (String file : options.files()) {
            String text = read(Path.of(file));
            List<Token> tokens = lex(text);
            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                if (isComment(token.type()) && !keep(options, file, tokens, i, text)) {
                    found++;
                    String firstLine = text.substring(token.start(), token.end()).lines().findFirst().orElse("");
                    System.out.println(file + ":" + lineOf(text, token.start()) + ": comment: " + firstLine);
                }
            }
        }
        System.out.println(found + " comments in " + options.files().size() + " files");
        if (found > 0) {
            System.exit(1);
        }
    }
}
