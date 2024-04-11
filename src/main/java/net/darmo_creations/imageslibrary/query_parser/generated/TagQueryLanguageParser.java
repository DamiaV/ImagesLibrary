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
      T__0 = 1, T__1 = 2, T__2 = 3, T__3 = 4, T__4 = 5, WS = 6, IDENT = 7, STRING = 8, REGEX = 9;
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
        null, "'+'", "'-'", "'('", "')'", "'='"
    };
  }

  private static final String[] _LITERAL_NAMES = makeLiteralNames();

  private static String[] makeSymbolicNames() {
    return new String[] {
        null, null, null, null, null, null, "WS", "IDENT", "STRING", "REGEX"
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
    try {
      enterOuterAlt(_localctx, 1);
      {
        setState(6);
        expr(0);
        setState(7);
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
    public LitContext lit() {
      return getRuleContext(LitContext.class, 0);
    }

    public List<TerminalNode> WS() {
      return getTokens(TagQueryLanguageParser.WS);
    }

    public TerminalNode WS(int i) {
      return getToken(TagQueryLanguageParser.WS, i);
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

    public List<TerminalNode> WS() {
      return getTokens(TagQueryLanguageParser.WS);
    }

    public TerminalNode WS(int i) {
      return getToken(TagQueryLanguageParser.WS, i);
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
        setState(19);
        _errHandler.sync(this);
        switch (_input.LA(1)) {
          case T__1: {
            _localctx = new NegationContext(_localctx);
            _ctx = _localctx;
            _prevctx = _localctx;

            setState(10);
            match(T__1);
            setState(14);
            _errHandler.sync(this);
            _la = _input.LA(1);
            while (_la == WS) {
              {
                {
                  setState(11);
                  match(WS);
                }
              }
              setState(16);
              _errHandler.sync(this);
              _la = _input.LA(1);
            }
            setState(17);
            lit();
          }
          break;
          case T__2:
          case IDENT: {
            _localctx = new LiteralContext(_localctx);
            _ctx = _localctx;
            _prevctx = _localctx;
            setState(18);
            lit();
          }
          break;
          default:
            throw new NoViableAltException(this);
        }
        _ctx.stop = _input.LT(-1);
        setState(46);
        _errHandler.sync(this);
        _alt = getInterpreter().adaptivePredict(_input, 6, _ctx);
        while (_alt != 2 && _alt != org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER) {
          if (_alt == 1) {
            if (_parseListeners != null) triggerExitRuleEvent();
            _prevctx = _localctx;
            {
              setState(44);
              _errHandler.sync(this);
              switch (getInterpreter().adaptivePredict(_input, 5, _ctx)) {
                case 1: {
                  _localctx = new AndContext(new ExprContext(_parentctx, _parentState));
                  pushNewRecursionContext(_localctx, _startState, RULE_expr);
                  setState(21);
                  if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
                  setState(25);
                  _errHandler.sync(this);
                  _la = _input.LA(1);
                  while (_la == WS) {
                    {
                      {
                        setState(22);
                        match(WS);
                      }
                    }
                    setState(27);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
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
                  setState(33);
                  _errHandler.sync(this);
                  _la = _input.LA(1);
                  while (_la == WS) {
                    {
                      {
                        setState(30);
                        match(WS);
                      }
                    }
                    setState(35);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                  }
                  setState(36);
                  match(T__0);
                  setState(40);
                  _errHandler.sync(this);
                  _la = _input.LA(1);
                  while (_la == WS) {
                    {
                      {
                        setState(37);
                        match(WS);
                      }
                    }
                    setState(42);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                  }
                  setState(43);
                  expr(4);
                }
                break;
              }
            }
          }
          setState(48);
          _errHandler.sync(this);
          _alt = getInterpreter().adaptivePredict(_input, 6, _ctx);
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
    public ExprContext expr() {
      return getRuleContext(ExprContext.class, 0);
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

  public static class PseudoTagStringContext extends LitContext {
    public List<TerminalNode> IDENT() {
      return getTokens(TagQueryLanguageParser.IDENT);
    }

    public TerminalNode IDENT(int i) {
      return getToken(TagQueryLanguageParser.IDENT, i);
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
      setState(78);
      _errHandler.sync(this);
      switch (getInterpreter().adaptivePredict(_input, 11, _ctx)) {
        case 1:
          _localctx = new GroupContext(_localctx);
          enterOuterAlt(_localctx, 1);
        {
          setState(49);
          match(T__2);
          setState(53);
          _errHandler.sync(this);
          _la = _input.LA(1);
          while (_la == WS) {
            {
              {
                setState(50);
                match(WS);
              }
            }
            setState(55);
            _errHandler.sync(this);
            _la = _input.LA(1);
          }
          setState(56);
          expr(0);
          setState(60);
          _errHandler.sync(this);
          _la = _input.LA(1);
          while (_la == WS) {
            {
              {
                setState(57);
                match(WS);
              }
            }
            setState(62);
            _errHandler.sync(this);
            _la = _input.LA(1);
          }
          setState(63);
          match(T__3);
        }
        break;
        case 2:
          _localctx = new PseudoTagStringContext(_localctx);
          enterOuterAlt(_localctx, 2);
        {
          setState(65);
          match(IDENT);
          setState(66);
          match(T__4);
          setState(68);
          _errHandler.sync(this);
          _la = _input.LA(1);
          if (_la == IDENT) {
            {
              setState(67);
              match(IDENT);
            }
          }

          setState(70);
          match(STRING);
        }
        break;
        case 3:
          _localctx = new PseudoTagRegexContext(_localctx);
          enterOuterAlt(_localctx, 3);
        {
          setState(71);
          match(IDENT);
          setState(72);
          match(T__4);
          setState(74);
          _errHandler.sync(this);
          _la = _input.LA(1);
          if (_la == IDENT) {
            {
              setState(73);
              match(IDENT);
            }
          }

          setState(76);
          match(REGEX);
        }
        break;
        case 4:
          _localctx = new TagContext(_localctx);
          enterOuterAlt(_localctx, 4);
        {
          setState(77);
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
      "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\13S\4\2\t\2\4\3\t" +
      "\3\4\4\t\4\3\2\3\2\3\2\3\3\3\3\3\3\7\3\17\n\3\f\3\16\3\22\13\3\3\3\3\3" +
      "\5\3\26\n\3\3\3\3\3\7\3\32\n\3\f\3\16\3\35\13\3\3\3\3\3\3\3\7\3\"\n\3" +
      "\f\3\16\3%\13\3\3\3\3\3\7\3)\n\3\f\3\16\3,\13\3\3\3\7\3/\n\3\f\3\16\3" +
      "\62\13\3\3\4\3\4\7\4\66\n\4\f\4\16\49\13\4\3\4\3\4\7\4=\n\4\f\4\16\4@" +
      "\13\4\3\4\3\4\3\4\3\4\3\4\5\4G\n\4\3\4\3\4\3\4\3\4\5\4M\n\4\3\4\3\4\5" +
      "\4Q\n\4\3\4\2\3\4\5\2\4\6\2\2\2]\2\b\3\2\2\2\4\25\3\2\2\2\6P\3\2\2\2\b" +
      "\t\5\4\3\2\t\n\7\2\2\3\n\3\3\2\2\2\13\f\b\3\1\2\f\20\7\4\2\2\r\17\7\b" +
      "\2\2\16\r\3\2\2\2\17\22\3\2\2\2\20\16\3\2\2\2\20\21\3\2\2\2\21\23\3\2" +
      "\2\2\22\20\3\2\2\2\23\26\5\6\4\2\24\26\5\6\4\2\25\13\3\2\2\2\25\24\3\2" +
      "\2\2\26\60\3\2\2\2\27\33\f\6\2\2\30\32\7\b\2\2\31\30\3\2\2\2\32\35\3\2" +
      "\2\2\33\31\3\2\2\2\33\34\3\2\2\2\34\36\3\2\2\2\35\33\3\2\2\2\36/\5\4\3" +
      "\7\37#\f\5\2\2 \"\7\b\2\2! \3\2\2\2\"%\3\2\2\2#!\3\2\2\2#$\3\2\2\2$&\3" +
      "\2\2\2%#\3\2\2\2&*\7\3\2\2\')\7\b\2\2(\'\3\2\2\2),\3\2\2\2*(\3\2\2\2*" +
      "+\3\2\2\2+-\3\2\2\2,*\3\2\2\2-/\5\4\3\6.\27\3\2\2\2.\37\3\2\2\2/\62\3" +
      "\2\2\2\60.\3\2\2\2\60\61\3\2\2\2\61\5\3\2\2\2\62\60\3\2\2\2\63\67\7\5" +
      "\2\2\64\66\7\b\2\2\65\64\3\2\2\2\669\3\2\2\2\67\65\3\2\2\2\678\3\2\2\2" +
      "8:\3\2\2\29\67\3\2\2\2:>\5\4\3\2;=\7\b\2\2<;\3\2\2\2=@\3\2\2\2><\3\2\2" +
      "\2>?\3\2\2\2?A\3\2\2\2@>\3\2\2\2AB\7\6\2\2BQ\3\2\2\2CD\7\t\2\2DF\7\7\2" +
      "\2EG\7\t\2\2FE\3\2\2\2FG\3\2\2\2GH\3\2\2\2HQ\7\n\2\2IJ\7\t\2\2JL\7\7\2" +
      "\2KM\7\t\2\2LK\3\2\2\2LM\3\2\2\2MN\3\2\2\2NQ\7\13\2\2OQ\7\t\2\2P\63\3" +
      "\2\2\2PC\3\2\2\2PI\3\2\2\2PO\3\2\2\2Q\7\3\2\2\2\16\20\25\33#*.\60\67>" +
      "FLP";
  public static final ATN _ATN =
      new ATNDeserializer().deserialize(_serializedATN.toCharArray());

  static {
    _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
    for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
      _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
    }
  }
}