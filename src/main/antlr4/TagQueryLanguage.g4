grammar TagQueryLanguage;

@header {
    package net.darmo_creations.imageslibrary.query_parser.generated;
}

WS: [ \n\r\t];
IDENT: [\p{L}\p{N}_]+;
STRING: '"'('\\'[\\"*?]|~[\\"])*'"';
REGEX: '/'('\\'[\\/()[\]{}*+?|^$=!.><dDsSwWbBAGzZQEnrtf-]|~[\\/])*'/';

query: expr EOF;

expr:
      expr WS* expr         # And
    | expr WS* '+' WS* expr # Or
    | '-' WS* lit           # Negation
    | lit                   # Literal
    ;

lit:
      '(' WS* expr WS* ')'    # Group
    | IDENT '=' IDENT? STRING # PseudoTagString
    | IDENT '=' IDENT? REGEX  # PseudoTagRegex
    | IDENT                   # Tag
    ;
