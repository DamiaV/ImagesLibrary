grammar TagQueryLanguage;

@header {
    package net.darmo_creations.bildumilo.query_parser.generated;
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
HASH: '#';
STAR: '*';

query: WS? (expr | STAR) WS? EOF;

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
    | HASH IDENT                 # BooleanPseudoTag
    | IDENT                      # Tag
    ;
