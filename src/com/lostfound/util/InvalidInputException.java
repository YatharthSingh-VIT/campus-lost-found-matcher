package com.lostfound.util;
public class InvalidInputException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;
    public InvalidInputException(String message) { super(message); }
}
