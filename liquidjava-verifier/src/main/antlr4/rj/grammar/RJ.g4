grammar RJ;


prog: start EOF;
start:
		pred	#startPred
	|	alias	#startAlias
	|	ghost	#startGhost
	;

//----------------------- Predicates -----------------------

pred:
		'-' pred					#opMinus
	|	'!' pred					#opNot
	|	'(' pred ')'				#predGroup
	|	literalExpression			#opLiteral
	|	pred MULOP pred				#opArith
	|	pred ('+'|'-') pred			#opArith
	|	pred BOOLOP pred			#expBool
	|	pred '&&' pred 				#predLogic
	|	pred '||' pred 				#predLogic
	|	<assoc=right> pred '-->' pred #predLogic
	|	<assoc=right> pred '?' pred ':' pred #ite
	;

literalExpression:
		literal						#lit
	| 	ID 							#var
	|	functionCall				#invocation
	|	enumerate					#enum
	;

functionCall:
 		ghostCall
 	|	aliasCall
	|	dotCall
	;

dotCall:
		OBJECT_TYPE '(' args? ')'
	| 	ID '(' args? ')' '.' ID '(' args? ')';

ghostCall:
 	ID '(' args? ')';

aliasCall:
	ID_UPPER '(' args? ')';

enumerate: 
	ENUM;

args:	pred (',' pred)* ;


literal:
		BOOL
	|	STRING
	|	INT
	|	REAL;

//----------------------- Declarations -----------------------

alias:
	ID_UPPER '(' argDeclID ')' '{' pred '}';

ghost:
	type ID ('(' argDecl? ')')?;

argDecl:
	type ID? (',' argDecl)?;

argDeclID:
	type ID (',' argDeclID)?;

type:
		'int'
	|	'double'
	|	'float'
	|	'boolean'
	|	ID_UPPER
	|	OBJECT_TYPE
	|	type '[]';


BOOLOP	 : '=='|'!='|'>='|'>'|'<='|'<';
MULOP   : '*'|'/'|'%';

BOOL    : 'true' | 'false';
ID_UPPER: ([A-Z][a-zA-Z0-9]*);
ENUM: [A-Z][a-zA-Z0-9_]* '.' [A-Z][a-zA-Z0-9_]*;
OBJECT_TYPE:
		  (([a-zA-Z][a-zA-Z0-9]+) ('.' [a-zA-Z][a-zA-Z0-9]*)+);
ID     	: '#'*[a-zA-Z_][a-zA-Z0-9_#]*;
STRING  : '"'(~["])*'"';
INT     : 	(([0-9]+) |	([0-9]+('_'[0-9]+)*));
REAL   	: (([0-9]+('.'[0-9]+)?) | '.'[0-9]+);

WS		:  (' '|'\t'|'\n'|'\r')+ -> channel(HIDDEN);
