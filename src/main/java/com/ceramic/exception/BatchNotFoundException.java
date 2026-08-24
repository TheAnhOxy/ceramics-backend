package com.ceramic.exception;

public class BatchNotFoundException extends RuntimeException {
    public BatchNotFoundException(Long batchId) {
        super("Không tìm thấy mẻ gốm có ID: " + batchId);
    }
}
