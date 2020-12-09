grammar MinAsm;

program
    : stmt*
    ;

block
    : stmt
    | OCURLY stmt* CCURLY
    | SCOL
    ;

stmt
    : expr SCOL
    | ifStmt
    | whileLoop
    | forLoop
    | labelStmt
    | gotoStmt
    | returnStmt
    | function
    | print
    | draw
    | drawflush
    | asm
    ;

expr
    : ID OPAREN (ID (COMMA ID)*)? CPAREN # funcCallExpr
    | OPAREN expr CPAREN                 # parenExpr
    | op=(MINUS | BNOT) expr             # unaryExpr // * no logical not?
    | <assoc=right> expr op=EXP expr     # binExpr
    | expr op=(MUL | DIV | MOD) expr     # binExpr
    | expr op=(PLUS | MINUS) expr        # binExpr
    | expr op=(LSHIFT | RSHIFT) expr     # binExpr
    | expr op=(LT | GT | LTE | GTE) expr # binExpr
    | expr op=(EQ | NEQ) expr            # binExpr
    | expr op=BAND expr                  # binExpr
    | expr op=BXOR expr                  # binExpr
    | expr op=BOR expr                   # binExpr
    | expr op=LAND expr                  # binExpr
//  | expr op=LOR expr                   # binExpr // * doesn't exist?
    | ID EQAS expr                       # assignExpr
    | ID op=(PEQAS | MEQAS) expr         # selfAssignExpr
    | atom                               # litExpr
    | sensor                             # sensorExpr
    ;

ifStmt
    : IF OPAREN expr CPAREN block            # nakedIf
    | IF OPAREN expr CPAREN block ELSE block # ifElse
    ;

whileLoop
    : WHILE OPAREN expr CPAREN block
    ;

forLoop
    : FOR OPAREN expr SCOL expr SCOL expr CPAREN block // *
    ;

labelStmt
    : LABEL ID COL
    ;

gotoStmt
    : GOTO ID SCOL
    ;

returnStmt
    : RETURN expr SCOL
    ;

function
    : FUNC ID OPAREN (ID (COMMA ID)*)? CPAREN block
    ;

print
    : PRINT expr (COMMA expr)* SCOL
    ;

draw
    : DRAW OPAREN atom COMMA atom COMMA atom COMMA atom COMMA atom COMMA atom COMMA atom CPAREN SCOL
    ;

drawflush
    : DRAWFLUSH OPAREN CPAREN SCOL
    ;

asm
    : ASM OPAREN STRING CPAREN SCOL
    ;

sensor
    : HASH ID DOT ID
    ;

atom
    : ID
    | NUMBER
    | STRING
    ;

// keywords
IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;
FOR : 'for' ;
LABEL : 'label' ;
GOTO : 'goto' ;
RETURN : 'return' ;

FUNC : 'function' ;

PRINT : 'print' ;
DRAW : 'draw' ;
DRAWFLUSH : 'drawflush' ;
RAND : 'rand' ;
ASM : 'asm' ;

// atoms

ID
    : ALPHA (ALPHA | DIGIT)*
    ;

NUMBER
    : DIGIT+ (DOT DIGIT*)?
    | DOT DIGIT+
    ;

STRING
    : QUOTE (~('"'))* QUOTE
    ;

// punctuation

SCOL : ';' ;
COL : ':' ;

QUOTE : '"' ;
OPAREN : '(' ;
CPAREN : ')' ;
OCURLY : '{' ;
CCURLY : '}' ;
COMMA : ',' ;

// operators

EQAS : '=' ;
PEQAS : '+=' ;
MEQAS : '-=' ;

PLUS : '+' ;
MINUS : '-' ;
MUL : '*' ;
DIV : '/';
MOD : '%' ;
EXP : '**' ;

LNOT : '!' ;
BNOT : '~' ;
LSHIFT : '<<' ;
RSHIFT : '>>' ;
LT : '<' ;
GT : '>' ;
LTE : '<=' ;
GTE : '>=' ;
EQ : '==' ;
NEQ : '!=' ;
BAND : '&' ;
BXOR : '^' ;
BOR : '|' ;
LAND : '&&' ;
LOR : '||' ;

HASH : '#' ;

// character classes

DIGIT : [0-9] ;
DOT : '.' ;
ALPHA : [_a-zA-Z] ;

// skip

WS : [ \n\t\r]+ -> skip ;
COMMENT
    : ( '//' ~[\r\n]* '\r'? '\n'
      | '/*' .*? '*/' ) -> skip
    ;