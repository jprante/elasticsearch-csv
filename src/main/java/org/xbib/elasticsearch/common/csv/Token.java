package org.xbib.elasticsearch.common.csv;

final class Token {

    private static final int INITIAL_TOKEN_LENGTH = 50;

    enum Type {
        INVALID,
        TOKEN,
        EOF,
        EORECORD,
        COMMENT
    }

    Token.Type type = Type.INVALID;

    StringBuilder content = new StringBuilder(INITIAL_TOKEN_LENGTH);

    boolean isReady;

    void reset() {
        content.setLength(0);
        type = Type.INVALID;
        isReady = false;
    }

}