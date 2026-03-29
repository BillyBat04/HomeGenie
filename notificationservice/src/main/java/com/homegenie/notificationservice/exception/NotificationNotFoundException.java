package com.homegenie.notificationservice.exception;

public class NotificationNotFoundException extends RuntimeException {

    public NotificationNotFoundException(Long id) {
        super("Notification not found: " + id);
    }

    public NotificationNotFoundException(String message) {
        super(message);
    }
}
