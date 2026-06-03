package com.opencode.agents.domain.vo;

import com.opencode.agents.common.ErrorCode;
import lombok.Data;

@Data
public class ResultVO<T> {

    private int code;
    private String message;
    private T data;

    public static <T> ResultVO<T> success(T data) {
        ResultVO<T> vo = new ResultVO<>();
        vo.code = 0;
        vo.message = "ok";
        vo.data = data;
        return vo;
    }

    public static <T> ResultVO<T> success() {
        return success(null);
    }

    public static <T> ResultVO<T> error(int code, String message) {
        ResultVO<T> vo = new ResultVO<>();
        vo.code = code;
        vo.message = message;
        return vo;
    }

    public static <T> ResultVO<T> error(ErrorCode errorCode) {
        return error(errorCode.getCode(), errorCode.getMessage());
    }

}
