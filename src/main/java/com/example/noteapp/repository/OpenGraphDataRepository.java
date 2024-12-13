package com.example.noteapp.repository;

import com.example.noteapp.model.OpenGraphData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OpenGraphDataRepository extends JpaRepository<OpenGraphData, UUID> {
}
