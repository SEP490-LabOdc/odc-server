package com.odc.notificationservice.service;

import com.odc.notificationservice.dto.response.GetNotificationResponse;

import java.util.List;

public interface FcmService {
    void sendToDevices(GetNotificationResponse notification, List<String> deviceTokens);
}