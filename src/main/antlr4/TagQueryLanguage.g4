grammar TagQueryLanguage;

@header {
    package net.darmo_creations.imageslibrary.query_parser.generated;
}

WS: [ \n\r\t]+;
IDENT: [\p{L}\p{N}_]+;
STRING: '"'('\\'[\\"*?]|~[\\"])*'"';
REGEX: '/'('\\'[\\/()[\]{}*+?|^$=!.><dDsSwWbBAGzZQEnrtf-]|~[\\/])*'/';
OR: '+';
NOT: '-';
LPAREN: '(';
RPAREN: ')';
EQUAL: '=';

query: WS? expr WS? EOF;

expr:
      expr WS? expr        # And
    | expr WS? OR WS? expr # Or
    | NOT WS? lit          # Negation
    | lit                  # Literal
    ;

lit:
      LPAREN WS? expr WS? RPAREN # Group
    | IDENT EQUAL IDENT? STRING  # PseudoTagString
    | IDENT EQUAL IDENT? REGEX   # PseudoTagRegex
    | IDENT                      # Tag
    ;
