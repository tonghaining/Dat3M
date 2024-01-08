grammar LitmusOpenCL;

import LitmusAssertions;

@header{
import com.dat3m.dartagnan.expression.op.*;
}

main
    :    LitmusLanguage ~(LBrace)* variableDeclaratorList program variableList? assertionFilter? assertionList? EOF
    ;

variableDeclaratorList
    :   LBrace variableDeclarator? (Semi variableDeclarator)* Semi? RBrace Semi?
    ;

variableDeclarator
    :   variableDeclaratorLocation
    |   variableDeclaratorRegister
    |   variableDeclaratorRegisterLocation
    |   variableDeclaratorLocationLocation
    ;

variableDeclaratorLocation
    :   location Equals constant
    ;

variableDeclaratorRegister
    :   threadId Colon register Equals constant
    ;

variableDeclaratorRegisterLocation
    :   threadId Colon register Equals Amp? location
    ;

variableDeclaratorLocationLocation
    :   location Equals Amp? location
    ;

variableList
    :   Locations LBracket variable (Semi variable)* Semi? RBracket
    ;

variable
    :   location
    |   threadId Colon register
    ;

program
    :   threadDeclaratorList instructionList
    ;

threadDeclaratorList
    :   threadScope (Bar threadScope)* Semi
    ;

threadScope
    :   threadId At scopeID
    ;

scopeID
    :   WG wgID Comma DEV dvID
    ;


instructionList
    :   (instructionRow) +
    ;

instructionRow
    :   instruction (Bar instruction)* Semi
    ;

instruction
    :
    |   storeInstruction
    |   loadInstruction
    |   atomInstruction
    |   fenceInstruction
    |   label
    |   branchCond
    |   jump
    ;

storeInstruction
    :   Store mo context scope location Comma value
    ;

loadInstruction
    :   localValue
    |   localAdd
    |   localSub
    |   localMul
    |   localDiv
    |   loadLocation
    ;

localValue
    :   Load mo scope register Comma value
    ;

localAdd
    :   Add register Comma value Comma value
    ;

localSub
    :   Sub register Comma value Comma value
    ;

localMul
    :   Mul register Comma value Comma value
    ;

localDiv
    :   Div register Comma value Comma value
    ;

loadLocation
    :   Load mo context scope register Comma location
    ;

atomInstruction
    :   atomCompareExchange
    |   atomOp
    ;

atomCompareExchange
    :   Atom CompareExchange register Comma location Comma location Comma value Comma mo Comma mo (Comma scope)?
    ;

atomOp
    :   Atom mo context scope operation register Comma location Comma value
    ;

fenceInstruction
    :   fence
    |   barrier
    ;

fence
    :   Fence mo context scope
    ;

barrier
    :   Barrier mo context scope value
    ;

label
    :   Label Colon
    ;

branchCond
    :   cond value Comma value Comma Label
    ;

jump
    :   Goto Label
    ;

value
    :   constant
    |   register
    |   location
    ;

location
    :   Identifier
    ;

register
    :   Register
    ;

assertionValue
    :   location
    |   threadId Colon register
    |   constant
    ;

scope returns [String content]
    :   Period WG {$content = "WG";}
    |   Period DEV {$content = "DEV";}
    |   Period ALL {$content = "ALL";}
    ;

context returns [String content]
    :   Period GLOBAL {$content = "GLOBAL";}
    |   Period LOCAL {$content = "LOCAL";}
    ;

wgID returns [int id]
    :   t = DigitSequence {$id = Integer.parseInt($t.text);}
    ;

dvID returns [int id]
    :   t = DigitSequence {$id = Integer.parseInt($t.text);}
    ;

mo returns [String content]
    :   Period Weak {$content = "WEAK";}
    |   Period Relaxed {$content = "RELAXED";}
    |   Period Release {$content = "REL";}
    |   Period Acquire {$content = "ACQ";}
    |   Period SC {$content = "SC";}
    ;

operation locals [IOpBin op]
    :   Period Add {$op = IOpBin.ADD;}
    |   Period Sub {$op = IOpBin.SUB;}
    |   Period Mul {$op = IOpBin.MUL;}
    |   Period Div {$op = IOpBin.DIV;}
    |   Period And {$op = IOpBin.AND;}
    |   Period Or {$op = IOpBin.OR;}
    |   Period Xor {$op = IOpBin.XOR;}
    ;

cond returns [COpBin op]
    :   Beq {$op = COpBin.EQ;}
    |   Bne {$op = COpBin.NEQ;}
    |   Bge {$op = COpBin.GTE;}
    |   Ble {$op = COpBin.LTE;}
    |   Bgt {$op = COpBin.GT;}
    |   Blt {$op = COpBin.LT;}
    ;

Locations
    :   'locations'
    ;

Register
    :   'r' DigitSequence
    ;

Label
    :   'LC' DigitSequence
    ;

CompareExchange
    :   'cmpxchg'
    ;

Load            :   'ld';
Store           :   'st';
Atom            :   'atom';
Fence           :   'fence';
Barrier         :   'bar';

MemoryBarrier   :   'membar';
ControlBarrier  :   'cbar';

WI              :   'wi';
WG              :   'wg';
DEV             :   'dev';
ALL             :   'all';

GLOBAL          :   'global';
LOCAL           :   'local';

Weak            :   'weak';
Relaxed         :   'relaxed';
Acquire         :   'acq';
Release         :   'rel';
SC              :   'sc';

Beq             :   'beq';
Bne             :   'bne';
Blt             :   'blt';
Bgt             :   'bgt';
Ble             :   'ble';
Bge             :   'bge';

Add             :   'add';
Sub             :   'sub';
Mul             :   'mul';
Div             :   'div';
And             :   'and';
Or              :   'or';
Xor             :   'xor';

Goto            :   'goto';

LitmusLanguage
    :   'OpenCL'
    ;