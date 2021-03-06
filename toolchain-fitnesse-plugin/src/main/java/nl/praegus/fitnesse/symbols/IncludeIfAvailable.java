package nl.praegus.fitnesse.symbols;

import fitnesse.wiki.PathParser;
import fitnesse.wiki.WikiPageProperty;
import fitnesse.wikitext.ParsingPage;
import fitnesse.wikitext.SourcePage;
import fitnesse.wikitext.parser.Collapsible;
import fitnesse.wikitext.parser.Matcher;
import fitnesse.wikitext.parser.Maybe;
import fitnesse.wikitext.parser.Parser;
import fitnesse.wikitext.parser.Rule;
import fitnesse.wikitext.parser.Symbol;
import fitnesse.wikitext.parser.SymbolType;
import fitnesse.wikitext.parser.Translation;
import fitnesse.wikitext.parser.Translator;
import fitnesse.wikitext.parser.WikiWord;

import java.util.Collection;
import java.util.Collections;

public class IncludeIfAvailable extends SymbolType implements Rule, Translation {

    private static final String[] setUpSymbols = new String[] {"COLLAPSE_SETUP"};
    private static final String includeHelpOption = "-h";
    public static final String TEARDOWN = "teardown";

    public IncludeIfAvailable() {
        super("IncludeIfAvailable");
        wikiMatcher(new Matcher().startLineOrCell().string("!includeIfAvailable"));
        wikiRule(this);
        htmlTranslation(this);
    }

    @Override
    public Maybe<Symbol> parse(Symbol current, Parser parser) {
        Symbol next = parser.moveNext(1);
        if (!next.isType(SymbolType.Whitespace)) return Symbol.nothing;

        next = parser.moveNext(1);
        String option = "";
        if ((next.isType(SymbolType.Text) && next.getContent().startsWith("-")) || next.isType(SymbolType.DateFormatOption)) {
            option = next.getContent() + (next.isType(SymbolType.DateFormatOption) ? parser.moveNext(1).getContent() : "");
            next = parser.moveNext(1);
            if (!next.isType(SymbolType.Whitespace)) return Symbol.nothing;
            next = parser.moveNext(1);
        }
        current.add(option);
        if (!next.isType(SymbolType.Text) && !next.isType(WikiWord.symbolType)) return Symbol.nothing;

        StringBuilder includedPageName = new StringBuilder(next.getContent());
        while (parser.peek().isType(SymbolType.Text) || parser.peek().isType(WikiWord.symbolType)) {
            Symbol remainderOfPageName = parser.moveNext(1);
            includedPageName.append(remainderOfPageName.getContent());
        }

        SourcePage sourcePage = parser.getPage().getNamedPage();

        // Record the page name anyway, since we might want to show an error if it's invalid
        if (PathParser.isWikiPath(includedPageName.toString())) {
            current.add(new Symbol(new WikiWord(sourcePage), includedPageName.toString()));
        } else {
            current.add(includedPageName.toString());
        }

        Maybe<SourcePage> includedPage = sourcePage.findIncludedPage(includedPageName.toString());
        if (includedPage.isNothing()) {
            current.add(new Symbol(SymbolType.Style, "meta").add(includedPageName + " will be included when available."));
            return new Maybe<>(current);
        }
        else if (includeHelpOption.equals(option)) {
            String helpText = includedPage.getValue().getProperty(WikiPageProperty.HELP);
            current.add("").add(Parser.make(
                    parser.getPage(),helpText).parse());
        } else {
            current.childAt(1).putProperty(WikiWord.WITH_EDIT, "true");
            ParsingPage included = option.equals("-setup") || option.equals("-teardown")
                    ? parser.getPage()
                    : parser.getPage().copyForNamedPage(includedPage.getValue());
            current.add("").add(Parser.make(
                    included,
                    includedPage.getValue().getContent())
                    .parse());
            if (option.equals("-setup")) current.copyVariables(setUpSymbols, parser.getVariableSource());
        }

        // Remove trailing newline so we do not introduce excessive whitespace in the page.
        if (parser.peek().isType(SymbolType.Newline)) {
            parser.moveNext(1);
        }

        return new Maybe<>(current);
    }

    @Override
    public String toTarget(Translator translator, Symbol symbol) {
        if (symbol.getChildren().size() < 4) {
            return translator.translate(symbol.childAt(2));
        }
        String option = symbol.childAt(0).getContent();
        if (option.equals("-seamless")) {
            return translator.translate(symbol.childAt(3));
        } else if (includeHelpOption.equals(option)) {
            return translator.translate(symbol.childAt(3));
        } else {
            String collapseState = stateForOption(option, symbol);
            String title = "Included page: "
                    + translator.translate(symbol.childAt(1));
            Collection<String> extraCollapsibleClass =
                    option.equals("-teardown") ? Collections.singleton(TEARDOWN) : Collections.emptySet();
            return Collapsible.generateHtml(collapseState, title, translator.translate(symbol.childAt(3)), extraCollapsibleClass);
        }
    }

    private String stateForOption(String option, Symbol symbol) {
        return ((option.equals("-setup") || option.equals("-teardown")) && symbol.findProperty("COLLAPSE_SETUP", "true").equals("true"))
                || option.equals("-c")
                ? Collapsible.CLOSED
                : "";
    }
}
