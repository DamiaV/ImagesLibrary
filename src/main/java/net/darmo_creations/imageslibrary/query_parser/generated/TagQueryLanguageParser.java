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
      HASH = 10;
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
        null, null, null, null, null, "'+'", "'-'", "'('", "')'", "'='", "'#'"
    };
  }

  private static final String[] _LITERAL_NAMES = makeLiteralNames();

  private static String[] makeSymbolicNames() {
    return new String[] {
        null, "WS", "IDENT", "STRING", "REGEX", "OR", "NOT", "LPAREN", "RPAREN",
        "EQUAL", "HASH"
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
    public ExprContext expr() {
      return getRuleContext(ExprContext.class, 0);
    }

    public TerminalNode EOF() {
      return getToken(TagQueryLanguageParser.EOF, 0);
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

        setState(9);
        expr(0);
        setState(11);
        _errHandler.sync(this);
        _la = _input.LA(1);
        if (_la == WS) {
          {
            setState(10);
            match(WS);
          }
        }

        setState(13);
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
        setState(22);
        _errHandler.sync(this);
        switch (_input.LA(1)) {
          case NOT: {
            _localctx = new NegationContext(_localctx);
            _ctx = _localctx;
            _prevctx = _localctx;

            setState(16);
            match(NOT);
            setState(18);
            _errHandler.sync(this);
            _la = _input.LA(1);
            if (_la == WS) {
              {
                setState(17);
                match(WS);
              }
            }

            setState(20);
            lit();
          }
          break;
          case IDENT:
          case LPAREN:
          case HASH: {
            _localctx = new LiteralContext(_localctx);
            _ctx = _localctx;
            _prevctx = _localctx;
            setState(21);
            lit();
          }
          break;
          default:
            throw new NoViableAltException(this);
        }
        _ctx.stop = _input.LT(-1);
        setState(40);
        _errHandler.sync(this);
        _alt = getInterpreter().adaptivePredict(_input, 8, _ctx);
        while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
          if (_alt == 1) {
            if (_parseListeners != null) triggerExitRuleEvent();
            _prevctx = _localctx;
            {
              setState(38);
              _errHandler.sync(this);
              switch (getInterpreter().adaptivePredict(_input, 7, _ctx)) {
                case 1: {
                  _localctx = new AndContext(new ExprContext(_parentctx, _parentState));
                  pushNewRecursionContext(_localctx, _startState, RULE_expr);
                  setState(24);
                  if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
                  setState(26);
                  _errHandler.sync(this);
                  _la = _input.LA(1);
                  if (_la == WS) {
                    {
                      setState(25);
                      match(WS);
                    }
                  }

                  setState(28);
                  expr(5);
                }
                break;
                case 2: {
                  _localctx = new OrContext(new ExprContext(_parentctx, _parentState));
                  pushNewRecursionContext(_localctx, _startState, RULE_expr);
                  setState(29);
                  if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
                  setState(31);
                  _errHandler.sync(this);
                  _la = _input.LA(1);
                  if (_la == WS) {
                    {
                      setState(30);
                      match(WS);
                    }
                  }

                  setState(33);
                  match(OR);
                  setState(35);
                  _errHandler.sync(this);
                  _la = _input.LA(1);
                  if (_la == WS) {
                    {
                      setState(34);
                      match(WS);
                    }
                  }

                  setState(37);
                  expr(4);
                }
                break;
              }
            }
          }
          setState(42);
          _errHandler.sync(this);
          _alt = getInterpreter().adaptivePredict(_input, 8, _ctx);
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
      setState(68);
      _errHandler.sync(this);
      switch (getInterpreter().adaptivePredict(_input, 13, _ctx)) {
        case 1:
          _localctx = new GroupContext(_localctx);
          enterOuterAlt(_localctx, 1);
        {
          setState(43);
          match(LPAREN);
          setState(45);
          _errHandler.sync(this);
          _la = _input.LA(1);
          if (_la == WS) {
            {
              setState(44);
              match(WS);
            }
          }

          setState(47);
          expr(0);
          setState(49);
          _errHandler.sync(this);
          _la = _input.LA(1);
          if (_la == WS) {
            {
              setState(48);
              match(WS);
            }
          }

          setState(51);
          match(RPAREN);
        }
        break;
        case 2:
          _localctx = new PseudoTagStringContext(_localctx);
          enterOuterAlt(_localctx, 2);
        {
          setState(53);
          match(IDENT);
          setState(54);
          match(EQUAL);
          setState(56);
          _errHandler.sync(this);
          _la = _input.LA(1);
          if (_la == IDENT) {
            {
              setState(55);
              match(IDENT);
            }
          }

          setState(58);
          match(STRING);
        }
        break;
        case 3:
          _localctx = new PseudoTagRegexContext(_localctx);
          enterOuterAlt(_localctx, 3);
        {
          setState(59);
          match(IDENT);
          setState(60);
          match(EQUAL);
          setState(62);
          _errHandler.sync(this);
          _la = _input.LA(1);
          if (_la == IDENT) {
            {
              setState(61);
              match(IDENT);
            }
          }

          setState(64);
          match(REGEX);
        }
        break;
        case 4:
          _localctx = new BooleanPseudoTagContext(_localctx);
          enterOuterAlt(_localctx, 4);
        {
          setState(65);
          match(HASH);
          setState(66);
          match(IDENT);
        }
        break;
        case 5:
          _localctx = new TagContext(_localctx);
          enterOuterAlt(_localctx, 5);
        {
          setState(67);
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
      "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\fI\4\2\t\2\4\3\t" +
      "\3\4\4\t\4\3\2\5\2\n\n\2\3\2\3\2\5\2\16\n\2\3\2\3\2\3\3\3\3\3\3\5\3\25" +
      "\n\3\3\3\3\3\5\3\31\n\3\3\3\3\3\5\3\35\n\3\3\3\3\3\3\3\5\3\"\n\3\3\3\3" +
      "\3\5\3&\n\3\3\3\7\3)\n\3\f\3\16\3,\13\3\3\4\3\4\5\4\60\n\4\3\4\3\4\5\4" +
      "\64\n\4\3\4\3\4\3\4\3\4\3\4\5\4;\n\4\3\4\3\4\3\4\3\4\5\4A\n\4\3\4\3\4" +
      "\3\4\3\4\5\4G\n\4\3\4\2\3\4\5\2\4\6\2\2\2V\2\t\3\2\2\2\4\30\3\2\2\2\6" +
      "F\3\2\2\2\b\n\7\3\2\2\t\b\3\2\2\2\t\n\3\2\2\2\n\13\3\2\2\2\13\r\5\4\3" +
      "\2\f\16\7\3\2\2\r\f\3\2\2\2\r\16\3\2\2\2\16\17\3\2\2\2\17\20\7\2\2\3\20" +
      "\3\3\2\2\2\21\22\b\3\1\2\22\24\7\b\2\2\23\25\7\3\2\2\24\23\3\2\2\2\24" +
      "\25\3\2\2\2\25\26\3\2\2\2\26\31\5\6\4\2\27\31\5\6\4\2\30\21\3\2\2\2\30" +
      "\27\3\2\2\2\31*\3\2\2\2\32\34\f\6\2\2\33\35\7\3\2\2\34\33\3\2\2\2\34\35" +
      "\3\2\2\2\35\36\3\2\2\2\36)\5\4\3\7\37!\f\5\2\2 \"\7\3\2\2! \3\2\2\2!\"" +
      "\3\2\2\2\"#\3\2\2\2#%\7\7\2\2$&\7\3\2\2%$\3\2\2\2%&\3\2\2\2&\'\3\2\2\2" +
      "\')\5\4\3\6(\32\3\2\2\2(\37\3\2\2\2),\3\2\2\2*(\3\2\2\2*+\3\2\2\2+\5\3" +
      "\2\2\2,*\3\2\2\2-/\7\t\2\2.\60\7\3\2\2/.\3\2\2\2/\60\3\2\2\2\60\61\3\2" +
      "\2\2\61\63\5\4\3\2\62\64\7\3\2\2\63\62\3\2\2\2\63\64\3\2\2\2\64\65\3\2" +
      "\2\2\65\66\7\n\2\2\66G\3\2\2\2\678\7\4\2\28:\7\13\2\29;\7\4\2\2:9\3\2" +
      "\2\2:;\3\2\2\2;<\3\2\2\2<G\7\5\2\2=>\7\4\2\2>@\7\13\2\2?A\7\4\2\2@?\3" +
      "\2\2\2@A\3\2\2\2AB\3\2\2\2BG\7\6\2\2CD\7\f\2\2DG\7\4\2\2EG\7\4\2\2F-\3" +
      "\2\2\2F\67\3\2\2\2F=\3\2\2\2FC\3\2\2\2FE\3\2\2\2G\7\3\2\2\2\20\t\r\24" +
      "\30\34!%(*/\63:@F";
  public static final ATN _ATN =
      new ATNDeserializer().deserialize(_serializedATN.toCharArray());

  static {
    _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
    for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
      _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
    }
  }
}