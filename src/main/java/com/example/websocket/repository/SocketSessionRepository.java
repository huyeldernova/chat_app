package com.example.websocket.repository;

import com.example.websocket.entity.SocketSession;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SocketSessionRepository extends CrudRepository<SocketSession, String> {


    List<SocketSession> findByUserId(String userId);

    void deleteBySocketSessionId(String socketSessionId);

}

