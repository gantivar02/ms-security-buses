package com.AJJ.ms_security.Repositories;

import com.AJJ.ms_security.Models.Session;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.Date;
import java.util.List;

public interface SessionRepository extends MongoRepository<Session, String> {

    // HU-012: busca sesión activa (sin JWT aún) por usuario
    @Query("{'user.$id': ObjectId(?0), 'token': null}")
    Session findActiveSessionByUserId(String userId);

    // HU-012: busca sesión por código 2FA para verificarlo
    @Query("{'_id': ObjectId(?0), 'code2FA': ?1}")
    Session findByIdAndCode2FA(String sessionId, String code2FA);

    // HU-012: sesiones parciales expiradas (sin JWT y con fecha pasada) para limpieza programada
    @Query("{'token': null, 'expiration': {$lt: ?0}}")
    List<Session> findExpiredPartialSessions(Date now);
}
