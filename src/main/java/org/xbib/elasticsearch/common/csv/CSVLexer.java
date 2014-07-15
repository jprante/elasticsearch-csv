package org.xbib.elasticsearch.common.csv;

import java.io.IOException;

class CSVLexer implements Constants {

    private final LookAheadReader reader;

    private final char delimiter;

    private final char escape;

    private final char quoteChar;

    private final char commentStart;

    private final boolean ignoreSurroundingSpaces;

    private final boolean ignoreEmptyLines;

    CSVLexer(LookAheadReader reader, char delimiter, char escape, char quoteChar, char commentStart,
             boolean ignoreSurroundingSpaces, boolean ignoreEmptyLines) {
        this.reader = reader;
        this.delimiter = delimiter;
        this.escape = escape;
        this.quoteChar = quoteChar;
        this.commentStart = commentStart;
        this.ignoreSurroundingSpaces = ignoreSurroundingSpaces;
        this.ignoreEmptyLines = ignoreEmptyLines;
    }

    Token nextToken(final Token token) throws IOException {
        int lastChar = reader.getLastChar();
        int c = reader.read();
        boolean eol = readEndOfLine(c);
        if (ignoreEmptyLines) {
            while (eol && isStartOfLine(lastChar)) {
                lastChar = c;
                c = reader.read();
                eol = readEndOfLine(c);
                if (isEndOfFile(c)) {
                    token.type = Token.Type.EOF;
                    return token;
                }
            }
        }
        if (isEndOfFile(lastChar) || (!isDelimiter(lastChar) && isEndOfFile(c))) {
            token.type = Token.Type.EOF;
            return token;
        }
        if (isStartOfLine(lastChar) && isCommentStart(c)) {
            final String line = reader.readLine();
            if (line == null) {
                token.type = Token.Type.EOF;
                return token;
            }
            final String comment = line.trim();
            token.content.append(comment);
            token.type = Token.Type.COMMENT;
            return token;
        }
        while (token.type == Token.Type.INVALID) {
            if (ignoreSurroundingSpaces) {
                while (isWhitespace(c) && !eol) {
                    c = reader.read();
                    eol = readEndOfLine(c);
                }
            }
            if (isDelimiter(c)) {
                token.type = Token.Type.TOKEN;
            } else if (eol) {
                token.type = Token.Type.EORECORD;
            } else if (isQuoteChar(c)) {
                parseEncapsulatedToken(token);
            } else if (isEndOfFile(c)) {
                token.type = Token.Type.EOF;
                token.isReady = true;
            } else {
                parseSimpleToken(token, c);
            }
        }
        return token;
    }

    private Token parseSimpleToken(final Token token, int ch) throws IOException {
        while (true) {
            if (readEndOfLine(ch)) {
                token.type = Token.Type.EORECORD;
                break;
            } else if (isEndOfFile(ch)) {
                token.type = Token.Type.EOF;
                token.isReady = true;
                break;
            } else if (isDelimiter(ch)) {
                token.type = Token.Type.TOKEN;
                break;
            } else if (isEscape(ch)) {
                final int unescaped = readEscape();
                if (unescaped == END_OF_STREAM) {
                    token.content.append((char) ch).append((char) reader.getLastChar());
                } else {
                    token.content.append((char) unescaped);
                }
                ch = reader.read();
            } else {
                token.content.append((char) ch);
                ch = reader.read();
            }
        }
        if (ignoreSurroundingSpaces) {
            trimTrailingSpaces(token.content);
        }
        return token;
    }

    private Token parseEncapsulatedToken(final Token token) throws IOException {
        final long startLineNumber = getCurrentLineNumber();
        int c;
        while (true) {
            c = reader.read();

            if (isEscape(c)) {
                final int unescaped = readEscape();
                if (unescaped == END_OF_STREAM) {
                    token.content.append((char) c).append((char) reader.getLastChar());
                } else {
                    token.content.append((char) unescaped);
                }
            } else if (isQuoteChar(c)) {
                if (isQuoteChar(reader.lookAhead())) {
                    c = reader.read();
                    token.content.append((char) c);
                } else {
                    while (true) {
                        c = reader.read();
                        if (isDelimiter(c)) {
                            token.type = Token.Type.TOKEN;
                            return token;
                        } else if (isEndOfFile(c)) {
                            token.type = Token.Type.EOF;
                            token.isReady = true;
                            return token;
                        } else if (readEndOfLine(c)) {
                            token.type = Token.Type.EORECORD;
                            return token;
                        } else if (!isWhitespace(c)) {
                            throw new IOException("(line " + getCurrentLineNumber() + ") invalid char between encapsulated token and delimiter");
                        }
                    }
                }
            } else if (isEndOfFile(c)) {
                throw new IOException("(startline " + startLineNumber + ") EOF reached before encapsulated token finished");
            } else {
                token.content.append((char) c);
            }
        }
    }

    long getCurrentLineNumber() {
        return reader.getCurrentLineNumber();
    }

    int readEscape() throws IOException {
        final int ch = reader.read();
        switch (ch) {
            case 'r':
                return CR;
            case 'n':
                return LF;
            case 't':
                return TAB;
            case 'b':
                return BACKSPACE;
            case 'f':
                return FF;
            case CR:
            case LF:
            case FF:
            case TAB:
            case BACKSPACE:
                return ch;
            case END_OF_STREAM:
                throw new IOException("EOF whilst processing escape sequence");
            default:
                if (isMetaChar(ch)) {
                    return ch;
                }
                return END_OF_STREAM;
        }
    }

    void trimTrailingSpaces(final StringBuilder buffer) {
        int length = buffer.length();
        while (length > 0 && Character.isWhitespace(buffer.charAt(length - 1))) {
            length = length - 1;
        }
        if (length != buffer.length()) {
            buffer.setLength(length);
        }
    }

    boolean readEndOfLine(int ch) throws IOException {
        if (ch == CR && reader.lookAhead() == LF) {
            ch = reader.read();
        }
        return ch == LF || ch == CR;
    }

    boolean isWhitespace(final int ch) {
        return !isDelimiter(ch) && Character.isWhitespace((char) ch);
    }

    boolean isStartOfLine(final int ch) {
        return ch == LF || ch == CR || ch == UNDEFINED;
    }

    boolean isEndOfFile(final int ch) {
        return ch == END_OF_STREAM;
    }

    boolean isDelimiter(final int ch) {
        return ch == delimiter;
    }

    boolean isEscape(final int ch) {
        return ch == escape;
    }

    boolean isQuoteChar(final int ch) {
        return ch == quoteChar;
    }

    boolean isCommentStart(final int ch) {
        return ch == commentStart;
    }

    private boolean isMetaChar(final int ch) {
        return ch == delimiter ||
                ch == escape ||
                ch == quoteChar ||
                ch == commentStart;
    }

    public void close() throws IOException {
        reader.close();
    }
}