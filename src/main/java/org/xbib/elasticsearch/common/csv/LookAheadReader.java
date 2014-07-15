package org.xbib.elasticsearch.common.csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

class LookAheadReader extends BufferedReader implements Constants {

    private int lastChar = UNDEFINED;

    private long eolCounter = 0;

    LookAheadReader(Reader reader) {
        super(reader);
    }

    @Override
    public int read() throws IOException {
        final int current = super.read();
        if (current == CR || (current == LF && lastChar != CR)) {
            eolCounter++;
        }
        lastChar = current;
        return lastChar;
    }

    int getLastChar() {
        return lastChar;
    }

    @Override
    public int read(final char[] buf, final int offset, final int length) throws IOException {
        if (length == 0) {
            return 0;
        }
        final int len = super.read(buf, offset, length);
        if (len > 0) {
            for (int i = offset; i < offset + len; i++) {
                final char ch = buf[i];
                if (ch == LF) {
                    if (CR != (i > 0 ? buf[i - 1] : lastChar)) {
                        eolCounter++;
                    }
                } else if (ch == CR) {
                    eolCounter++;
                }
            }
            lastChar = buf[offset + len - 1];
        } else if (len == -1) {
            lastChar = END_OF_STREAM;
        }
        return len;
    }

    @Override
    public String readLine() throws IOException {
        final String line = super.readLine();
        if (line != null) {
            lastChar = LF;
            eolCounter++;
        } else {
            lastChar = END_OF_STREAM;
        }
        return line;
    }

    int lookAhead() throws IOException {
        super.mark(1);
        final int c = super.read();
        super.reset();

        return c;
    }

    long getCurrentLineNumber() {
        if (lastChar == CR || lastChar == LF || lastChar == UNDEFINED || lastChar == END_OF_STREAM) {
            return eolCounter;
        }
        return eolCounter + 1;
    }

    @Override
    public void close() throws IOException {
        lastChar = END_OF_STREAM;
        super.close();
    }

}