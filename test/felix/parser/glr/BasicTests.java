package felix.parser.glr;

import static felix.parser.glr.grammar.Symbols.*;
import static felix.parser.glr.grammar.Symbols.kw;
import static felix.parser.glr.grammar.Symbols.nt;
import static felix.parser.glr.grammar.Symbols.re;
import static felix.parser.glr.grammar.Symbols.rule;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import felix.parser.glr.grammar.Grammar;
import felix.parser.glr.grammar.KeywordTerminal;
import felix.parser.glr.grammar.Marker;
import felix.parser.glr.grammar.NonTerminal;
import felix.parser.glr.grammar.Priority;
import felix.parser.glr.grammar.Sequence;
import felix.parser.glr.grammar.Symbol;
import felix.parser.glr.grammar.SymbolRef;
import felix.parser.glr.grammar.Terminal;
import felix.parser.glr.parsetree.Element;
import felix.parser.glr.parsetree.Node;
import felix.parser.glr.parsetree.Token;
import felix.parser.util.FilePos;
import felix.parser.util.FileRange;
import felix.parser.util.ParserReader;
 
public class BasicTests {
	private static final String TEST_FILENAME = "<string>";
	final Terminal NUM = re("NUM", "[0-9]+");
	final Terminal ID = re("ID", "\\p{Alpha}\\w*");
	final KeywordTerminal PLUS = kw("+");
	final KeywordTerminal TIMES = kw("*");
	final KeywordTerminal COMMA = kw(",");
	final Terminal WS = re("WS", "\\s+");
	final Terminal SL_COMMENT = re("SL_COMMENT", "\\s*//[^\n]*\\s*");
	final Terminal ML_COMMENT = re("ML_COMMENT", "\\s*/\\*.*?\\*/\\s*");
	
	final Set<Terminal> ignore = new TreeSet<>(Arrays.asList(WS, SL_COMMENT, ML_COMMENT));
	
	static void assertEqualTrees(String message, Node expected, Node actual, String path) {
		if(expected.equals(actual))
			return;
		
		String msg = path!=null ? "In "+path+(message != null?": "+message:""):message;
		
		// See if they different by string
		assertEquals(msg, expected.toString(), actual.toString());
		// Different types?
		assertEquals(msg, expected.getClass(), actual.getClass());
		// Different file position?
		assertEquals(msg, expected.getFileRange().toString(), actual.getFileRange().toString());
		assertEquals(msg, expected.getFileRange(), actual.getFileRange());
		
		assertEquals(msg, expected.getChildCount(), actual.getChildCount());
		int count = expected.getChildCount();
		for(int i=0; i < count; i++) {
			Node expectedChild = expected.getChild(i);
			Node actualChild = actual.getChild(i);
			assertEqualTrees(message, expectedChild, actualChild, path+"["+i+"]");
		}
		
		assertEquals(expected, actual);
	}
	
	static void assertEqualTrees(Node expected, Node actual) {
		assertEqualTrees(null, expected, actual, null);
	}
	
	Node _parse(String str, Symbol root, Symbol ... symbols) throws IOException, ParseException {
		Grammar grammar = new Grammar(root, ignore);
		final Node parseResult = grammar.parse(str, TEST_FILENAME);
		System.out.println("Parse result: "+parseResult);
		return parseResult;
	}
	
	Token parse(String str, Terminal term, Symbol ... symbols) throws IOException, ParseException {
		return (Token)_parse(str, term, symbols);
	}
	Element parse(String str, NonTerminal rule, Symbol ... symbols) throws IOException, ParseException {
		return (Element)_parse(str, rule, symbols);
	}
	
	FileRange range(int start, int end) {
		return new FileRange(TEST_FILENAME, new FilePos(start, 1, start+1), new FilePos(end, 1, end+1));
	}
	
	Token tok(String src, Symbol symbol, int startIndex, int endIndex) throws IOException {
		final ParserReader reader = new ParserReader(new StringReader(src), TEST_FILENAME, src.length());
		try {
			reader.seek(startIndex);
			FilePos startPos = reader.getFilePos();
			assertEquals(startIndex, startPos.offset);
			reader.seek(endIndex);
			FilePos endPos = reader.getFilePos();
			assertEquals(endIndex, endPos.offset);
			String text = src.substring(startIndex, endIndex);
			return new Token(new FileRange(TEST_FILENAME, startPos, endPos), symbol, text, "");
		} finally { reader.close(); }
	}
	Token tok(String src, Symbol symbol, int startIndex, String expectedText) throws IOException {
		final Token t = tok(src, symbol, startIndex, startIndex+expectedText.length());
		assertEquals("Token position is off - the test needs to be fixed.", expectedText, t.getText());
		return t;
	}
	Token tok(String src, KeywordTerminal symbol, int startIndex) throws IOException {
		return tok(src, symbol, startIndex, symbol.text);
	}
	
	@Test
	public void parseSingleNumber() throws Exception {
		Token token = (Token) parse("5", NUM);
		assertEquals(token.getText(), "5");
		assertEquals(token.symbol, NUM);
	}
	
	@Test
	public void parseAddition() throws Exception {
		String src = "12+34";
		NonTerminal sum = nt("Sum", NUM, PLUS, NUM);
		Element node = parse(src, sum, NUM, PLUS);
		Token expectLeft = tok(src, NUM, 0, "12");
		Token expectOp = tok(src, PLUS, 2, "+");
		Token expectRight = tok(src, NUM, 3, "34");
		Token expecteds[] = {expectLeft, expectOp, expectRight};
		assertArrayEquals(expecteds, node.children);
	}

	@Test
	public void parseAdditionWithWs() throws Exception {
		String input = " 12 + 34 ";
		NonTerminal sum = nt("Sum", NUM, PLUS, NUM);
		Element node = parse(input, sum, NUM, PLUS);
		Token expectLeft = tok(input, NUM, 1, "12");
		Token expectOp = tok(input, PLUS, 4, "+");
		Token expectRight = tok(input, NUM, 6, "34");
		Token expecteds[] = {expectLeft, expectOp, expectRight};
		assertArrayEquals(expecteds, node.children);
		checkIgnored(node, new String[] {" ", " ", " "});
	}

	private void checkIgnored(Element node, String[] ignoredExpected) {
		String[] ignoredActual = new String[node.children.length];
		int i=0;
		for(Node n : node.children) {
			ignoredActual[i] = ((Token)n).getIgnoredPrefix();
			i++;
		}
		assertArrayEquals(ignoredExpected, ignoredActual);
	}

	@Test
	public void parseAdditionWithInlineComments() throws Exception {
		String input = "/*pre*/12/*mid*/+/*mid2*/34/*after*/";
		NonTerminal sum = nt("Sum", NUM, PLUS, NUM);
		Element node = parse(input, sum, NUM, PLUS, sum);
		Token expectLeft = tok(input, NUM, 7, "12");
		Token expectOp = tok(input, PLUS, 16, "+");
		Token expectRight = tok(input, NUM, 25, "34");
		Token expecteds[] = {expectLeft, expectOp, expectRight};
		assertArrayEquals(expecteds, node.children);
		checkIgnored(node, new String[] {"/*pre*/", "/*mid*/", "/*mid2*/"});
	}
	
	@Test
	public void parseAdditionWithSingleLineComments() throws Exception {
		String input = "//pre\n 12 //mid\n + //mid2\n 34 // after";
		NonTerminal sum = nt("Sum", NUM, PLUS, NUM);
		Element node = parse(input, sum, NUM, PLUS, sum);
		Token expectLeft = tok(input, NUM, 7, "12");
		Token expectOp = tok(input, PLUS, 17, "+");
		Token expectRight = tok(input, NUM, 27, "34");
		Token expecteds[] = {expectLeft, expectOp, expectRight};
		assertArrayEquals(expecteds, node.children);
		checkIgnored(node, new String[] {"//pre\n ", " //mid\n ", " //mid2\n "});
	}
	
	@Test
	public void parseSumAndProductWithPrecedence1() throws Exception {
		Parser.debug = true;
		String src = "12*34+56*78";
		Symbol _expr = new SymbolRef("Expr");
		Priority ps = new Priority("ps"); // Sum priority
		Priority pp = new Priority("pp", ps); // Product priority (generally * and /)
		Priority pi = new Priority("pi", pp, ps); // Highest priority: literal integer
		
		NonTerminal expr = nt("Expr", 
				rule(ps, _expr.gt(ps), PLUS, _expr.ge(ps)), 
				rule(pp, _expr.gt(pp), TIMES, _expr.ge(pp)), 
				rule(pi, NUM));
		Element node = parse(src, expr);
		
		System.out.println(node);
		// Should be (12*34) + (56*78), or
		// Sum(Expr(Product(Expr(NUM(12)),*,Expr(NUM(34)))),Expr(Product(Expr(NUM(56)),Expr(NUM(78)))))
		Token _12 = tok(src, NUM, 0, "12");
		Token star = tok(src, TIMES, 2);
		Token _34 = tok(src, NUM, 3, "34");
		Token plus = tok(src, PLUS, 5);
		Token _56 = tok(src, NUM, 6, "56");
		Token star2 = tok(src, TIMES, 8);
		Token _78 = tok(src, NUM, 9, "78");
		
		Node expected = expr.build(
				expr.build(expr.build(_12), star, expr.build(_34)),
				plus,
				expr.build(expr.build(_56), star2, expr.build(_78))
				);
		assertEquals(expected, node);
	}

	@Test
	public void parseSumAndProductWithPrecedence4() throws Exception {
		String src = "12+34*56+78";
		Symbol _expr = new SymbolRef("Expr");
		Priority ps = new Priority("ps"); // Sum priority
		Priority pp = new Priority("pp", ps); // Product priority (generally * and /)
		Priority pi = new Priority("pi", pp, ps); // Highest priority: literal integer
		
		NonTerminal expr = nt("Expr", 
				rule(ps, _expr.gt(ps), PLUS, _expr.ge(ps)), 
				rule(pp, _expr.gt(pp), TIMES, _expr.ge(pp)), 
				rule(pi, NUM));
		Element node = parse(src, expr);
		
		System.out.println(node);
		// Should be 12+((34*56)+78)
		Token _12 = tok(src, NUM, 0, "12");
		Token plus = tok(src, PLUS, 2);
		Token _34 = tok(src, NUM, 3, "34");
		Token star = tok(src, TIMES, 5);
		Token _56 = tok(src, NUM, 6, "56");
		Token plus2 = tok(src, PLUS, 8);
		Token _78 = tok(src, NUM, 9, "78");
		
		Node expected = expr.build(expr.build(_12), plus,
				expr.build(
						expr.build(expr.build(_34), star, expr.build(_56)),
						plus2,
						expr.build(_78)
				));
		assertEquals(expected, node);
		
	}
	
	@Test
	public void parseOptionalSuffix() throws Exception {
		Symbol maybePair = nt("maybe_pair", ID, opt(ID));
		Element pair = (Element) maybePair.parse("a b", ignore);
		System.out.println("pair:"+pair);
		assertEquals(2, pair.children.length);
		assertEquals("a", ((Token)pair.children[0]).getText());
		Element opt_b = ((Element)pair.children[1]);
		assertEquals(1, opt_b.children.length);
		assertEquals("b", ((Token)opt_b.children[0]).getText());
		Element single = (Element) maybePair.parse("c", ignore);
		System.out.println("single:"+single);
		assertEquals(2, single.children.length);
		assertEquals("c", ((Token)single.children[0]).getText());
		Element opt_nil = ((Element)single.children[1]);
		assertEquals(0, opt_nil.children.length);
	}
	
	@Test
	public void parseOptionalCommaSuffix() throws Exception {
		Parser.debug = true;
		final Sequence optSuffix = opt(COMMA, ID);
		Symbol maybePair = nt("maybe_pair", ID, optSuffix);
		final String pairSrc = "a,b";
		Element pair = (Element) maybePair.parse(pairSrc);
		System.out.println("pair:"+pair);
		final String singleSrc = "c";
		Element single = (Element) maybePair.parse(singleSrc);
		System.out.println("single:"+single);
		Node pairExpected = maybePair.build(tok(pairSrc, ID, 0, "a"), 
				optSuffix.build(optSuffix.item.build(tok(pairSrc, COMMA, 1), tok(pairSrc, ID, 2, "b"))));
		assertEqualTrees(pairExpected, pair);
		Node singleExpected = maybePair.build(tok(singleSrc, ID, 0, singleSrc), optSuffix.build(tok(singleSrc, Marker.NIL, 1, "")));
		assertEqualTrees(singleExpected, single);
	}
	
	@Test
	public void parseList() throws Exception {
		String src="a b c d e";
		Symbol idList = new Sequence("id_list", ID, Sequence.Mode.ONE_OR_MORE);
		Parser.debug = true;
		final Element list = (Element) idList.parse(src, ignore);
		System.out.println("list result node: "+list);
		assertEqualTrees(new Element(idList,
				tok(src, ID, 0, "a"),
				tok(src, ID, 2, "b"),
				tok(src, ID, 4, "c"),
				tok(src, ID, 6, "d"),
				tok(src, ID, 8, "e")
				), list);
	}
	
	@Test
	public void parseCommaList() throws Exception {
		String src="a,b,c,d,e";
		Symbol idList = new Sequence("id_list", ID, Sequence.Mode.ONE_OR_MORE, COMMA);
		Parser.debug = true;
		final Element list = (Element) idList.parse(src, ignore);
		System.out.println("comma list result node: "+list);
		assertEquals(9, list.children.length);
		assertEqualTrees(new Element(idList,
				tok(src, ID, 0, "a"),
				tok(src, COMMA, 1),
				tok(src, ID, 2, "b"),
				tok(src, COMMA, 3),
				tok(src, ID, 4, "c"),
				tok(src, COMMA, 5),
				tok(src, ID, 6, "d"),
				tok(src, COMMA, 7),
				tok(src, ID, 8, "e")
				), list);
	}
}
