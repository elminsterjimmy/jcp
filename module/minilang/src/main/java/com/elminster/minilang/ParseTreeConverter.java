package com.elminster.minilang;

import com.elminster.jcp.ast.*;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.literal.*;
import com.elminster.jcp.ast.expression.operation.*;
import com.elminster.jcp.ast.expression.operation.operator.AssignmentOperator;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.expression.base.ModuleFunctionCallExpression;
import com.elminster.jcp.ast.expression.StaticMethodCallExpression;
import com.elminster.jcp.ast.expression.base.VariableExpression;
import com.elminster.jcp.ast.statement.*;
import com.elminster.jcp.ast.statement.control.*;
import com.elminster.jcp.ast.statement.declaration.*;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.data.DataType;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

/**
 * Converts ANTLR parse tree to JCP AST.
 *
 * <p>This class demonstrates the core pattern for integrating a custom DSL with JCP:
 * <ol>
 *   <li>Parse source code using ANTLR grammar</li>
 *   <li>Convert parse tree nodes to JCP AST nodes via visitor pattern</li>
 *   <li>Attach source locations to all nodes for debugging</li>
 *   <li>Execute using JCP's eval or compile modes</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>{@code
 * String source = "let x: int = 10\nprint(x)";
 * ParseTreeConverter converter = new ParseTreeConverter("test.ml", source);
 * Block program = converter.parse();
 *
 * // Execute in eval mode
 * EvalContext ctx = new RootEvalContext();
 * new EvalVisitor(ctx).visit(program);
 *
 * // Or compile to bytecode
 * JcpCompiler compiler = new JcpCompiler();
 * Class<?> clazz = compiler.compileAndLoad(program, "Test");
 * }</pre>
 */
public class ParseTreeConverter extends MiniLangBaseVisitor<Node> {

    private final String sourceFile;
    private final String[] sourceLines;

    /**
     * Creates a new converter for the given source file and content.
     *
     * @param sourceFile the source file path (for error messages)
     * @param sourceCode the complete source code
     */
    public ParseTreeConverter(String sourceFile, String sourceCode) {
        this.sourceFile = sourceFile;
        this.sourceLines = sourceCode.split("\n");
    }

    /**
     * Parses MiniLang source code into a JCP AST Block.
     *
     * @param sourceCode the source code to parse
     * @return the root Block node representing the program
     * @throws RuntimeException if parsing fails
     */
    public Block parse(String sourceCode) {
        CharStream input = CharStreams.fromString(sourceCode);
        MiniLangLexer lexer = new MiniLangLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        MiniLangParser parser = new MiniLangParser(tokens);

        // Remove default error listeners and add custom ones for both lexer and parser
        lexer.removeErrorListeners();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                  int line, int charPositionInColumn, String msg,
                                  RecognitionException e) {
                throw new RuntimeException(
                    String.format("Syntax error at %s:%d:%d - %s",
                                sourceFile, line, charPositionInColumn + 1, msg));
            }
        });

        parser.removeErrorListeners();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                  int line, int charPositionInColumn, String msg,
                                  RecognitionException e) {
                throw new RuntimeException(
                    String.format("Syntax error at %s:%d:%d - %s",
                                sourceFile, line, charPositionInColumn + 1, msg));
            }
        });

        MiniLangParser.ProgramContext tree = parser.program();
        return (Block) visit(tree);
    }

    /**
     * Parses a program which is just a sequence of statements wrapped in a Block.
     */
    @Override
    public Block visitProgram(MiniLangParser.ProgramContext ctx) {
        List<Statement> statements = new ArrayList<>();
        for (MiniLangParser.StatementContext stmtCtx : ctx.statement()) {
            Statement stmt = (Statement) visit(stmtCtx);
            if (stmt != null) {
                statements.add(stmt);
            }
        }
        Block block = new BlockImpl(statements.toArray(new Statement[0]));
        attachLocation(block, ctx);
        return block;
    }

    // ===== STATEMENTS =====

    @Override
    public Statement visitLetStmt(MiniLangParser.LetStmtContext ctx) {
        MiniLangParser.LetStatementContext let = ctx.letStatement();
        String varName = let.ID().getText();
        DataType type = resolveType(let.typeAnnotation().getText());
        Expression init = (Expression) visit(let.expression());

        VariableDeclaration decl = new VariableDeclarationImpl(varName, type, init);
        attachLocation(decl, ctx);
        return decl;
    }

    @Override
    public Statement visitIfStmt(MiniLangParser.IfStmtContext ctx) {
        MiniLangParser.IfStatementContext ifStmt = ctx.ifStatement();
        Expression condition = (Expression) visit(ifStmt.expression());
        Block thenBlock = (Block) visit(ifStmt.block(0));
        Block elseBlock = ifStmt.ELSE() != null ? (Block) visit(ifStmt.block(1)) : null;

        IfElseStatement stmt = new IfElseStatement(thenBlock, elseBlock, condition);
        attachLocation(stmt, ctx);
        return stmt;
    }

    @Override
    public Statement visitWhileStmt(MiniLangParser.WhileStmtContext ctx) {
        MiniLangParser.WhileStatementContext whileStmt = ctx.whileStatement();
        Expression condition = (Expression) visit(whileStmt.expression());
        Block body = (Block) visit(whileStmt.block());

        WhileStatement stmt = new WhileStatement(condition, body);
        attachLocation(stmt, ctx);
        return stmt;
    }

    @Override
    public Statement visitReturnStmt(MiniLangParser.ReturnStmtContext ctx) {
        Expression value = (Expression) visit(ctx.returnStatement().expression());
        ReturnStatement stmt = new ReturnStatement(value);
        attachLocation(stmt, ctx);
        return stmt;
    }

    @Override
    public Statement visitBreakStmt(MiniLangParser.BreakStmtContext ctx) {
        BreakStatement stmt = new BreakStatement();
        attachLocation(stmt, ctx);
        return stmt;
    }

    @Override
    public Statement visitContinueStmt(MiniLangParser.ContinueStmtContext ctx) {
        ContinueStatement stmt = new ContinueStatement();
        attachLocation(stmt, ctx);
        return stmt;
    }

    @Override
    public Statement visitFuncDecl(MiniLangParser.FuncDeclContext ctx) {
        MiniLangParser.FunctionDeclContext funcDecl = ctx.functionDecl();
        String funcName = funcDecl.ID().getText();

        // Parse parameters
        ParameterDef[] params;
        if (funcDecl.parameterList() != null) {
            List<MiniLangParser.ParameterContext> paramCtxs = funcDecl.parameterList().parameter();
            params = new ParameterDef[paramCtxs.size()];
            for (int i = 0; i < paramCtxs.size(); i++) {
                MiniLangParser.ParameterContext paramCtx = paramCtxs.get(i);
                String paramName = paramCtx.ID().getText();
                DataType paramType = resolveType(paramCtx.typeAnnotation().getText());
                params[i] = ParameterDef.of(paramName, paramType);
            }
        } else {
            params = new ParameterDef[0];
        }

        // Parse return type
        DataType returnType = SystemDataType.VOID;
        if (funcDecl.ARROW() != null) {
            returnType = resolveType(funcDecl.typeAnnotation().getText());
        }

        // Parse body
        Block body = (Block) visit(funcDecl.block());

        FunctionDeclaration func = new FunctionDeclarationImpl(
            Identifier.fromName(funcName), returnType, params, body);
        attachLocation(func, ctx);
        return func;
    }

    @Override
    public Statement visitExprStmt(MiniLangParser.ExprStmtContext ctx) {
        Expression expr = (Expression) visit(ctx.expressionStmt().expression());
        ExpressionStatement stmt = new ExpressionStatement(expr);
        attachLocation(stmt, ctx);
        return stmt;
    }

    @Override
    public Statement visitEmptyStmt(MiniLangParser.EmptyStmtContext ctx) {
        // Empty statement (blank line) - return null to filter it out
        return null;
    }

    @Override
    public Block visitBlock(MiniLangParser.BlockContext ctx) {
        List<Statement> statements = new ArrayList<>();
        for (MiniLangParser.StatementContext stmtCtx : ctx.statement()) {
            Statement stmt = (Statement) visit(stmtCtx);
            if (stmt != null) {
                statements.add(stmt);
            }
        }
        Block block = new BlockImpl(statements.toArray(new Statement[0]));
        attachLocation(block, ctx);
        return block;
    }

    // ===== EXPRESSIONS =====

    @Override
    public Expression visitFunctionCall(MiniLangParser.FunctionCallContext ctx) {
        Expression funcExpr = (Expression) visit(ctx.expression());

        // Get function name - the expression should be an identifier
        String funcName;
        if (funcExpr instanceof VariableExpression) {
            funcName = ((VariableExpression) funcExpr).getId().getId();
        } else if (funcExpr instanceof IdentifierExpression) {
            funcName = ((IdentifierExpression) funcExpr).getId();
        } else {
            throw new RuntimeException("Function call on non-identifier: " + funcExpr);
        }

        // Parse arguments
        Expression[] args;
        if (ctx.argumentList() != null) {
            List<MiniLangParser.ExpressionContext> argCtxs = ctx.argumentList().expression();
            args = new Expression[argCtxs.size()];
            for (int i = 0; i < argCtxs.size(); i++) {
                args[i] = (Expression) visit(argCtxs.get(i));
            }
        } else {
            args = new Expression[0];
        }

        // All functions use FunctionCallExpression
        // JCP's function lookup system will resolve dotted names like "Assertions.assertTrue"
        FunctionCallExpression call = new FunctionCallExpression(Identifier.fromName(funcName), args);
        attachLocation(call, ctx);
        return call;
    }

    @Override
    public Expression visitMemberAccess(MiniLangParser.MemberAccessContext ctx) {
        Expression object = (Expression) visit(ctx.expression());
        String member = ctx.ID().getText();

        // Get the base identifier name
        String baseName;
        if (object instanceof VariableExpression) {
            baseName = ((VariableExpression) object).getId().getId();
        } else if (object instanceof IdentifierExpression) {
            baseName = ((IdentifierExpression) object).getId();
        } else {
            throw new RuntimeException("Member access on non-identifier: " + object);
        }

        // Build the dotted name
        String dottedName = baseName + "." + member;

        // Return as an identifier that can be used in function calls
        IdentifierExpression result = new IdentifierExpression(dottedName);
        attachLocation(result, ctx);
        return result;
    }

    @Override
    public Expression visitMultDiv(MiniLangParser.MultDivContext ctx) {
        Expression left = (Expression) visit(ctx.expression(0));
        Expression right = (Expression) visit(ctx.expression(1));
        String op = ctx.op.getText();

        Expression result;
        switch (op) {
            case "*":
                result = new Multi(left, right);
                break;
            case "/":
                result = new Divide(left, right);
                break;
            case "%":
                result = new Mod(left, right);
                break;
            default:
                throw new RuntimeException("Unknown operator: " + op);
        }
        attachLocation(result, ctx);
        return result;
    }

    @Override
    public Expression visitAddSub(MiniLangParser.AddSubContext ctx) {
        Expression left = (Expression) visit(ctx.expression(0));
        Expression right = (Expression) visit(ctx.expression(1));
        String op = ctx.op.getText();

        Expression result;
        if (op.equals("+")) {
            result = new Plus(left, right);
        } else {
            result = new Minus(left, right);
        }
        attachLocation(result, ctx);
        return result;
    }

    @Override
    public Expression visitComparison(MiniLangParser.ComparisonContext ctx) {
        Expression left = (Expression) visit(ctx.expression(0));
        Expression right = (Expression) visit(ctx.expression(1));
        String op = ctx.op.getText();

        Expression result;
        switch (op) {
            case "<":
                result = new LessThan(left, right);
                break;
            case ">":
                result = new GreaterThan(left, right);
                break;
            case "<=":
                result = new LessThanEqual(left, right);
                break;
            case ">=":
                result = new GreaterThanEqual(left, right);
                break;
            default:
                throw new RuntimeException("Unknown comparison operator: " + op);
        }
        attachLocation(result, ctx);
        return result;
    }

    @Override
    public Expression visitEquality(MiniLangParser.EqualityContext ctx) {
        Expression left = (Expression) visit(ctx.expression(0));
        Expression right = (Expression) visit(ctx.expression(1));
        String op = ctx.op.getText();

        Expression result;
        if (op.equals("==")) {
            result = new Equal(left, right);
        } else {
            result = new NotEqual(left, right);
        }
        attachLocation(result, ctx);
        return result;
    }

    @Override
    public Expression visitLogicalAnd(MiniLangParser.LogicalAndContext ctx) {
        Expression left = (Expression) visit(ctx.expression(0));
        Expression right = (Expression) visit(ctx.expression(1));
        LogicalAndExpression result = new LogicalAndExpression(left, right);
        attachLocation(result, ctx);
        return result;
    }

    @Override
    public Expression visitLogicalOr(MiniLangParser.LogicalOrContext ctx) {
        Expression left = (Expression) visit(ctx.expression(0));
        Expression right = (Expression) visit(ctx.expression(1));
        LogicalOrExpression result = new LogicalOrExpression(left, right);
        attachLocation(result, ctx);
        return result;
    }

    @Override
    public Expression visitAssignment(MiniLangParser.AssignmentContext ctx) {
        String varName = ctx.ID().getText();
        Expression value = (Expression) visit(ctx.expression());
        AssignmentExpression result = new AssignmentExpression(
            Identifier.fromName(varName), AssignmentOperator.ASSIGNMENT, value);
        attachLocation(result, ctx);
        return result;
    }

    @Override
    public Expression visitLogicalNot(MiniLangParser.LogicalNotContext ctx) {
        Expression operand = (Expression) visit(ctx.expression());
        LogicalNotExpression result = new LogicalNotExpression(operand);
        attachLocation(result, ctx);
        return result;
    }

    @Override
    public Expression visitNegate(MiniLangParser.NegateContext ctx) {
        Expression operand = (Expression) visit(ctx.expression());
        // Unary minus - need to create a negation expression
        // For now, we'll use (0 - expr) pattern
        Expression zero = LiteralExpression.of(IntLiteral.of(0));
        Expression result = new Minus(zero, operand);
        attachLocation(result, ctx);
        return result;
    }

    @Override
    public Expression visitParens(MiniLangParser.ParensContext ctx) {
        return (Expression) visit(ctx.expression());
    }

    @Override
    public Expression visitIdentifier(MiniLangParser.IdentifierContext ctx) {
        String name = ctx.ID().getText();
        VariableExpression result = VariableExpression.of(name);
        attachLocation(result, ctx);
        return result;
    }

    @Override
    public Expression visitLiteralExpr(MiniLangParser.LiteralExprContext ctx) {
        return (Expression) visit(ctx.literal());
    }

    // ===== LITERALS =====

    @Override
    public Expression visitIntLiteral(MiniLangParser.IntLiteralContext ctx) {
        int value = Integer.parseInt(ctx.INT_LITERAL().getText());
        LiteralExpression result = LiteralExpression.of(IntLiteral.of(value));
        attachLocation(result, ctx);
        return result;
    }

    @Override
    public Expression visitDoubleLiteral(MiniLangParser.DoubleLiteralContext ctx) {
        double value = Double.parseDouble(ctx.DOUBLE_LITERAL().getText());
        LiteralExpression result = LiteralExpression.of(DoubleLiteral.of(value));
        attachLocation(result, ctx);
        return result;
    }

    @Override
    public Expression visitStringLiteral(MiniLangParser.StringLiteralContext ctx) {
        String text = ctx.STRING_LITERAL().getText();
        // Remove quotes
        String value = text.substring(1, text.length() - 1);
        LiteralExpression result = LiteralExpression.of(StringLiteral.of(value));
        attachLocation(result, ctx);
        return result;
    }

    @Override
    public Expression visitBooleanLiteral(MiniLangParser.BooleanLiteralContext ctx) {
        boolean value = Boolean.parseBoolean(ctx.BOOLEAN_LITERAL().getText());
        LiteralExpression result = LiteralExpression.of(BooleanLiteral.of(value));
        attachLocation(result, ctx);
        return result;
    }

    // ===== HELPER METHODS =====

    /**
     * Resolves a type annotation string to a JCP DataType.
     * This is inline type resolution as per the simplified plan.
     */
    private DataType resolveType(String typeName) {
        switch (typeName.toLowerCase()) {
            case "int":
                return SystemDataType.INT;
            case "double":
                return SystemDataType.DOUBLE;
            case "boolean":
                return SystemDataType.BOOLEAN;
            case "string":
                return SystemDataType.STRING;
            case "void":
                return SystemDataType.VOID;
            default:
                return SystemDataType.ANY;  // Fallback for unknown types
        }
    }

    /**
     * Attaches source location to an AST node from ANTLR parse tree context.
     * ANTLR uses 0-based column indexing; JCP uses 1-based.
     */
    private void attachLocation(Node node, ParserRuleContext ctx) {
        if (ctx == null || !(node instanceof AbstractNode)) {
            return;
        }

        Token start = ctx.getStart();
        Token stop = ctx.getStop() != null ? ctx.getStop() : start;

        // Get source line content (1-based line numbers)
        String sourceLineContent = null;
        if (start.getLine() <= sourceLines.length) {
            sourceLineContent = sourceLines[start.getLine() - 1];
        }

        SourceLocation location = SourceLocation.span(
            sourceFile,
            start.getLine(),
            start.getCharPositionInLine() + 1,  // ANTLR is 0-based, convert to 1-based
            stop.getLine(),
            stop.getCharPositionInLine() + stop.getText().length(),
            sourceLineContent
        );

        ((AbstractNode) node).setLocation(location);
    }
}
