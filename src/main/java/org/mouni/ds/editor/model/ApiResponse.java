package org.mouni.ds.editor.model;

public class ApiResponse<T> {
    private int code;
    private String status;
    private T payload;

    public ApiResponse() {}

    public ApiResponse(int code, String status, T payload) {
        this.code = code;
        this.status = status;
        this.payload = payload;
    }

    public static <T> ApiResponse<T> ok(T payload) {
        return new ApiResponse<>(200, "OK", payload);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public String getStatus() {
        return status;
    }

    public T getPayload() {
        return payload;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setPayload(T payload) {
        this.payload = payload;
    }
}
