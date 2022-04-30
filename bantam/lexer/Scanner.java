/**
 * File: Scanner Java
 * Names: Anton Dimitrov, Baron Wang, Phil Bogatyrev
 * Class: CS 361
 * Project 7
 * Date: April 10
 */


package bantam.lexer;

import bantam.util.CompilationException;
import bantam.util.Error;
import bantam.util.ErrorHandler;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

/**
 * This class reads characters from a file or a Reader
 * and breaks it into Tokens.
 * @author Anton Dimitrov, Baron Wang, Phil Bogatyrev
 *
 */
public class Scanner
{
    /** the source of the characters to be broken into tokens */
    private final SourceFile sourceFile;
    /** collector of all errors that occur */
    private final ErrorHandler errorHandler;
    private char currentChar;
    private String tokenContent;
    private boolean inString;


    // hashmap for special symbols and their corresponding tokens
    private final HashMap<String,Token.Kind> kindHashMap = new HashMap<>() {{
        put("{", Token.Kind.LCURLY);
        put("}", Token.Kind.RCURLY);
        put("(", Token.Kind.LPAREN);
        put(")", Token.Kind.RPAREN);
        put(".", Token.Kind.DOT);
        put(";", Token.Kind.SEMICOLON);
        put(":", Token.Kind.COLON);
        put(",", Token.Kind.COMMA);
        put("=", Token.Kind.ASSIGN);
        put("+", Token.Kind.PLUSMINUS);
        put("-", Token.Kind.PLUSMINUS);
        put("*", Token.Kind.MULDIV);
        put("/", Token.Kind.MULDIV);
        put("%", Token.Kind.MULDIV);
        put("!", Token.Kind.UNARYNOT);
        put(">", Token.Kind.COMPARE);
        put("<", Token.Kind.COMPARE);
    }};

    /**
     * creates a new scanner for the given file
     * @param filename the name of the file to be scanned
     * @param handler the ErrorHandler that collects all the errors found
     */
    public Scanner(String filename, ErrorHandler handler) {
        errorHandler = handler;
        currentChar = ' ';
        sourceFile = new SourceFile(filename);
        tokenContent = "";
        inString = false;
    }

    /**
     * creates a new reader for the given file
     * @param reader the name of the file to be scanned
     * @param handler the ErrorHandler that collects all the errors found
     */
    public Scanner(Reader reader, ErrorHandler handler) {
        errorHandler = handler;
        sourceFile = new SourceFile(reader);
        tokenContent = "";
        inString = false;
    }


    /** registers the error in the error handler,
     * wipe the tokenContent, then return the error token
     *
     * @param errorMessage String: error message
     * @return a LEX_ERROR token
     * @author Baron Wang
     */
    private Token lexErrorToken(String errorMessage){
        errorHandler.register(Error.Kind.LEX_ERROR, errorMessage);
        String spel = tokenContent;

        // if current character is whitespace
        // or if current character hits a quotationmark
        // which means it must have hit the end of a
        // STRCONST, simply wipe tokenContent
        if (currentChar == SourceFile.EOL ||
                currentChar == SourceFile.CR ||
                currentChar == (' ') ||
                currentChar == '\t' ||
                currentChar == '"'){
            tokenContent = "";
        }
        else{
            tokenContent = String.valueOf(currentChar);
        }
        return new Token(Token.Kind.ERROR,
                spel,
                lineNum());
    }

    /**
     * returns the current line number.
     *
     * This method is necessary because
     * sourceFile.getCurrentLineNumber
     * returns line number based on currentChar
     * but sometimes currentChar is the
     * new line character, causing the
     * original function to sometimes
     * report one line ahead
     * @return int line number
     */
    private int lineNum(){
        return currentChar == SourceFile.EOL ||
                currentChar == SourceFile.CR ?
                sourceFile.getCurrentLineNumber()-1:
                sourceFile.getCurrentLineNumber();
    }

    /**
     * read characters and collect them into a Token.
     * It ignores white space unless it is inside a string or a comment.
     * It returns an EOF Token if all characters from the sourceFile have
     * already been read.
     * @return the Token containing the characters read
     */
    public Token scan() throws IOException{
            // if it hasn't reached the end of file or tokenContent is an empty string
        while(tokenContent.isEmpty() ||
                    tokenContent.charAt(tokenContent.length() - 1) != SourceFile.EOF )
            {
                currentChar = sourceFile.getNextChar();

                // identifier: if currentChar is alphabetic
                // or if tokenContent is alphabetic
                if (tokenContent.length()==1
                        && Character.isAlphabetic(tokenContent.charAt(0)) )
                {
                    // Instructions:
                    // "starts with an uppercase or lowercase letter
                    // and is followed by a sequence of letters
                    // (upper or lowercase), digits, and underscore '_'."
                    while(Character.isAlphabetic(currentChar)
                            || Character.isDigit(currentChar)
                            || currentChar == '_'){
                        tokenContent += currentChar;
                        currentChar = sourceFile.getNextChar();
                    }
                    // currentChar no longer in IDENTIFIER
                    // could be pointing to whitespace,
                    // special symbols, or anything
                    String tContentCopy = tokenContent;
                    if (currentChar == SourceFile.EOL ||
                            currentChar == SourceFile.CR ||
                            currentChar == (' ')){
                        tokenContent = "";
                    }
                    else{
                        tokenContent = String.valueOf(currentChar);
                    }
                    return new Token(Token.Kind.IDENTIFIER,
                            tContentCopy, lineNum());
                }

                // int constant
                if (tokenContent.length()==1
                        && Character.isDigit(tokenContent.charAt(0))){
                    while(Character.isDigit(currentChar)){
                        tokenContent += currentChar;
                        currentChar = sourceFile.getNextChar();
                    }
                    // check if the integer is too big
                    try {
                        Integer.parseInt(tokenContent);
                        String tContentCopy = tokenContent;

                        if (currentChar == SourceFile.EOL ||
                                currentChar == SourceFile.CR ||
                                currentChar == (' ')){
                            tokenContent = "";
                        }
                        else{
                            tokenContent = String.valueOf(currentChar);
                        }

                        return new Token(Token.Kind.INTCONST,
                                tContentCopy, lineNum());
                    } catch (NumberFormatException e){
                        String falseIntErrMessage = "Integer too large";
                        return lexErrorToken(falseIntErrMessage);
                    }
                }


                // If currentChar is any kind of whitespace
                // out of a string, start interpreting
                // the tokenContent as a token, and move currentChar forward by one
                if (!inString) {
                    while (currentChar == SourceFile.EOL ||
                            currentChar == SourceFile.CR ||
                            currentChar == '\t'||
                            currentChar == (' ')) {

                        // at this point, only tokenContent of length
                        // 1 is possible, since all IDENTIFIER's,
                        // INTCONSTANT's, COMMENT's and STRCONSTANT's
                        // are properly handled and there's only symbols left
                        if (kindHashMap.containsKey(tokenContent)
                        ){
                            String tContentCopy = tokenContent;
                            tokenContent = "";
                            return new Token(kindHashMap.get(tContentCopy),
                                    tContentCopy, lineNum());
                        }
                        else if (tokenContent.equals("|")
                                || tokenContent.equals("&")){
                            return lexErrorToken("Exception:" +
                                    "bitwise logic not supported in bantam");
                        }
                            // if the symbol isn't in the kindhashmap or a binary logic
                        else if (!tokenContent.isEmpty()) {
                            return lexErrorToken("Exception: illegal character");
                        }
                        currentChar = sourceFile.getNextChar();
                        }
                }

                // check if it's a symbol, including binary logic symbols
                if (tokenContent.length() == 1 &&
                        kindHashMap.containsKey(tokenContent)
                    || tokenContent.equals("&") || tokenContent.equals("|") ) {

                    // saving a backup of the string because we
                    // need to check for stuff like ++
                    String prevStr = tokenContent;

                    // double-character tokens handling
                    if (currentChar == '+' || currentChar == '-'){
                        if (currentChar == prevStr.charAt(0)){

                            Token token = currentChar == '+' ?
                                    new Token(Token.Kind.UNARYINCR, "++",
                                            lineNum()) :
                                    new Token(Token.Kind.UNARYDECR, "--",
                                            lineNum());
                            tokenContent="";
                            return token;
                        }
                    }else if (currentChar == '/'){
                        if (currentChar == prevStr.charAt(0)){
                            // inline comment starting with double "/"
                            while (currentChar != SourceFile.CR && currentChar != SourceFile.EOF
                            && currentChar != '\n'){
                                tokenContent += String.valueOf(currentChar);
                                currentChar = sourceFile.getNextChar();
                            }
                            // once currentChar hits end of line, return comment token
                            String prevStr1 = tokenContent;
                            tokenContent = "";
                            continue;
                        }
                        // block comment starting with /* and ending with */
                    }else if (currentChar == '*'){
                        if (prevStr.charAt(0) == '/'){
                            tokenContent = "/";
                            while (currentChar != '/' || tokenContent.charAt(tokenContent.length()-1) != '*'){
                                tokenContent += String.valueOf(currentChar);
                                currentChar = sourceFile.getNextChar();
                                if (currentChar == SourceFile.EOF){
                                    String errorMessage = "Exception: unterminated block comment";
                                    return lexErrorToken(errorMessage);
                                }
                            }
                            // once currentChar hits end of line, return comment token
                            String prevStr1 = tokenContent;
                            tokenContent = "";
                            continue;
                        }
                        // binary logic
                    }else if (prevStr.equals("&") || prevStr.equals("|")){
                        if (currentChar == prevStr.charAt(0)){
                            String tokenSpel = currentChar == '&' ? "&&" : "||";
                            Token token = new Token(Token.Kind.BINARYLOGIC, tokenSpel,
                                            lineNum());
                            tokenContent="";
                            return token;
                        }
                        // if there is only one '&' or '|' ...
                        else {
                            return lexErrorToken("Exception: illegal character." +
                                    "A single & or | character isn't allowed in Bantam.");
                        }
                        // handling COMPARE tokens with two characters (==,!=,<=,>=)
                    } else if (currentChar == '=') {
                        if (prevStr.charAt(0) == '='
                         || prevStr.charAt(0) == '<'
                         || prevStr.charAt(0) == '>'
                         || prevStr.charAt(0) == '!'
                        ) {
                            Token token =
                                    new Token(Token.Kind.COMPARE,
                                            prevStr+"=",
                                            lineNum());
                            tokenContent = "";
                            return token;
                        }
                    }
                    tokenContent = String.valueOf(currentChar);


                    // check if a string is following
                    if (currentChar == '\"'){
                        inString = true;
                    }

                    // assign special symbol kind
                    Token.Kind tokenKind = kindHashMap.get(String.valueOf(prevStr.charAt(0)));
                    return new Token(tokenKind,
                            prevStr,
                            lineNum());
                }
                // string
                else if (tokenContent.length() == 1 && tokenContent.equals( "\"")){
                    // helper variables for error handling
                    boolean hasLegalEscapeCharOnly = true;
                    boolean unterminatedStr = false;
                    // do while loop because the character pointer
                    // (currentChar) needs to move on

                    // empty string -> return early
                    if (currentChar == '"'){
                        tokenContent += currentChar;
                        String strCopy = tokenContent;
                        tokenContent = "";
                        inString = false;
                        return new Token(Token.Kind.STRCONST,strCopy,
                                lineNum());
                    }

                    while (currentChar != '"'){

                        // check for unterminated tokenContent
                        if (currentChar == SourceFile.EOF){
                            unterminatedStr = true;
                            break;
                        }

                        // handling backslash
                        if (currentChar == '\\'){
                            tokenContent += String.valueOf(currentChar);
                            currentChar = sourceFile.getNextChar();
                            // get next char to see if its legal escape char
                            if (!(currentChar == 'n' ||
                                    currentChar == 't' ||
                                    currentChar == '"' ||
                                    currentChar == '\\' ||
                                    currentChar == 'f')){
                                hasLegalEscapeCharOnly = false;
                            }
                        }
                        tokenContent += String.valueOf(currentChar);
                        currentChar = sourceFile.getNextChar();
                    }
                    // appending the last '"'
                    tokenContent += currentChar;


                    // error handling
                    // TODO current tokenContent is always the string with
                    if (unterminatedStr){
                        String errorMessage = "Exception: unterminated string";
                        inString = false;
                        return lexErrorToken(errorMessage);
                    }
                    if (!hasLegalEscapeCharOnly) {
                        // if it has illegal escape character
                        String errorMessage = "Exception: illegal escape character";
                        inString = false;
                        return lexErrorToken(errorMessage);

                    }else if (tokenContent.length() > 5000) {
                        // if it is too long
                        String errorMessage = "Exception: string larger than 5000 chars";
                        inString = false;
                        return lexErrorToken(errorMessage);
                    }

                    else if (tokenContent.contains("\n") || tokenContent.contains("\r")){
                        // if it spans multiple lines
                        String errorMessage = "Exception: spanning multiple lines";
                        inString = false;
                        return lexErrorToken(errorMessage);

                    }

                    String strCopy = tokenContent;
                    tokenContent = "";
                    // exiting string
                    inString = false;
                    return new Token(Token.Kind.STRCONST,strCopy,
                            lineNum());
                }
                // when encounters a special symbol
                // also handling empty string
                else if (kindHashMap.containsKey(String.valueOf(currentChar))
                || currentChar == '"' || currentChar == '&' || currentChar == '|')
                {

                    tokenContent = String.valueOf(currentChar);

                    // if currentChar hit a ", set inString to true
                    // so it won't skip whitespaces
                    if (currentChar == '"'){
                        inString = true;
                    }
                }


                // if current character is part of any stray
                // IDENTIFIER or INTCONST
                // add them to tokenContent
                else if (tokenContent.chars().allMatch(Character::isWhitespace)
                && (Character.isAlphabetic(currentChar)
                || Character.isDigit(currentChar)
                || currentChar == '_')){
                    tokenContent += currentChar;
                }

                // unsupported character
                // either currentChar or tokenContent could be unsupported
                else if (currentChar != SourceFile.EOF){
                    if (tokenContent.isEmpty()){
                        tokenContent = String.valueOf(currentChar);
                        continue;
                    }
                    return lexErrorToken("Exception: illegal character");
                }

                // current character is EOF, and a token needs to be returned
                // -> must be illegal, otherwise it wouldn't reach here
                else if (!tokenContent.isEmpty()){
                    String tContentCopy = tokenContent;
                    // "\u0000" is NOT empty.
                    // So tokenContent can break
                    // out of the loop
                    tokenContent+=currentChar;
                    return new Token(Token.Kind.ERROR,tContentCopy,
                            lineNum());
                // if tokenContent.isEmpty() and EOF just break out of it
                }else{
                    break;
                }
            }
            // if it reached the end of the file, any further
            // calls will return a token of type EOF
            // with a spelling of "" (empty string)
            return new Token(Token.Kind.EOF,"",
                    lineNum());


    }

    public static void main(String[] args) throws IOException {

        for (String arg : args) {
            try {
                ErrorHandler errorHandler = new ErrorHandler();
                Scanner scanner = new Scanner(arg, errorHandler);

                System.out.println(arg);
                Token cur_token = scanner.scan();
                Token prev_token = cur_token;

                while (prev_token.kind != Token.Kind.EOF) {
                    System.out.println(cur_token.toString());
                    prev_token = cur_token;
                    cur_token = scanner.scan();
                }

                if (errorHandler.errorsFound()) {
                    System.out.println(errorHandler.getErrorList().size()
                            + " errors found");
                } else {
                    System.out.println("No errors found");
                }
            } catch (CompilationException e) {
                System.out.println("Compilation Exception");
            }
        }

    }

}


