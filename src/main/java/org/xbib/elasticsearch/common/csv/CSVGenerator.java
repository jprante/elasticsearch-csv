package org.xbib.elasticsearch.common.csv;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class CSVGenerator implements Closeable, Flushable {

    private final static char comma = ',';

    private final static char quote = '\"';

    private final static char escape = '\"';

    private final static char tab = '\t';

    private final static String lf = System.getProperty("line.separator");

    private Writer writer;

    private int col;

    private String[] keys;

    public CSVGenerator(Writer writer) {
        this.writer = writer;
        this.col = 0;
        this.keys = new String[]{};
    }

    public CSVGenerator keys(List<String> keys) {
        this.keys = keys.toArray(new String[keys.size()]);
        return this;
    }

    public CSVGenerator writeKeys() throws IOException {
        for (String k : keys) {
            write(k);
        }
        return this;
    }

    public CSVGenerator write(String value) throws IOException {
        if (col > 0) {
            writer.write(comma);
        }
        writer.write(escape(value));
        col++;
        if (col >= keys.length) {
            nextRow();
        }
        return this;
    }

    public CSVGenerator nextRow() throws IOException {
        if (col > 0) {
            writer.write(lf);
            col = 0;
        }
        return this;
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.indexOf(quote) < 0 && value.indexOf(escape) < 0
                && value.indexOf(comma) < 0 && value.indexOf(tab) < 0  && !value.contains(lf)) {
           return value;
        }
        int length = value.length();
        StringBuilder sb = new StringBuilder(length + 2);
        sb.append(quote);
        for (int i = 0; i < length; i++) {
            char ch = value.charAt(i);
            if (ch == quote) {
                sb.append(quote);
            }
            sb.append(ch);
        }
        sb.append(quote);
        return sb.toString();
    }

}
