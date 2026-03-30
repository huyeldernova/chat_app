package com.example.websocket.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    INTERNAL_SERVER_ERROR(500, "Unknow Exception", HttpStatus.INTERNAL_SERVER_ERROR),

    ACCESS_DENIED(403, "Access Denied", HttpStatus.FORBIDDEN),

    TOKEN_GENERATION_FAILED(500, "Failed to generate JWT token", HttpStatus.INTERNAL_SERVER_ERROR),
    TOKEN_EXPIRED(401, "Token expired", HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID(401, "Token invalid", HttpStatus.UNAUTHORIZED),

    USER_EXISTED(400, "User already existed", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(404, "User not found", HttpStatus.NOT_FOUND),

    CONVERSATION_NOT_FOUND(404, "Conversation not found", HttpStatus.NOT_FOUND),
    PRIVATE_CONVERSATION_MAX_TWO_PARTICIPANTS(400, "A private conversation can only have up to two participants", HttpStatus.BAD_REQUEST),
    CONVERSATION_NAME_REQUIRED(400, "Conversation name is required", HttpStatus.BAD_REQUEST),
    GROUP_CONVERSATION_MINIMUM_THREE_PARTICIPANTS(400, "A group conversation must have at least three participants", HttpStatus.BAD_REQUEST),


    FILE_EMPTY(400, "File is empty", HttpStatus.BAD_REQUEST),
    FILE_TOO_LARGE(400, "File size exceeds maximum allowed size", HttpStatus.BAD_REQUEST),
    INVALID_FILE_TYPE(400, "File type not supported. Only image and video allowed", HttpStatus.BAD_REQUEST),
    FILE_UPLOAD_FAILED(500, "Failed to upload file to S3", HttpStatus.INTERNAL_SERVER_ERROR),
    FILE_NOT_FOUND(404, "File not found", HttpStatus.NOT_FOUND);


    private final int code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
