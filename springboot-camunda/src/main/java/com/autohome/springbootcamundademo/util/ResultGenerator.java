package com.autohome.springbootcamundademo.util;

public class ResultGenerator {
    private static final String DEFAULT_SUCCESS_MESSAGE = "";

    public static Result genSuccessResult() {
        Result result = new Result();
        result.setReturncode(ResultCode.SUCCESS.code());
        result.setMessage(DEFAULT_SUCCESS_MESSAGE);
        return result;
    }

    public static Result genSuccessResult(Object data) {
        Result result = new Result();
        result.setReturncode(ResultCode.SUCCESS.code());
        result.setResult(data);
        result.setMessage(DEFAULT_SUCCESS_MESSAGE);
        return result;
    }

    public static Result genFailResult(String message) {
        Result result = new Result();
        result.setReturncode(ResultCode.FAIL.code());
        result.setMessage(message);
        return result;
    }

    public static Result genFailResult(ResultCode resultCode, String message) {
        Result result = new Result();
        result.setReturncode(resultCode.code());
        result.setMessage("");
        return result;
    }
}
enum ResultCode {
    SUCCESS(0),
    FAIL(-1);

    private final int code;

    ResultCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
