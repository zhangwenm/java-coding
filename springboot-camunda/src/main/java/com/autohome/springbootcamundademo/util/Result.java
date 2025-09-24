package com.autohome.springbootcamundademo.util;

import lombok.Data;

@Data
public class Result<T> {
    private int returncode;
    private T result;
    private String message;
}
