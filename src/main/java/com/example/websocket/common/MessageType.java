package com.example.websocket.common;

public enum MessageType {
    TEXT,       // Tin nhắn văn bản
    IMAGE,      // Hình ảnh
    VIDEO,      // Video
    FILE,       // File đính kèm
    SYSTEM      // Tin nhắn hệ thống (thêm/xóa thành viên, đổi tên group, etc.)
}
