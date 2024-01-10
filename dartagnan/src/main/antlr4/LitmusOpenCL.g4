grammar LitmusOpenCL;

import C11Lexer, LitmusAssertions;

@header{
import com.dat3m.dartagnan.expression.op.*;
import static com.dat3m.dartagnan.program.event.Tag.*;
}

main
    :    LitmusLanguage ~(LBrace)* variableDeclaratorList program variableList? assertionFilter? assertionList? comment? EOF
    ;

variableDeclaratorList
    :   LBrace (globalDeclarator Semi comment?)* RBrace (Semi)?
    ;

globalDeclarator
    :   typeSpecifier? LBracket? varName RBracket? (Equals initConstantValue)?                                          # globalDeclaratorLocation
    |   typeSpecifier? t = threadId Colon n = varName (Equals initConstantValue)?                                       # globalDeclaratorRegister
    |   typeSpecifier? varName (Equals Ast? (Amp? varName | LPar Amp? varName RPar))?                                   # globalDeclaratorLocationLocation
    |   typeSpecifier? t = threadId Colon n = varName (Equals Ast? (Amp? varName | LPar Amp? varName RPar))?            # globalDeclaratorRegisterLocation
    |   typeSpecifier? varName LBracket DigitSequence? RBracket (Equals initArray)?                                     # globalDeclaratorArray
    ;

program
    :   thread+
    ;

thread
    :   threadId LPar threadArguments? RPar LBrace expression* RBrace
    ;

threadArguments
    :   pointerTypeSpecifier varName (Comma pointerTypeSpecifier varName)*
    ;

expression
    :   nre Semi
    |   ifExpression
    |   whileExpression
    ;

ifExpression
    :   If LPar re RPar expression elseExpression?
    |   If LPar re RPar LBrace expression* RBrace elseExpression?
    ;

elseExpression
    :   Else expression
    |   Else LBrace expression* RBrace
    ;

whileExpression
    :   While LPar re RPar expression
    |   While LPar re RPar LBrace expression* RBrace
    ;

re locals [IOpBin op, String mo]
    :   ( C11AtomicAdd LPar address = re Comma value = re Comma openCLMo RPar {$op = IOpBin.ADD;}
        | C11AtomicSub LPar address = re Comma value = re Comma openCLMo RPar {$op = IOpBin.SUB;}
        | C11AtomicOr LPar address = re Comma value = re Comma openCLMo RPar {$op = IOpBin.OR;}
        | C11AtomicXor LPar address = re Comma value = re Comma openCLMo RPar {$op = IOpBin.XOR;}
        | C11AtomicAnd LPar address = re Comma value = re Comma openCLMo RPar {$op = IOpBin.AND;})                      # OpenCLAtomicOp

    |   C11AtomicSCAS LPar address = re Comma expectedAdd = re Comma value = re Comma openCLMo Comma openCLMo RPar      # reOpenCLSCmpXchg
    |   C11AtomicWCAS LPar address = re Comma expectedAdd = re Comma value = re Comma openCLMo Comma openCLMo RPar      # reOpenCLWCmpXchg

    |   C11AtomicLoad    LPar address = re Comma openCLMo RPar                                                          # reOpenCLLoad

    |   boolConst                                                                                                       # reBoolConst
    |   Excl re                                                                                                         # reOpBoolNot
    |   re opBool re                                                                                                    # reOpBool
    |   re opCompare re                                                                                                 # reOpCompare
    |   re opArith re                                                                                                   # reOpArith

    |   LPar re RPar                                                                                                    # reParenthesis
    |   cast re                                                                                                         # reCast
    |   varName                                                                                                         # reVarName
    |   constant                                                                                                        # reConst
    ;

nre locals [IOpBin op, String mo, String name]
    :   C11AtomicStore    LPar address = re  Comma value = re Comma openCLMo RPar                                       # nreC11Store

    |   Ast? varName Equals re                                                                                          # nreAssignment
    |   typeSpecifier varName (Equals re)?                                                                              # nreRegDeclaration

    |   C11AtomicFence LPar openCLMo RPar                                                                               # nreC11Fence
    ;

variableList
    :   Locations LBracket (threadVariable | varName) (Semi (threadVariable | varName))* Semi? RBracket
    ;

boolConst returns [Boolean value]
    :   True    {$value = true;}
    |   False   {$value = false;}
    ;

opBool returns [BOpBin op]
    :   AmpAmp  {$op = BOpBin.AND;}
    |   BarBar  {$op = BOpBin.OR;}
    ;

opCompare returns [COpBin op]
    :   EqualsEquals    {$op = COpBin.EQ;}
    |   NotEquals       {$op = COpBin.NEQ;}
    |   LessEquals      {$op = COpBin.LTE;}
    |   GreaterEquals   {$op = COpBin.GTE;}
    |   Less            {$op = COpBin.LT;}
    |   Greater         {$op = COpBin.GT;}
    ;

opArith returns [IOpBin op]
    :   Plus    {$op = IOpBin.ADD;}
    |   Minus   {$op = IOpBin.SUB;}
    |   Amp     {$op = IOpBin.AND;}
    |   Bar     {$op = IOpBin.OR;}
    |   Circ    {$op = IOpBin.XOR;}
    ;

openCLMo returns [String mo]
    :   MoRelaxed   {$mo = C11.MO_RELAXED;}
    |   MoAcquire   {$mo = C11.MO_ACQUIRE;}
    |   MoRelease   {$mo = C11.MO_RELEASE;}
    |   MoAcqRel    {$mo = C11.MO_ACQUIRE_RELEASE;}
    |   MoSeqCst    {$mo = C11.MO_SC;}
    ;

threadVariable returns [int tid, String name]
    :   t = threadId Colon n = varName  {$tid = $t.id; $name = $n.text;}
    ;

initConstantValue
    :   constant
    ;

initArray
    :   LBrace arrayElement* (Comma arrayElement)* RBrace
    ;

arrayElement
    :   constant
    |   Ast? (Amp? varName | LPar Amp? varName RPar)
    ;

cast
    :   LPar typeSpecifier Ast* RPar
    ;

pointerTypeSpecifier
    :   (Volatile)? basicTypeSpecifier Ast
    ;

typeSpecifier
    :   (Volatile)? basicTypeSpecifier Ast*
    ;

basicTypeSpecifier
    :   Int
    |   AtomicInt
    |   IntPtr
    |   Char
    ;

varName
    :   Underscore* Identifier (Underscore (Identifier | DigitSequence)*)*
    ;

// Allowed outside of thread body (otherwise might conflict with pointer cast)
comment
    :   LPar Ast .*? Ast RPar
    ;

MoRelaxed
    :   'memory_order_relaxed'
    ;

MoAcquire
    :   'memory_order_acquire'
    ;

MoRelease
    :   'memory_order_release'
    ;

MoAcqRel
    :   'memory_order_acq_rel'
    ;

MoSeqCst
    :   'memory_order_seq_cst'
    ;

Locations
    :   'locations'
    ;

If
    :   'if'
    ;

Else
    :   'else'
    ;

While
    :   'while'
    ;

True
    :   'true'
    ;

False
    :   'false'
    ;

Volatile
    :   'volatile'
    ;

AtomicInt
    :   'atomic_int'
    ;

Int
    :   'int'
    ;

IntPtr
    :   'intptr_t'
    ;

Char
    :   'char'
    ;

AmpAmp
    :   '&&'
    ;

BarBar
    :   '||'
    ;

LitmusLanguage
    :   'OpenCL'
    ;

AssertionNot
    :   Tilde
    |   'not'
    ;

BlockComment
    :   '/*' .*? '*/'
        -> channel(HIDDEN)
    ;

LineComment
    :   '//' .*? Newline
        -> channel(HIDDEN)
    ;
