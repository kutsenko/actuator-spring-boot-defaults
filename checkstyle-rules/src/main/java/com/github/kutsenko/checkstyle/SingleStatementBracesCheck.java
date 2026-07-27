package com.github.kutsenko.checkstyle;

import com.puppycrawl.tools.checkstyle.api.AbstractCheck;
import com.puppycrawl.tools.checkstyle.api.DetailAST;
import com.puppycrawl.tools.checkstyle.api.TokenTypes;

/**
 * Enforces arc42 §8.10.2 — "No curly braces on single-statement blocks". Flags an
 * {@code if}/{@code else}/{@code for}/{@code while}/{@code do} body that is a block
 * (<code>{ ... }</code>) containing exactly one statement.
 *
 * <p>Two deliberate carve-outs keep the rule aligned with its intent (kill trivial
 * one-liner braces, not documented or footgun-prone ones):
 *
 * <ul>
 *   <li><b>Dangling-else safety:</b> when the single statement is itself a control-flow
 *       construct ({@code if}/{@code for}/{@code while}/{@code do}/{@code switch}/
 *       {@code try}/{@code synchronized}), the braces are <em>allowed</em> — stripping
 *       them there can change {@code else} binding (the classic dangling-else footgun).</li>
 *   <li><b>Documented blocks:</b> when the block also contains a comment, the braces are
 *       <em>allowed</em>. A statement carrying an explanatory comment is not a trivial
 *       one-liner, and going braceless would wedge the comment between the condition and
 *       its body — less readable and a foot-gun for the next edit. This mirrors the manual
 *       arc42 sweep, which kept exactly these braced.</li>
 * </ul>
 *
 * <p>Multi-statement and empty blocks are never flagged. Comment nodes are required
 * ({@link #isCommentNodesRequired()}) so the documented-block carve-out can see them.
 * The statement-terminating {@code SEMI} is ignored when counting, so expression
 * statements ({@code foo();}, {@code x--;}) count as one statement just like a
 * {@code return}/{@code throw} — otherwise the trailing semicolon would masquerade as
 * a second statement and the block would slip through unflagged.
 */
public class SingleStatementBracesCheck extends AbstractCheck {

    private static final int[] CONTROL_FLOW = {
        TokenTypes.LITERAL_IF, TokenTypes.LITERAL_FOR, TokenTypes.LITERAL_WHILE,
        TokenTypes.LITERAL_DO, TokenTypes.LITERAL_SWITCH, TokenTypes.LITERAL_TRY,
        TokenTypes.LITERAL_SYNCHRONIZED,
    };

    @Override
    public int[] getDefaultTokens() {
        return new int[]{
            TokenTypes.LITERAL_IF, TokenTypes.LITERAL_ELSE,
            TokenTypes.LITERAL_FOR, TokenTypes.LITERAL_WHILE, TokenTypes.LITERAL_DO,
        };
    }

    @Override
    public int[] getAcceptableTokens() {
        return getDefaultTokens();
    }

    @Override
    public int[] getRequiredTokens() {
        return getDefaultTokens();
    }

    @Override
    public boolean isCommentNodesRequired() {
        return true; // documented-block carve-out inspects comment nodes inside the block
    }

    @Override
    public void visitToken(DetailAST ast) {
        DetailAST block = ast.findFirstToken(TokenTypes.SLIST);
        if (block == null)
            return; // already braceless (or "else if")
        if (containsComment(block))
            return; // documented block — keep braces (see class doc)

        DetailAST onlyStatement = null;
        for (DetailAST child = block.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getType() == TokenTypes.RCURLY || child.getType() == TokenTypes.SEMI)
                continue; // block terminator / statement-terminating semicolon, not content
            if (onlyStatement != null)
                return; // two or more statements
            onlyStatement = child;
        }

        if (onlyStatement == null)
            return; // empty block
        if (isControlFlow(onlyStatement))
            return; // dangling-else safety: keep braces around nested control flow
        log(block, "Single-statement block must omit braces (arc42 §8.10.2).");
    }

    /**
     * Any comment anywhere inside the braces makes this a documented block. Checkstyle
     * attaches a comment either as a direct child of the {@code SLIST} (own-line comment)
     * or under the statement it precedes, so the scan must recurse — a shallow check would
     * carve out some documented blocks but not others (inconsistent, and it wedges the
     * comment between the condition and a braceless body).
     */
    private static boolean containsComment(DetailAST node) {
        for (DetailAST child = node.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (isComment(child) || containsComment(child))
                return true;
        }
        return false;
    }

    private static boolean isComment(DetailAST node) {
        return node.getType() == TokenTypes.SINGLE_LINE_COMMENT
                || node.getType() == TokenTypes.BLOCK_COMMENT_BEGIN;
    }

    private static boolean isControlFlow(DetailAST statement) {
        for (int type : CONTROL_FLOW) {
            if (statement.getType() == type)
                return true;
        }
        return false;
    }
}
