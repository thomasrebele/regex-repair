// Generated from RegexGrammar.g4 by ANTLR 4.5.3
package fr.telecom_paristech.dbweb.regexrepair.adaptive.parser;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link RegexGrammarParser}.
 */
public interface RegexGrammarListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#parse}.
	 * @param ctx the parse tree
	 */
	void enterParse(RegexGrammarParser.ParseContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#parse}.
	 * @param ctx the parse tree
	 */
	void exitParse(RegexGrammarParser.ParseContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#alternation}.
	 * @param ctx the parse tree
	 */
	void enterAlternation(RegexGrammarParser.AlternationContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#alternation}.
	 * @param ctx the parse tree
	 */
	void exitAlternation(RegexGrammarParser.AlternationContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(RegexGrammarParser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(RegexGrammarParser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#element}.
	 * @param ctx the parse tree
	 */
	void enterElement(RegexGrammarParser.ElementContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#element}.
	 * @param ctx the parse tree
	 */
	void exitElement(RegexGrammarParser.ElementContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#quantifier}.
	 * @param ctx the parse tree
	 */
	void enterQuantifier(RegexGrammarParser.QuantifierContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#quantifier}.
	 * @param ctx the parse tree
	 */
	void exitQuantifier(RegexGrammarParser.QuantifierContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#quantifier_type}.
	 * @param ctx the parse tree
	 */
	void enterQuantifier_type(RegexGrammarParser.Quantifier_typeContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#quantifier_type}.
	 * @param ctx the parse tree
	 */
	void exitQuantifier_type(RegexGrammarParser.Quantifier_typeContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#character_class}.
	 * @param ctx the parse tree
	 */
	void enterCharacter_class(RegexGrammarParser.Character_classContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#character_class}.
	 * @param ctx the parse tree
	 */
	void exitCharacter_class(RegexGrammarParser.Character_classContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#backreference}.
	 * @param ctx the parse tree
	 */
	void enterBackreference(RegexGrammarParser.BackreferenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#backreference}.
	 * @param ctx the parse tree
	 */
	void exitBackreference(RegexGrammarParser.BackreferenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#backreference_or_octal}.
	 * @param ctx the parse tree
	 */
	void enterBackreference_or_octal(RegexGrammarParser.Backreference_or_octalContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#backreference_or_octal}.
	 * @param ctx the parse tree
	 */
	void exitBackreference_or_octal(RegexGrammarParser.Backreference_or_octalContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#capture}.
	 * @param ctx the parse tree
	 */
	void enterCapture(RegexGrammarParser.CaptureContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#capture}.
	 * @param ctx the parse tree
	 */
	void exitCapture(RegexGrammarParser.CaptureContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#non_capture}.
	 * @param ctx the parse tree
	 */
	void enterNon_capture(RegexGrammarParser.Non_captureContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#non_capture}.
	 * @param ctx the parse tree
	 */
	void exitNon_capture(RegexGrammarParser.Non_captureContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#comment}.
	 * @param ctx the parse tree
	 */
	void enterComment(RegexGrammarParser.CommentContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#comment}.
	 * @param ctx the parse tree
	 */
	void exitComment(RegexGrammarParser.CommentContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#option}.
	 * @param ctx the parse tree
	 */
	void enterOption(RegexGrammarParser.OptionContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#option}.
	 * @param ctx the parse tree
	 */
	void exitOption(RegexGrammarParser.OptionContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#option_flags}.
	 * @param ctx the parse tree
	 */
	void enterOption_flags(RegexGrammarParser.Option_flagsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#option_flags}.
	 * @param ctx the parse tree
	 */
	void exitOption_flags(RegexGrammarParser.Option_flagsContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#option_flag}.
	 * @param ctx the parse tree
	 */
	void enterOption_flag(RegexGrammarParser.Option_flagContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#option_flag}.
	 * @param ctx the parse tree
	 */
	void exitOption_flag(RegexGrammarParser.Option_flagContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#look_around}.
	 * @param ctx the parse tree
	 */
	void enterLook_around(RegexGrammarParser.Look_aroundContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#look_around}.
	 * @param ctx the parse tree
	 */
	void exitLook_around(RegexGrammarParser.Look_aroundContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#subroutine_reference}.
	 * @param ctx the parse tree
	 */
	void enterSubroutine_reference(RegexGrammarParser.Subroutine_referenceContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#subroutine_reference}.
	 * @param ctx the parse tree
	 */
	void exitSubroutine_reference(RegexGrammarParser.Subroutine_referenceContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#conditional}.
	 * @param ctx the parse tree
	 */
	void enterConditional(RegexGrammarParser.ConditionalContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#conditional}.
	 * @param ctx the parse tree
	 */
	void exitConditional(RegexGrammarParser.ConditionalContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#backtrack_control}.
	 * @param ctx the parse tree
	 */
	void enterBacktrack_control(RegexGrammarParser.Backtrack_controlContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#backtrack_control}.
	 * @param ctx the parse tree
	 */
	void exitBacktrack_control(RegexGrammarParser.Backtrack_controlContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#newline_convention}.
	 * @param ctx the parse tree
	 */
	void enterNewline_convention(RegexGrammarParser.Newline_conventionContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#newline_convention}.
	 * @param ctx the parse tree
	 */
	void exitNewline_convention(RegexGrammarParser.Newline_conventionContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#callout}.
	 * @param ctx the parse tree
	 */
	void enterCallout(RegexGrammarParser.CalloutContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#callout}.
	 * @param ctx the parse tree
	 */
	void exitCallout(RegexGrammarParser.CalloutContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#atom}.
	 * @param ctx the parse tree
	 */
	void enterAtom(RegexGrammarParser.AtomContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#atom}.
	 * @param ctx the parse tree
	 */
	void exitAtom(RegexGrammarParser.AtomContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#cc_atom}.
	 * @param ctx the parse tree
	 */
	void enterCc_atom(RegexGrammarParser.Cc_atomContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#cc_atom}.
	 * @param ctx the parse tree
	 */
	void exitCc_atom(RegexGrammarParser.Cc_atomContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#shared_atom}.
	 * @param ctx the parse tree
	 */
	void enterShared_atom(RegexGrammarParser.Shared_atomContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#shared_atom}.
	 * @param ctx the parse tree
	 */
	void exitShared_atom(RegexGrammarParser.Shared_atomContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#literal}.
	 * @param ctx the parse tree
	 */
	void enterLiteral(RegexGrammarParser.LiteralContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#literal}.
	 * @param ctx the parse tree
	 */
	void exitLiteral(RegexGrammarParser.LiteralContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#cc_literal}.
	 * @param ctx the parse tree
	 */
	void enterCc_literal(RegexGrammarParser.Cc_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#cc_literal}.
	 * @param ctx the parse tree
	 */
	void exitCc_literal(RegexGrammarParser.Cc_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#shared_literal}.
	 * @param ctx the parse tree
	 */
	void enterShared_literal(RegexGrammarParser.Shared_literalContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#shared_literal}.
	 * @param ctx the parse tree
	 */
	void exitShared_literal(RegexGrammarParser.Shared_literalContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#number}.
	 * @param ctx the parse tree
	 */
	void enterNumber(RegexGrammarParser.NumberContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#number}.
	 * @param ctx the parse tree
	 */
	void exitNumber(RegexGrammarParser.NumberContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#octal_char}.
	 * @param ctx the parse tree
	 */
	void enterOctal_char(RegexGrammarParser.Octal_charContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#octal_char}.
	 * @param ctx the parse tree
	 */
	void exitOctal_char(RegexGrammarParser.Octal_charContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#octal_digit}.
	 * @param ctx the parse tree
	 */
	void enterOctal_digit(RegexGrammarParser.Octal_digitContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#octal_digit}.
	 * @param ctx the parse tree
	 */
	void exitOctal_digit(RegexGrammarParser.Octal_digitContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#digits}.
	 * @param ctx the parse tree
	 */
	void enterDigits(RegexGrammarParser.DigitsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#digits}.
	 * @param ctx the parse tree
	 */
	void exitDigits(RegexGrammarParser.DigitsContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#digit}.
	 * @param ctx the parse tree
	 */
	void enterDigit(RegexGrammarParser.DigitContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#digit}.
	 * @param ctx the parse tree
	 */
	void exitDigit(RegexGrammarParser.DigitContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#name}.
	 * @param ctx the parse tree
	 */
	void enterName(RegexGrammarParser.NameContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#name}.
	 * @param ctx the parse tree
	 */
	void exitName(RegexGrammarParser.NameContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#alpha_nums}.
	 * @param ctx the parse tree
	 */
	void enterAlpha_nums(RegexGrammarParser.Alpha_numsContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#alpha_nums}.
	 * @param ctx the parse tree
	 */
	void exitAlpha_nums(RegexGrammarParser.Alpha_numsContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#non_close_parens}.
	 * @param ctx the parse tree
	 */
	void enterNon_close_parens(RegexGrammarParser.Non_close_parensContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#non_close_parens}.
	 * @param ctx the parse tree
	 */
	void exitNon_close_parens(RegexGrammarParser.Non_close_parensContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#non_close_paren}.
	 * @param ctx the parse tree
	 */
	void enterNon_close_paren(RegexGrammarParser.Non_close_parenContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#non_close_paren}.
	 * @param ctx the parse tree
	 */
	void exitNon_close_paren(RegexGrammarParser.Non_close_parenContext ctx);
	/**
	 * Enter a parse tree produced by {@link RegexGrammarParser#letter}.
	 * @param ctx the parse tree
	 */
	void enterLetter(RegexGrammarParser.LetterContext ctx);
	/**
	 * Exit a parse tree produced by {@link RegexGrammarParser#letter}.
	 * @param ctx the parse tree
	 */
	void exitLetter(RegexGrammarParser.LetterContext ctx);
}