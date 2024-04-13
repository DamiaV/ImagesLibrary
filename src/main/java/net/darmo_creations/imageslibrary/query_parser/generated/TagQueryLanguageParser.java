// Generated from TagQueryLanguage.g4 by ANTLR 4.9.3

package net.darmo_creations.imageslibrary.query_parser.generated;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.*;
import org.antlr.v4.runtime.tree.*;

import java.util.*;

@SuppressWarnings({ "all", "warnings", "unchecked", "unused", "cast" })
public class TagQueryLanguageParser extends Parser {
  static {
    RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION);
  }

  protected static final DFA[] _decisionToDFA;
  protected static final PredictionContextCache _sharedContextCache =
      new PredictionContextCache();
  public static final int
      WS = 1, IDENT = 2, STRING = 3, REGEX = 4, OR = 5, NOT = 6, LPAREN = 7, RPAREN = 8, EQUAL = 9,
      HASH = 10, STAR = 11;
  public static final int
      RULE_query = 0, RULE_expr = 1, RULE_lit = 2;

  private static String[] makeRuleNames() {
    return new String[] {
        "query", "expr", "lit"
    };
  }

  public static final String[] ruleNames = makeRuleNames();

  private static String[] makeLiteralNames() {
    return new String[] {
        null, null, null, null, null, "'+'", "'-'", "'('", "')'", "'='", "'#'",
        "'*'"
    };
  }

  private static final String[] _LITERAL_NAMES = makeLiteralNames();

  private static String[] makeSymbolicNames() {
    return new String[] {
        null, "WS", "IDENT", "STRING", "REGEX", "OR", "NOT", "LPAREN", "RPAREN",
        "EQUAL", "HASH", "STAR"
    };
  }

  private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
  public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

  /**
   * @deprecated Use {@link #VOCABULARY} instead.
   */
  @Deprecated
  public static final String[] tokenNames;

  static {
    tokenNames = new String[_SYMBOLIC_NAMES.length];
    for (int i = 0; i < tokenNames.length; i++) {
      tokenNames[i] = VOCABULARY.getLiteralName(i);
      if (tokenNames[i] == null) {
        tokenNames[i] = VOCABULARY.getSymbolicName(i);
      }

      if (tokenNames[i] == null) {
        tokenNames[i] = "<INVALID>";
      }
    }
  }

  @Override
  @Deprecated
  public String[] getTokenNames() {
    return tokenNames;
  }

  @Override

  public Vocabulary getVocabulary() {
    return VOCABULARY;
  }

  @Override
  public String getGrammarFileName() {
    return "TagQueryLanguage.g4";
  }

  @Override
  public String[] getRuleNames() {
    return ruleNames;
  }

  @Override
  public String getSerializedATN() {
    return _serializedATN;
  }

  @Override
  public ATN getATN() {
    return _ATN;
  }

  public TagQueryLanguageParser(TokenStream input) {
    super(input);
    _interp = new ParserATNSimulator(this, _ATN, _decisionToDFA, _sharedContextCache);
  }

  public static class QueryContext extends ParserRuleContext {
    public TerminalNode EOF() {
      return getToken(TagQueryLanguageParser.EOF, 0);
    }

    public ExprContext expr() {
      return getRuleContext(ExprContext.class, 0);
    }

    public TerminalNode STAR() {
      return getToken(TagQueryLanguageParser.STAR, 0);
    }

    public List<TerminalNode> WS() {
      return getTokens(TagQueryLanguageParser.WS);
    }

    public TerminalNode WS(int i) {
      return getToken(TagQueryLanguageParser.WS, i);
    }

    public QueryContext(ParserRuleContext parent, int invokingState) {
      super(parent, invokingState);
    }

    @Override
    public int getRuleIndex() {
      return RULE_query;
    }

    @Override
    public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
      if (visitor instanceof TagQueryLanguageVisitor)
        return ((TagQueryLanguageVisitor<? extends T>) visitor).visitQuery(this);
      else return visitor.visitChildren(this);
    }
  }

  public final QueryContext query() throws RecognitionException {
    QueryContext _localctx = new QueryContext(_ctx, getState());
    enterRule(_localctx, 0, RULE_query);
    int _la;
    try {
      enterOuterAlt(_localctx, 1);
      {
        setState(7);
        _errHandler.sync(this);
        _la = _input.LA(1);
        if (_la == WS) {
          {
            setState(6);
            match(WS);
          }
        }

        setState(11);
        _errHandler.sync(this);
        switch (_input.LA(1)) {
          case IDENT:
          case NOT:
          case LPAREN:
          case HASH: {
            setState(9);
            expr(0);
          }
          break;
          case STAR: {
            setState(10);
            match(STAR);
          }
          break;
          default:
            throw new NoViableAltException(this);
        }
        setState(14);
        _errHandler.sync(this);
        _la = _input.LA(1);
        if (_la == WS) {
          {
            setState(13);
            match(WS);
          }
        }

        setState(16);
        match(EOF);
      }
    } catch (RecognitionException re) {
      _localctx.exception = re;
      _errHandler.reportError(this, re);
      _errHandler.recover(this, re);
    } finally {
      exitRule();
    }
    return _localctx;
  }

  public static class ExprContext extends ParserRuleContext {
    public ExprContext(ParserRuleContext parent, int invokingState) {
      super(parent, invokingState);
    }

    @Override
    public int getRuleIndex() {
      return RULE_expr;
    }

    public ExprContext() {
    }

    public void copyFrom(ExprContext ctx) {
      super.copyFrom(ctx);
    }
  }

  public static class OrContext extends ExprContext {
    public List<ExprContext> expr() {
      return getRuleContexts(ExprContext.class);
    }

    public ExprContext expr(int i) {
      return getRuleContext(ExprContext.class, i);
    }

    public TerminalNode OR() {
      return getToken(TagQueryLanguageParser.OR, 0);
    }

    public List<TerminalNode> WS() {
      return getTokens(TagQueryLanguageParser.WS);
    }

    public TerminalNode WS(int i) {
      return getToken(TagQueryLanguageParser.WS, i);
    }

    public OrContext(ExprContext ctx) {
      copyFrom(ctx);
    }

    @Override
    public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
      if (visitor instanceof TagQueryLanguageVisitor)
        return ((TagQueryLanguageVisitor<? extends T>) visitor).visitOr(this);
      else return visitor.visitChildren(this);
    }
  }

  public static class NegationContext extends ExprContext {
    public TerminalNode NOT() {
      return getToken(TagQueryLanguageParser.NOT, 0);
    }

    public LitContext lit() {
      return getRuleContext(LitContext.class, 0);
    }

    public TerminalNode WS() {
      return getToken(TagQueryLanguageParser.WS, 0);
    }

    public NegationContext(ExprContext ctx) {
      copyFrom(ctx);
    }

    @Override
    public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
      if (visitor instanceof TagQueryLanguageVisitor)
        return ((TagQueryLanguageVisitor<? extends T>) visitor).visitNegation(this);
      else return visitor.visitChildren(this);
    }
  }

  public static class AndContext extends ExprContext {
    public List<ExprContext> expr() {
      return getRuleContexts(ExprContext.class);
    }

    public ExprContext expr(int i) {
      return getRuleContext(ExprContext.class, i);
    }

    public TerminalNode WS() {
      return getToken(TagQueryLanguageParser.WS, 0);
    }

    public AndContext(ExprContext ctx) {
      copyFrom(ctx);
    }

    @Override
    public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
      if (visitor instanceof TagQueryLanguageVisitor)
        return ((TagQueryLanguageVisitor<? extends T>) visitor).visitAnd(this);
      else return visitor.visitChildren(this);
    }
  }

  public static class LiteralContext extends ExprContext {
    public LitContext lit() {
      return getRuleContext(LitContext.class, 0);
    }

    public LiteralContext(ExprContext ctx) {
      copyFrom(ctx);
    }

    @Override
    public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
      if (visitor instanceof TagQueryLanguageVisitor)
        return ((TagQueryLanguageVisitor<? extends T>) visitor).visitLiteral(this);
      else return visitor.visitChildren(this);
    }
  }

  public final ExprContext expr() throws RecognitionException {
    return expr(0);
  }

  private ExprContext expr(int _p) throws RecognitionException {
    ParserRuleContext _parentctx = _ctx;
    int _parentState = getState();
    ExprContext _localctx = new ExprContext(_ctx, _parentState);
    ExprContext _prevctx = _localctx;
    int _startState = 2;
    enterRecursionRule(_localctx, 2, RULE_expr, _p);
    int _la;
    try {
      int _alt;
      enterOuterAlt(_localctx, 1);
      {
        setState(25);
        _errHandler.sync(this);
        switch (_input.LA(1)) {
          case NOT: {
            _localctx = new NegationContext(_localctx);
            _ctx = _localctx;
            _prevctx = _localctx;

            setState(19);
            match(NOT);
            setState(21);
            _errHandler.sync(this);
            _la = _input.LA(1);
            if (_la == WS) {
              {
                setState(20);
                match(WS);
              }
            }

            setState(23);
            lit();
          }
          break;
          case IDENT:
          case LPAREN:
          case HASH: {
            _localctx = new LiteralContext(_localctx);
            _ctx = _localctx;
            _prevctx = _localctx;
            setState(24);
            lit();
          }
          break;
          default:
            throw new NoViableAltException(this);
        }
        _ctx.stop = _input.LT(-1);
        setState(43);
        _errHandler.sync(this);
        _alt = getInterpreter().adaptivePredict(_input, 9, _ctx);
        while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
          if (_alt == 1) {
            if (_parseListeners != null) triggerExitRuleEvent();
            _prevctx = _localctx;
            {
              setState(41);
              _errHandler.sync(this);
              switch (getInterpreter().adaptivePredict(_input, 8, _ctx)) {
                case 1: {
                  _localctx = new AndContext(new ExprContext(_parentctx, _parentState));
                  pushNewRecursionContext(_localctx, _startState, RULE_expr);
                  setState(27);
                  if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
                  setState(29);
                  _errHandler.sync(this);
                  _la = _input.LA(1);
                  if (_la == WS) {
                    {
                      setState(28);
                      match(WS);
                    }
                  }

                  setState(31);
                  expr(5);
                }
                break;
                case 2: {
                  _localctx = new OrContext(new ExprContext(_parentctx, _parentState));
                  pushNewRecursionContext(_localctx, _startState, RULE_expr);
                  setState(32);
                  if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
                  setState(34);
                  _errHandler.sync(this);
                  _la = _input.LA(1);
                  if (_la == WS) {
                    {
                      setState(33);
                      match(WS);
                    }
                  }

                  setState(36);
                  match(OR);
                  setState(38);
                  _errHandler.sync(this);
                  _la = _input.LA(1);
                  if (_la == WS) {
                    {
                      setState(37);
                      match(WS);
                    }
                  }

                  setState(40);
                  expr(4);
                }
                break;
              }
            }
          }
          setState(45);
          _errHandler.sync(this);
          _alt = getInterpreter().adaptivePredict(_input, 9, _ctx);
        }
      }
    } catch (RecognitionException re) {
      _localctx.exception = re;
      _errHandler.reportError(this, re);
      _errHandler.recover(this, re);
    } finally {
      unrollRecursionContexts(_parentctx);
    }
    return _localctx;
  }

  public static class LitContext extends ParserRuleContext {
    public LitContext(ParserRuleContext parent, int invokingState) {
      super(parent, invokingState);
    }

    @Override
    public int getRuleIndex() {
      return RULE_lit;
    }

    public LitContext() {
    }

    public void copyFrom(LitContext ctx) {
      super.copyFrom(ctx);
    }
  }

  public static class GroupContext extends LitContext {
    public TerminalNode LPAREN() {
      return getToken(TagQueryLanguageParser.LPAREN, 0);
    }

    public ExprContext expr() {
      return getRuleContext(ExprContext.class, 0);
    }

    public TerminalNode RPAREN() {
      return getToken(TagQueryLanguageParser.RPAREN, 0);
    }

    public List<TerminalNode> WS() {
      return getTokens(TagQueryLanguageParser.WS);
    }

    public TerminalNode WS(int i) {
      return getToken(TagQueryLanguageParser.WS, i);
    }

    public GroupContext(LitContext ctx) {
      copyFrom(ctx);
    }

    @Override
    public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
      if (visitor instanceof TagQueryLanguageVisitor)
        return ((TagQueryLanguageVisitor<? extends T>) visitor).visitGroup(this);
      else return visitor.visitChildren(this);
    }
  }

  public static class PseudoTagRegexContext extends LitContext {
    public List<TerminalNode> IDENT() {
      return getTokens(TagQueryLanguageParser.IDENT);
    }

    public TerminalNode IDENT(int i) {
      return getToken(TagQueryLanguageParser.IDENT, i);
    }

    public TerminalNode EQUAL() {
      return getToken(TagQueryLanguageParser.EQUAL, 0);
    }

    public TerminalNode REGEX() {
      return getToken(TagQueryLanguageParser.REGEX, 0);
    }

    public PseudoTagRegexContext(LitContext ctx) {
      copyFrom(ctx);
    }

    @Override
    public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
      if (visitor instanceof TagQueryLanguageVisitor)
        return ((TagQueryLanguageVisitor<? extends T>) visitor).visitPseudoTagRegex(this);
      else return visitor.visitChildren(this);
    }
  }

  public static class BooleanPseudoTagContext extends LitContext {
    public TerminalNode HASH() {
      return getToken(TagQueryLanguageParser.HASH, 0);
    }

    public TerminalNode IDENT() {
      return getToken(TagQueryLanguageParser.IDENT, 0);
    }

    public BooleanPseudoTagContext(LitContext ctx) {
      copyFrom(ctx);
    }

    @Override
    public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
      if (visitor instanceof TagQueryLanguageVisitor)
        return ((TagQueryLanguageVisitor<? extends T>) visitor).visitBooleanPseudoTag(this);
      else return visitor.visitChildren(this);
    }
  }

  public static class PseudoTagStringContext extends LitContext {
    public List<TerminalNode> IDENT() {
      return getTokens(TagQueryLanguageParser.IDENT);
    }

    public TerminalNode IDENT(int i) {
      return getToken(TagQueryLanguageParser.IDENT, i);
    }

    public TerminalNode EQUAL() {
      return getToken(TagQueryLanguageParser.EQUAL, 0);
    }

    public TerminalNode STRING() {
      return getToken(TagQueryLanguageParser.STRING, 0);
    }

    public PseudoTagStringContext(LitContext ctx) {
      copyFrom(ctx);
    }

    @Override
    public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
      if (visitor instanceof TagQueryLanguageVisitor)
        return ((TagQueryLanguageVisitor<? extends T>) visitor).visitPseudoTagString(this);
      else return visitor.visitChildren(this);
    }
  }

  public static class TagContext extends LitContext {
    public TerminalNode IDENT() {
      return getToken(TagQueryLanguageParser.IDENT, 0);
    }

    public TagContext(LitContext ctx) {
      copyFrom(ctx);
    }

    @Override
    public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
      if (visitor instanceof TagQueryLanguageVisitor)
        return ((TagQueryLanguageVisitor<? extends T>) visitor).visitTag(this);
      else return visitor.visitChildren(this);
    }
  }

  public final LitContext lit() throws RecognitionException {
    LitContext _localctx = new LitContext(_ctx, getState());
    enterRule(_localctx, 4, RULE_lit);
    int _la;
    try {
      setState(71);
      _errHandler.sync(this);
      switch (getInterpreter().adaptivePredict(_input, 14, _ctx)) {
        case 1:
          _localctx = new GroupContext(_localctx);
          enterOuterAlt(_localctx, 1);
        {
          setState(46);
          match(LPAREN);
          setState(48);
          _errHandler.sync(this);
          _la = _input.LA(1);
          if (_la == WS) {
            {
              setState(47);
              match(WS);
            }
          }

          setState(50);
          expr(0);
          setState(52);
          _errHandler.sync(this);
          _la = _input.LA(1);
          if (_la == WS) {
            {
              setState(51);
              match(WS);
            }
          }

          setState(54);
          match(RPAREN);
        }
        break;
        case 2:
          _localctx = new PseudoTagStringContext(_localctx);
          enterOuterAlt(_localctx, 2);
        {
          setState(56);
          match(IDENT);
          setState(57);
          match(EQUAL);
          setState(59);
          _errHandler.sync(this);
          _la = _input.LA(1);
          if (_la == IDENT) {
            {
              setState(58);
              match(IDENT);
            }
          }

          setState(61);
          match(STRING);
        }
        break;
        case 3:
          _localctx = new PseudoTagRegexContext(_localctx);
          enterOuterAlt(_localctx, 3);
        {
          setState(62);
          match(IDENT);
          setState(63);
          match(EQUAL);
          setState(65);
          _errHandler.sync(this);
          _la = _input.LA(1);
          if (_la == IDENT) {
            {
              setState(64);
              match(IDENT);
            }
          }

          setState(67);
          match(REGEX);
        }
        break;
        case 4:
          _localctx = new BooleanPseudoTagContext(_localctx);
          enterOuterAlt(_localctx, 4);
        {
          setState(68);
          match(HASH);
          setState(69);
          match(IDENT);
        }
        break;
        case 5:
          _localctx = new TagContext(_localctx);
          enterOuterAlt(_localctx, 5);
        {
          setState(70);
          match(IDENT);
        }
        break;
      }
    } catch (RecognitionException re) {
      _localctx.exception = re;
      _errHandler.reportError(this, re);
      _errHandler.recover(this, re);
    } finally {
      exitRule();
    }
    return _localctx;
  }

  public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
    switch (ruleIndex) {
      case 1:
        return expr_sempred((ExprContext) _localctx, predIndex);
    }
    return true;
  }

  private boolean expr_sempred(ExprContext _localctx, int predIndex) {
    switch (predIndex) {
      case 0:
        return precpred(_ctx, 4);
      case 1:
        return precpred(_ctx, 3);
    }
    return true;
  }

  public static final String _serializedATN =
      "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\rL\4\2\t\2\4\3\t" +
      "\3\4\4\t\4\3\2\5\2\n\n\2\3\2\3\2\5\2\16\n\2\3\2\5\2\21\n\2\3\2\3\2\3\3" +
      "\3\3\3\3\5\3\30\n\3\3\3\3\3\5\3\34\n\3\3\3\3\3\5\3 \n\3\3\3\3\3\3\3\5" +
      "\3%\n\3\3\3\3\3\5\3)\n\3\3\3\7\3,\n\3\f\3\16\3/\13\3\3\4\3\4\5\4\63\n" +
      "\4\3\4\3\4\5\4\67\n\4\3\4\3\4\3\4\3\4\3\4\5\4>\n\4\3\4\3\4\3\4\3\4\5\4" +
      "D\n\4\3\4\3\4\3\4\3\4\5\4J\n\4\3\4\2\3\4\5\2\4\6\2\2\2Z\2\t\3\2\2\2\4" +
      "\33\3\2\2\2\6I\3\2\2\2\b\n\7\3\2\2\t\b\3\2\2\2\t\n\3\2\2\2\n\r\3\2\2\2" +
      "\13\16\5\4\3\2\f\16\7\r\2\2\r\13\3\2\2\2\r\f\3\2\2\2\16\20\3\2\2\2\17" +
      "\21\7\3\2\2\20\17\3\2\2\2\20\21\3\2\2\2\21\22\3\2\2\2\22\23\7\2\2\3\23" +
      "\3\3\2\2\2\24\25\b\3\1\2\25\27\7\b\2\2\26\30\7\3\2\2\27\26\3\2\2\2\27" +
      "\30\3\2\2\2\30\31\3\2\2\2\31\34\5\6\4\2\32\34\5\6\4\2\33\24\3\2\2\2\33" +
      "\32\3\2\2\2\34-\3\2\2\2\35\37\f\6\2\2\36 \7\3\2\2\37\36\3\2\2\2\37 \3" +
      "\2\2\2 !\3\2\2\2!,\5\4\3\7\"$\f\5\2\2#%\7\3\2\2$#\3\2\2\2$%\3\2\2\2%&" +
      "\3\2\2\2&(\7\7\2\2\')\7\3\2\2(\'\3\2\2\2()\3\2\2\2)*\3\2\2\2*,\5\4\3\6" +
      "+\35\3\2\2\2+\"\3\2\2\2,/\3\2\2\2-+\3\2\2\2-.\3\2\2\2.\5\3\2\2\2/-\3\2" +
      "\2\2\60\62\7\t\2\2\61\63\7\3\2\2\62\61\3\2\2\2\62\63\3\2\2\2\63\64\3\2" +
      "\2\2\64\66\5\4\3\2\65\67\7\3\2\2\66\65\3\2\2\2\66\67\3\2\2\2\678\3\2\2" +
      "\289\7\n\2\29J\3\2\2\2:;\7\4\2\2;=\7\13\2\2<>\7\4\2\2=<\3\2\2\2=>\3\2" +
      "\2\2>?\3\2\2\2?J\7\5\2\2@A\7\4\2\2AC\7\13\2\2BD\7\4\2\2CB\3\2\2\2CD\3" +
      "\2\2\2DE\3\2\2\2EJ\7\6\2\2FG\7\f\2\2GJ\7\4\2\2HJ\7\4\2\2I\60\3\2\2\2I" +
      ":\3\2\2\2I@\3\2\2\2IF\3\2\2\2IH\3\2\2\2J\7\3\2\2\2\21\t\r\20\27\33\37" +
      "$(+-\62\66=CI";
  public static final ATN _ATN =
      new ATNDeserializer().deserialize(_serializedATN.toCharArray());

  static {
    _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
    for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
      _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
    }
  }
}