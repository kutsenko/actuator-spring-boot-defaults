package com.github.kutsenko.checkstyle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Drives the real Checkstyle engine over source snippets to prove
 * {@link SingleStatementBracesCheck} flags exactly the intended blocks. A wrong
 * carve-out here silently disables the arc42 §8.10.2 gate (or floods the build
 * with false positives), so each branch of the rule gets an explicit case.
 */
class SingleStatementBracesCheckTest {

    @TempDir
    Path tempDir;

    // ---- violations: a lone statement wrapped in braces ---------------------

    @Test
    void flagsBracedSingleStatementIf() throws Exception {
        assertOneViolation(wrapInMethod("""
                if (x > 0) {
                    return 1;
                }
                return 0;
                """));
    }

    @Test
    void flagsBracedElse() throws Exception {
        assertOneViolation(wrapInMethod("""
                if (x > 0)
                    return 1;
                else {
                    return 2;
                }
                """));
    }

    @Test
    void flagsBracedFor() throws Exception {
        assertOneViolation(wrapInMethod("""
                for (int i = 0; i < x; i++) {
                    sink(i);
                }
                return 0;
                """));
    }

    @Test
    void flagsBracedWhile() throws Exception {
        assertOneViolation(wrapInMethod("""
                while (x > 0) {
                    x--;
                }
                return 0;
                """));
    }

    @Test
    void flagsBracedDoWhile() throws Exception {
        assertOneViolation(wrapInMethod("""
                do {
                    x--;
                } while (x > 0);
                return 0;
                """));
    }

    // ---- clean: nothing to flag ---------------------------------------------

    @Test
    void allowsBracelessSingleStatement() throws Exception {
        assertNoViolations(wrapInMethod("""
                if (x > 0)
                    return 1;
                return 0;
                """));
    }

    @Test
    void allowsMultiStatementBlock() throws Exception {
        assertNoViolations(wrapInMethod("""
                if (x > 0) {
                    sink(x);
                    return 1;
                }
                return 0;
                """));
    }

    @Test
    void allowsEmptyBlock() throws Exception {
        assertNoViolations(wrapInMethod("""
                if (x > 0) {
                }
                return 0;
                """));
    }

    // ---- carve-out 1: dangling-else safety ----------------------------------

    @Test
    void allowsBracesAroundNestedIf() throws Exception {
        // Stripping the outer braces would rebind the else to the inner if.
        // (else is braceless here so it isn't itself a separate violation.)
        assertNoViolations(wrapInMethod("""
                if (x > 0) {
                    if (x > 5)
                        return 5;
                } else
                    return 2;
                return 0;
                """));
    }

    @Test
    void allowsBracesAroundNestedFor() throws Exception {
        assertNoViolations(wrapInMethod("""
                while (x > 0) {
                    for (int i = 0; i < x; i++)
                        sink(i);
                }
                return 0;
                """));
    }

    // ---- carve-out 2: documented block --------------------------------------

    @Test
    void allowsSingleStatementWithLineComment() throws Exception {
        assertNoViolations(wrapInMethod("""
                if (x > 0) {
                    // a documented branch is not a trivial one-liner
                    return 1;
                }
                return 0;
                """));
    }

    @Test
    void allowsSingleStatementWithBlockComment() throws Exception {
        assertNoViolations(wrapInMethod("""
                if (x > 0) {
                    /* a documented branch is not a trivial one-liner */
                    return 1;
                }
                return 0;
                """));
    }

    @Test
    void allowsExpressionStatementWithLeadingComment() throws Exception {
        // Checkstyle attaches this comment to the expression statement, not the SLIST —
        // the recursive carve-out must still find it (else the comment gets wedged
        // between the loop header and a braceless body).
        assertNoViolations(wrapInMethod("""
                for (int i = 0; i < x; i++) {
                    // discount grows with rank
                    sink(i);
                }
                return 0;
                """));
    }

    // ---- harness ------------------------------------------------------------

    /** Wraps a method body so the snippet is a compilable compilation unit. */
    private static String wrapInMethod(String body) {
        var indented = body.stripTrailing().indent(8);
        return "class Probe {\n"
                + "    void sink(int i) {\n"
                + "    }\n"
                + "    int run(int x) {\n"
                + indented
                + "\n    }\n"
                + "}\n";
    }

    private void assertNoViolations(String source) throws Exception {
        assertEquals(List.of(), runCheck(source), "expected no violations");
    }

    private void assertOneViolation(String source) throws Exception {
        var lines = runCheck(source);
        assertEquals(1, lines.size(), () -> "expected exactly one violation, got lines " + lines);
    }

    /** Runs the check over {@code source}; returns the 1-based lines it flagged. */
    private List<Integer> runCheck(String source) throws Exception {
        var file = tempDir.resolve("Probe.java");
        Files.writeString(file, source, StandardCharsets.UTF_8);

        var checkConfig = new DefaultConfiguration(SingleStatementBracesCheck.class.getName());
        var treeWalker = new DefaultConfiguration("TreeWalker");
        treeWalker.addChild(checkConfig);
        var root = new DefaultConfiguration("Checker");
        root.addProperty("charset", "UTF-8");
        root.addChild(treeWalker);

        var collector = new Collector();
        var checker = new Checker();
        checker.setModuleClassLoader(Thread.currentThread().getContextClassLoader());
        checker.configure(root);
        checker.addListener(collector);
        try {
            checker.process(List.of(file.toFile()));
        } finally {
            checker.destroy();
        }
        return collector.lines;
    }

    /** Captures only our rule's messages so unrelated audit noise cannot skew a case. */
    private static final class Collector implements AuditListener {
        private final List<Integer> lines = new ArrayList<>();

        @Override
        public void addError(AuditEvent event) {
            if (event.getMessage().contains("§8.10.2"))
                lines.add(event.getLine());
        }

        @Override
        public void addException(AuditEvent event, Throwable throwable) {
            throw new AssertionError("checkstyle raised an exception", throwable);
        }

        @Override
        public void auditStarted(AuditEvent event) {
        }

        @Override
        public void auditFinished(AuditEvent event) {
        }

        @Override
        public void fileStarted(AuditEvent event) {
        }

        @Override
        public void fileFinished(AuditEvent event) {
        }
    }

    @Test
    void harnessWritesReadableFile() throws IOException {
        // Guards the harness itself: the wrapped snippet must be non-empty on disk.
        var file = tempDir.resolve("probe-guard.java");
        Files.writeString(file, wrapInMethod("return 0;\n"), StandardCharsets.UTF_8);
        assertTrue(new File(file.toString()).length() > 0);
    }
}
