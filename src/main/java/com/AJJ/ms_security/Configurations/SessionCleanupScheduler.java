package com.AJJ.ms_security.Configurations;

import com.AJJ.ms_security.Models.Session;
import com.AJJ.ms_security.Repositories.SessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

// HU-012: elimina sesiones 2FA parciales que expiraron sin ser completadas ni canceladas
@Component
public class SessionCleanupScheduler {

    @Autowired
    private SessionRepository sessionRepository;

    @Scheduled(fixedRate = 5 * 60 * 1000) // cada 5 minutos
    public void cleanExpiredPartialSessions() {
        List<Session> expired = sessionRepository.findExpiredPartialSessions(new Date());
        if (!expired.isEmpty()) {
            sessionRepository.deleteAll(expired);
        }
    }
}
