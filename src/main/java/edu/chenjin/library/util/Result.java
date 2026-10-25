package edu.chenjin.library.util;

/**
 * 统一的 JSON 响应结构。
 */
public class Result {

    private int code;
    private String message;
    private Object data;

    public Result() {
    }

    public Result(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static Result ok() {
        return new Result(200, "操作成功", null);
    }

    public static Result ok(Object data) {
        return new Result(200, "操作成功", data);
    }

    public static Result ok(String message, Object data) {
        return new Result(200, message, data);
    }

    public static Result fail(String message) {
        return new Result(400, message, null);
    }

    public static Result fail(int code, String message) {
        return new Result(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}
