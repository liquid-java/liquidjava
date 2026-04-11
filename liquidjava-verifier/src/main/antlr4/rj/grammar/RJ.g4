grammar RJ;


prog: start | ;
start:
		pred	#startPred
	|	alias	#startAlias
	|	ghost	#startGhost
	;


pred:
		'(' pred ')'				#predGroup
	|	'!' pred					#predNegate
	|	pred LOGOP pred 			#predLogic
	|	pred '?' pred ':' pred		#ite
	|	exp							#predExp
	;

exp:
		'(' exp ')'					#expGroup
	|	exp BOOLOP exp				#expBool
	|	operand						#expOperand
	;

operand:
		literalExpression			#opLiteral
	|	operand ARITHOP	operand		#opArith
	|	operand '-'	operand			#opSub
	|	'-' operand					#opMinus
	|	'!' operand					#opNot
	|	'(' operand ')'				#opGroup
	;


literalExpression:
		'(' literalExpression ')'	#litGroup
	|	literal						#lit
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


LOGOP   : '&&'|'||'| '-->';
BOOLOP	 : '=='|'!='|'>='|'>'|'<='|'<';
ARITHOP : '+'|'*'|'/'|'%';//|'-';

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
