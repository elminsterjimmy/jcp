// MiniLang Grammar
// A minimalist DSL demonstrating JCP integration patterns
grammar MiniLang;

// ===== PROGRAM STRUCTURE =====

// A program is a sequence of statements
program
    : NEWLINE* statement* EOF
    ;

// ===== STATEMENTS =====

statement
    : letStatement          # LetStmt        // Variable declaration: let x: int = 10
    | ifStatement           # IfStmt         // Conditional: if condition { ... } else { ... }
    | whileStatement        # WhileStmt      // Loop: while condition { ... }
    | returnStatement       # ReturnStmt     // Return from function: return expr
    | breakStatement        # BreakStmt      // Break from loop: break
    | continueStatement     # ContinueStmt   // Continue loop: continue
    | functionDecl          # FuncDecl       // Function: func name(params) -> type { ... }
    | expressionStmt        # ExprStmt       // Expression statement: x = 5, print(x)
    | NEWLINE               # EmptyStmt      // Empty statement (blank line)
    ;

// Variable declaration with explicit type annotation
letStatement
    : LET ID COLON typeAnnotation ASSIGN expression NEWLINE
    ;

// If statement with optional else
ifStatement
    : IF expression block (ELSE block)?
    ;

// While loop
whileStatement
    : WHILE expression block
    ;

// Return statement
returnStatement
    : RETURN expression NEWLINE
    ;

// Break statement
breakStatement
    : BREAK NEWLINE
    ;

// Continue statement
continueStatement
    : CONTINUE NEWLINE
    ;

// Function declaration
functionDecl
    : FUNC ID LPAREN parameterList? RPAREN (ARROW typeAnnotation)? block
    ;

// Parameter list: name: type, name: type
parameterList
    : parameter (COMMA parameter)*
    ;

parameter
    : ID COLON typeAnnotation
    ;

// Expression as statement
expressionStmt
    : expression NEWLINE
    ;

// Code block: { statements }
block
    : LBRACE NEWLINE* statement* RBRACE
    ;

// ===== EXPRESSIONS =====
// Operator precedence (highest to lowest):
// 1. Function calls, literals, identifiers, parentheses
// 2. Multiplication, division, modulo
// 3. Addition, subtraction
// 4. Comparison operators
// 5. Equality operators
// 6. Logical AND
// 7. Logical OR
// 8. Assignment

expression
    : expression LPAREN argumentList? RPAREN            # FunctionCall    // f(a, b)
    | expression op=(MULT | DIV | MOD) expression       # MultDiv         // a * b
    | expression op=(PLUS | MINUS) expression           # AddSub          // a + b
    | expression op=(LT | GT | LE | GE) expression      # Comparison      // a < b
    | expression op=(EQ | NE) expression                # Equality        // a == b
    | expression AND expression                         # LogicalAnd      // a && b
    | expression OR expression                          # LogicalOr       // a || b
    | ID ASSIGN expression                              # Assignment      // x = 5
    | NOT expression                                    # LogicalNot      // !x
    | MINUS expression                                  # Negate          // -x
    | literal                                           # LiteralExpr     // 42, "hello", true
    | ID                                                # Identifier      // x
    | LPAREN expression RPAREN                          # Parens          // (expr)
    ;

// Function call arguments
argumentList
    : expression (COMMA expression)*
    ;

// ===== LITERALS =====

literal
    : INT_LITERAL           # IntLiteral
    | DOUBLE_LITERAL        # DoubleLiteral
    | STRING_LITERAL        # StringLiteral
    | BOOLEAN_LITERAL       # BooleanLiteral
    ;

// ===== TYPE ANNOTATIONS =====

typeAnnotation
    : INT               // int
    | DOUBLE            // double
    | BOOLEAN           // boolean
    | STRING            // string
    | VOID              // void
    ;

// ===== LEXER RULES =====
// Note: Keywords MUST come before ID to prevent them being matched as identifiers

// Keywords (must come before ID)
LET         : 'let' ;
FUNC        : 'func' ;
IF          : 'if' ;
ELSE        : 'else' ;
WHILE       : 'while' ;
RETURN      : 'return' ;
BREAK       : 'break' ;
CONTINUE    : 'continue' ;

// Type keywords
INT         : 'int' ;
DOUBLE      : 'double' ;
BOOLEAN     : 'boolean' ;
STRING      : 'string' ;
VOID        : 'void' ;

// Boolean literals
BOOLEAN_LITERAL : 'true' | 'false' ;

// Operators
PLUS        : '+' ;
MINUS       : '-' ;
MULT        : '*' ;
DIV         : '/' ;
MOD         : '%' ;
ASSIGN      : '=' ;
EQ          : '==' ;
NE          : '!=' ;
LT          : '<' ;
GT          : '>' ;
LE          : '<=' ;
GE          : '>=' ;
AND         : '&&' ;
OR          : '||' ;
NOT         : '!' ;

// Delimiters
LPAREN      : '(' ;
RPAREN      : ')' ;
LBRACE      : '{' ;
RBRACE      : '}' ;
COMMA       : ',' ;
COLON       : ':' ;
ARROW       : '->' ;

// Newline - statement terminator
NEWLINE     : '\r'? '\n' | '\r' ;

// Identifiers (must come after keywords)
ID          : [a-zA-Z_][a-zA-Z_0-9]* ;

// Literals
INT_LITERAL     : [0-9]+ ;
DOUBLE_LITERAL  : [0-9]+ '.' [0-9]+ ;
STRING_LITERAL  : '"' (~["\r\n])* '"' ;

// Comments and whitespace
COMMENT     : '#' ~[\r\n]* -> skip ;
WS          : [ \t]+ -> skip ;
