package jt.skunkworks.dataflow.util;

public class DuplicateMessageException extends Exception {
    public DuplicateMessageException() {
        super("Duplicate Message :: message rejected");
    }
}
