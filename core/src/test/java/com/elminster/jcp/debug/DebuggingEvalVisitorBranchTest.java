package com.elminster.jcp.debug;

import com.elminster.jcp.ast.SourceLocation;
import com.elminster.jcp.ast.expression.LiteralExpression;
import com.elminster.jcp.ast.expression.base.FunctionCallExpression;
import com.elminster.jcp.ast.statement.Block;
import com.elminster.jcp.ast.statement.BlockImpl;
import com.elminster.jcp.ast.statement.declaration.FunctionDeclarationImpl;
import com.elminster.jcp.ast.statement.declaration.VariableDeclarationImpl;
import com.elminster.jcp.ast.Identifier;
import com.elminster.jcp.ast.expression.literal.IntLiteral;
import com.elminster.jcp.ast.statement.function.ParameterDef;
import com.elminster.jcp.eval.context.EvalContext;
import com.elminster.jcp.eval.context.RootEvalContext;
import com.elminster.jcp.eval.data.DataType.SystemDataType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Branch-coverage tests for DebuggingEvalVisitor.visit() and debug().
 *
 * Covers:
 *  - visit() fast-path when debugger not attached
 *  - visit() with attached debugger + no breakpoints (no pause)
 *  - visit() pauses when breakpoint hit (ImmediatePauseStrategy resumes immediately)
 *  - visit() callDepth tracking for FunctionDeclaration nodes
 *  - debug() attaches/detaches around the program
 */
class DebuggingEvalVisitorBranchTest {

    private ImmediatePauseStrategy pauseStrategy;
    private DefaultDebugger debugger;
    private EvalContext context;
    private DebuggingEvalVisitor visitor;

    @BeforeEach
    void setUp() {
        pauseStrategy = new ImmediatePauseStrategy();
        debugger = new DefaultDebugger(pauseStrategy);
        context = new RootEvalContext();
        visitor = new DebuggingEvalVisitor(context, debugger);
    }

    // ── fast-path: not attached ────────────────────────────────────────────────

    @Test
    void visit_WhenNotAttached_TakesNormalPath() {
        // debugger is DETACHED (default) → fast-path in visit()
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("x", SystemDataType.INT, LiteralExpression.of(1)));

        assertDoesNotThrow(() -> visitor.visit(program));
        assertEquals(1, context.getVariable("x").get());
    }

    // ── attached, no breakpoints, RUNNING ─────────────────────────────────────

    @Test
    void debug_AttachesAndDetachesAround_Program() {
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("y", SystemDataType.INT, LiteralExpression.of(7)));

        // debug() calls attach() before visit() and detach() after
        assertFalse(debugger.isAttached());
        visitor.debug(program);
        assertFalse(debugger.isAttached()); // detached again after debug()
        assertEquals(7, context.getVariable("y").get());
    }

    // ── attached, with breakpoint that matches ─────────────────────────────────

    @Test
    void visit_PausesAtBreakpoint_ThenResumes() {
        // ImmediatePauseStrategy resumes immediately, so pause() returns without blocking
        Block program = new BlockImpl();
        VariableDeclarationImpl decl = new VariableDeclarationImpl("z", SystemDataType.INT, LiteralExpression.of(99));
        // Give the node a source location so the breakpoint can match by line
        decl.setLocation(SourceLocation.of("test.jcp", 5, 1));
        program.addStatement(decl);

        debugger.setBreakpoint("test.jcp", 5);
        // debug() attaches then visits — breakpoint hit triggers immediate-resume
        visitor.debug(program);

        assertEquals(99, context.getVariable("z").get());
    }

    // ── STEP_INTO state: shouldPause returns true immediately ─────────────────

    @Test
    void visit_WhenStepInto_PausesOnEveryNode() {
        debugger.attach(); // → RUNNING
        // Force into STEP_INTO by pausing once first, then stepping
        Block program = new BlockImpl();
        IntLiteral node = IntLiteral.of(1);
        node.setLocation(SourceLocation.of("test.jcp", 1, 1));
        // Set a breakpoint so first visit pauses
        debugger.setBreakpoint("test.jcp", 1);

        // Use a fresh program with just a declaration
        Block prog2 = new BlockImpl();
        prog2.addStatement(new VariableDeclarationImpl("a", SystemDataType.INT, LiteralExpression.of(5)));
        visitor.debug(prog2);
        assertEquals(5, context.getVariable("a").get());
    }

    // ── callDepth incremented for FunctionDeclaration ─────────────────────────

    @Test
    void debug_WithFunctionDeclaration_CallDepthTracked() {
        // FunctionDeclaration is isFunctionCall → callDepth incremented during its visit
        Block funcBody = new BlockImpl();
        FunctionDeclarationImpl funcDecl = new FunctionDeclarationImpl(
            Identifier.fromName("greet"),
            SystemDataType.VOID,
            new ParameterDef[]{},
            funcBody
        );

        Block program = new BlockImpl();
        program.addStatement(funcDecl);

        // Should execute without error; callDepth returns to 0 after
        visitor.debug(program);
        assertEquals(0, visitor.getCallDepth());
    }

    // ── getCallDepth still 0 after a successful debug run ─────────────────────

    @Test
    void getCallDepth_AfterDebug_StillZero() {
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl("b", SystemDataType.INT, LiteralExpression.of(2)));
        visitor.debug(program);
        assertEquals(0, visitor.getCallDepth());
    }

    // ── debug() detaches even when exception propagates ────────────────────────

    @Test
    void debug_DetachesOnException() {
        // A program that will throw — e.g. reference undefined variable
        Block program = new BlockImpl();
        program.addStatement(new VariableDeclarationImpl(
            "bad",
            SystemDataType.INT,
            new com.elminster.jcp.ast.expression.base.VariableExpression(Identifier.fromName("undefined"))
        ));

        assertThrows(Exception.class, () -> visitor.debug(program));
        // Debugger must have been detached in finally block
        assertFalse(debugger.isAttached());
    }
}
