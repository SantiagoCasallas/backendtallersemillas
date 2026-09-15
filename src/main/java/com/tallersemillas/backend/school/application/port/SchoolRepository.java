package com.tallersemillas.backend.school.application.port;

import com.tallersemillas.backend.school.domain.RegistryKind;
import java.util.*;
/** Extensible field values are stored as JSON; identifiers and relationships are relational. */
public interface SchoolRepository {
 Map<String,Object> get(RegistryKind kind,long id);
 List<Map<String,Object>> list(RegistryKind kind);
 Map<String,Object> insert(RegistryKind kind,Map<String,Object> fields);
 Map<String,Object> update(RegistryKind kind,long id,Map<String,Object> fields);
 void delete(RegistryKind kind,long id);
 void lock(RegistryKind kind,long id);
 void lockSchema(String type);
}
