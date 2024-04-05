// Generated from TagQueryLanguage.g4 by ANTLR 4.9.3

    package net.darmo_creations.imageslibrary.query_parser.generated;

import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TagQueryLanguageParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.9.3", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, WS=6, IDENT=7, FLAG=8, STRING=9, 
		REGEX=10;
	public static final int
		RULE_expr = 0, RULE_lit = 1;
	private static String[] makeRuleNames() {
		return new String[] {
			"expr", "lit"
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
			null, null, null, null, null, null, "WS", "IDENT", "FLAG", "STRING", 
			"REGEX"
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
	public String getGrammarFileName() { return "TagQueryLanguage.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public TagQueryLanguageParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class ExprContext extends ParserRuleContext {
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
	 
		public ExprContext() { }
		public void copyFrom(ExprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class OrContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> WS() { return getTokens(TagQueryLanguageParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(TagQueryLanguageParser.WS, i);
		}
		public OrContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TagQueryLanguageVisitor ) return ((TagQueryLanguageVisitor<? extends T>)visitor).visitOr(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class NegationContext extends ExprContext {
		public LitContext lit() {
			return getRuleContext(LitContext.class,0);
		}
		public List<TerminalNode> WS() { return getTokens(TagQueryLanguageParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(TagQueryLanguageParser.WS, i);
		}
		public NegationContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TagQueryLanguageVisitor ) return ((TagQueryLanguageVisitor<? extends T>)visitor).visitNegation(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class AndContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public List<TerminalNode> WS() { return getTokens(TagQueryLanguageParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(TagQueryLanguageParser.WS, i);
		}
		public AndContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TagQueryLanguageVisitor ) return ((TagQueryLanguageVisitor<? extends T>)visitor).visitAnd(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class LiteralContext extends ExprContext {
		public LitContext lit() {
			return getRuleContext(LitContext.class,0);
		}
		public LiteralContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TagQueryLanguageVisitor ) return ((TagQueryLanguageVisitor<? extends T>)visitor).visitLiteral(this);
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
		int _startState = 0;
		enterRecursionRule(_localctx, 0, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(14);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__1:
				{
				_localctx = new NegationContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(5);
				match(T__1);
				setState(9);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==WS) {
					{
					{
					setState(6);
					match(WS);
					}
					}
					setState(11);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(12);
				lit();
				}
				break;
			case T__2:
			case IDENT:
				{
				_localctx = new LiteralContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(13);
				lit();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(41);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(39);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,5,_ctx) ) {
					case 1:
						{
						_localctx = new AndContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(16);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(20);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==WS) {
							{
							{
							setState(17);
							match(WS);
							}
							}
							setState(22);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						setState(23);
						expr(5);
						}
						break;
					case 2:
						{
						_localctx = new OrContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(24);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(28);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==WS) {
							{
							{
							setState(25);
							match(WS);
							}
							}
							setState(30);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						setState(31);
						match(T__0);
						setState(35);
						_errHandler.sync(this);
						_la = _input.LA(1);
						while (_la==WS) {
							{
							{
							setState(32);
							match(WS);
							}
							}
							setState(37);
							_errHandler.sync(this);
							_la = _input.LA(1);
						}
						setState(38);
						expr(4);
						}
						break;
					}
					} 
				}
				setState(43);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,6,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class LitContext extends ParserRuleContext {
		public LitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_lit; }
	 
		public LitContext() { }
		public void copyFrom(LitContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class GroupContext extends LitContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public List<TerminalNode> WS() { return getTokens(TagQueryLanguageParser.WS); }
		public TerminalNode WS(int i) {
			return getToken(TagQueryLanguageParser.WS, i);
		}
		public GroupContext(LitContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TagQueryLanguageVisitor ) return ((TagQueryLanguageVisitor<? extends T>)visitor).visitGroup(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PseudoTagRegexContext extends LitContext {
		public TerminalNode IDENT() { return getToken(TagQueryLanguageParser.IDENT, 0); }
		public TerminalNode REGEX() { return getToken(TagQueryLanguageParser.REGEX, 0); }
		public TerminalNode FLAG() { return getToken(TagQueryLanguageParser.FLAG, 0); }
		public PseudoTagRegexContext(LitContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TagQueryLanguageVisitor ) return ((TagQueryLanguageVisitor<? extends T>)visitor).visitPseudoTagRegex(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class PseudoTagStringContext extends LitContext {
		public TerminalNode IDENT() { return getToken(TagQueryLanguageParser.IDENT, 0); }
		public TerminalNode STRING() { return getToken(TagQueryLanguageParser.STRING, 0); }
		public TerminalNode FLAG() { return getToken(TagQueryLanguageParser.FLAG, 0); }
		public PseudoTagStringContext(LitContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TagQueryLanguageVisitor ) return ((TagQueryLanguageVisitor<? extends T>)visitor).visitPseudoTagString(this);
			else return visitor.visitChildren(this);
		}
	}
	public static class TagContext extends LitContext {
		public TerminalNode IDENT() { return getToken(TagQueryLanguageParser.IDENT, 0); }
		public TagContext(LitContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TagQueryLanguageVisitor ) return ((TagQueryLanguageVisitor<? extends T>)visitor).visitTag(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LitContext lit() throws RecognitionException {
		LitContext _localctx = new LitContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_lit);
		int _la;
		try {
			setState(73);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
			case 1:
				_localctx = new GroupContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(44);
				match(T__2);
				setState(48);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==WS) {
					{
					{
					setState(45);
					match(WS);
					}
					}
					setState(50);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(51);
				expr(0);
				setState(55);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==WS) {
					{
					{
					setState(52);
					match(WS);
					}
					}
					setState(57);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(58);
				match(T__3);
				}
				break;
			case 2:
				_localctx = new PseudoTagStringContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(60);
				match(IDENT);
				setState(61);
				match(T__4);
				setState(63);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FLAG) {
					{
					setState(62);
					match(FLAG);
					}
				}

				setState(65);
				match(STRING);
				}
				break;
			case 3:
				_localctx = new PseudoTagRegexContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(66);
				match(IDENT);
				setState(67);
				match(T__4);
				setState(69);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==FLAG) {
					{
					setState(68);
					match(FLAG);
					}
				}

				setState(71);
				match(REGEX);
				}
				break;
			case 4:
				_localctx = new TagContext(_localctx);
				enterOuterAlt(_localctx, 4);
				{
				setState(72);
				match(IDENT);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 0:
			return expr_sempred((ExprContext)_localctx, predIndex);
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\fN\4\2\t\2\4\3\t"+
		"\3\3\2\3\2\3\2\7\2\n\n\2\f\2\16\2\r\13\2\3\2\3\2\5\2\21\n\2\3\2\3\2\7"+
		"\2\25\n\2\f\2\16\2\30\13\2\3\2\3\2\3\2\7\2\35\n\2\f\2\16\2 \13\2\3\2\3"+
		"\2\7\2$\n\2\f\2\16\2\'\13\2\3\2\7\2*\n\2\f\2\16\2-\13\2\3\3\3\3\7\3\61"+
		"\n\3\f\3\16\3\64\13\3\3\3\3\3\7\38\n\3\f\3\16\3;\13\3\3\3\3\3\3\3\3\3"+
		"\3\3\5\3B\n\3\3\3\3\3\3\3\3\3\5\3H\n\3\3\3\3\3\5\3L\n\3\3\3\2\3\2\4\2"+
		"\4\2\2\2Y\2\20\3\2\2\2\4K\3\2\2\2\6\7\b\2\1\2\7\13\7\4\2\2\b\n\7\b\2\2"+
		"\t\b\3\2\2\2\n\r\3\2\2\2\13\t\3\2\2\2\13\f\3\2\2\2\f\16\3\2\2\2\r\13\3"+
		"\2\2\2\16\21\5\4\3\2\17\21\5\4\3\2\20\6\3\2\2\2\20\17\3\2\2\2\21+\3\2"+
		"\2\2\22\26\f\6\2\2\23\25\7\b\2\2\24\23\3\2\2\2\25\30\3\2\2\2\26\24\3\2"+
		"\2\2\26\27\3\2\2\2\27\31\3\2\2\2\30\26\3\2\2\2\31*\5\2\2\7\32\36\f\5\2"+
		"\2\33\35\7\b\2\2\34\33\3\2\2\2\35 \3\2\2\2\36\34\3\2\2\2\36\37\3\2\2\2"+
		"\37!\3\2\2\2 \36\3\2\2\2!%\7\3\2\2\"$\7\b\2\2#\"\3\2\2\2$\'\3\2\2\2%#"+
		"\3\2\2\2%&\3\2\2\2&(\3\2\2\2\'%\3\2\2\2(*\5\2\2\6)\22\3\2\2\2)\32\3\2"+
		"\2\2*-\3\2\2\2+)\3\2\2\2+,\3\2\2\2,\3\3\2\2\2-+\3\2\2\2.\62\7\5\2\2/\61"+
		"\7\b\2\2\60/\3\2\2\2\61\64\3\2\2\2\62\60\3\2\2\2\62\63\3\2\2\2\63\65\3"+
		"\2\2\2\64\62\3\2\2\2\659\5\2\2\2\668\7\b\2\2\67\66\3\2\2\28;\3\2\2\29"+
		"\67\3\2\2\29:\3\2\2\2:<\3\2\2\2;9\3\2\2\2<=\7\6\2\2=L\3\2\2\2>?\7\t\2"+
		"\2?A\7\7\2\2@B\7\n\2\2A@\3\2\2\2AB\3\2\2\2BC\3\2\2\2CL\7\13\2\2DE\7\t"+
		"\2\2EG\7\7\2\2FH\7\n\2\2GF\3\2\2\2GH\3\2\2\2HI\3\2\2\2IL\7\f\2\2JL\7\t"+
		"\2\2K.\3\2\2\2K>\3\2\2\2KD\3\2\2\2KJ\3\2\2\2L\5\3\2\2\2\16\13\20\26\36"+
		"%)+\629AGK";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}