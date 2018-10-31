grammar LitmusX86;

@header{
package dartagnan;
}

import LitmusBase;

main
    :    LitmusLanguage ~(LBrace)* variableDeclaratorList program variableList? assertionList? EOF
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
    :   location Equals value
    ;

variableDeclaratorRegister
    :   threadId Colon register Equals value
    ;

variableDeclaratorRegisterLocation
    :   threadId Colon register Equals location
    ;

variableDeclaratorLocationLocation
    :   location Equals location
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
    :   threadId (Bar threadId)* Semi
    ;

instructionList
    :   (instructionRow)+
    ;

instructionRow
    :   instruction (Bar instruction)* Semi
    ;

instruction
    :   none
    |   loadValueToRegister
    |   loadLocationToRegister
    |   storeValueToLocation
    |   storeRegisterToLocation
    |   fence
    |   exchangeRegisterLocation
    |   incrementLocation
    |   compareRegisterValue
    |   compareLocationValue
    |   addRegisterRegister
    |   addRegisterValue
    ;

none
    :
    ;

loadValueToRegister
    :   Mov register Comma Dollar? value
    ;

loadLocationToRegister
    :   Mov register Comma LBracket? location RBracket?
    ;

storeValueToLocation
    :   Mov LBracket? location RBracket? Comma Dollar? value
    ;

storeRegisterToLocation
    :   Mov LBracket? location RBracket? Comma register
    ;

fence
    :   Mfence
    |   Lfence
    |   Sfence
    ;

exchangeRegisterLocation
    :   Xchg register Comma LBracket? location RBracket?
    |   Xchg LBracket? location RBracket? Comma register
    ;

incrementLocation
    :   Inc LBracket? location RBracket?
    ;

compareRegisterValue
    :   Cmp register Comma Dollar? value
    ;

compareLocationValue
    :   Cmp LBracket? location RBracket? Comma Dollar? value
    ;

addRegisterRegister
    :   Add register Comma register
    ;

addRegisterValue
    :   Add register Comma Dollar? value
    ;

location
    :   Identifier
    ;

register
    :   Register
    ;

value
    :   DigitSequence
    ;

assertionValue
    :   location
    |   threadId Colon register
    |   value
    ;


Mov
    :   'MOV'
    |   'mov'
    ;

Xchg
    :   'XCHG'
    |   'xchg'
    ;

Mfence
    :   'MFENCE'
    |   'mfence'
    ;

Lfence
    :   'LFENCE'
    |   'lfence'
    ;

Sfence
    :   'SFENCE'
    |   'sfence'
    ;

Inc
    :   'INC'
    |   'inc'
    ;

Cmp
    :   'CMP'
    |   'cmp'
    ;

Add
    :   'ADD'
    |   'add'
    ;

Register
    :   'EAX'
    |   'EBX'
    |   'ECX'
    |   'EDX'
    |   'ESP'
    |   'EBP'
    |   'ESI'
    |   'EDI'
    ;

LitmusLanguage
    :   'X86'
    |   'x86'
    ;