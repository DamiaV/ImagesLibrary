grammar TagQueryLanguage;

@header {
    package net.darmo_creations.imageslibrary.query_parser.generated;
}

WS: [ \n\r\t];
IDENT: [a-z_][a-z0-9_]*;
FLAG: [is];
STRING: '"'('\\'[\\"*?]|~[\\"])*'"';
REGEX: '/'('\\'[\\/()[\]{}*+?|^$=!.><dDsSwWbBAGzZQEnrtf-]|~[\\/])*'/';

expr:
      expr WS* expr         # And
    | expr WS* '+' WS* expr # Or
    | '-' WS* lit           # Negation
    | lit                   # Literal
    ;

lit:
      '(' WS* expr WS* ')'   # Group
    | IDENT '=' FLAG? STRING # PseudoTagString
    | IDENT '=' FLAG? REGEX  # PseudoTagRegex
    | IDENT                  # Tag
    ;
