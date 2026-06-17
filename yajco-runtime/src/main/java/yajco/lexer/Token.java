package yajco.lexer;

/**
 * A matched token.
 */
public class Token {
    static public class Range<T> {
        private final T start;
        private final T end;

        public Range(T start, T end) {
            this.start = start;
            this.end = end;
        }

        public T getStart() {
            return this.start;
        }

        public T getEnd() {
            return this.end;
        }
    }

    static public class Position {
        private final int index;
        private final int line;
        private final int column;

        public Position(int index, int line, int column) {
            this.index = index;
            this.line = line;
            this.column = column;
        }

        public int getIndex() {
            return this.index;
        }

        public int getLine() {
            return this.line;
        }

        public int getColumn() {
            return this.column;
        }
    }

    static public int EOF = -1;

    private final int type;
    private final String text;
    private final Range<Position> range;
    private final Range<Position> groupRange;

    public Token(int type, String text, Range<Position> range, Range<Position> groupRange) {
        this.type = type;
        this.text = text;
        this.range = range;
        this.groupRange = groupRange;
    }

    public boolean isEOF() {
        return this.type == EOF;
    }

    public int getType() {
        return this.type;
    }

    public String getText() {
        return this.text;
    }

    /**
     * Returns the match range [start, end] of the entire pattern.
     * @return The match range of the entire pattern.
     */
    public Range<Position> getRange() {
        return this.range;
    }

    /**
     * Returns the match range [start, end] of the first group of the pattern.
     * @return The match range of the first group of the pattern.
     */
    public Range<Position> getGroupRange() {
        return this.groupRange;
    }
}
