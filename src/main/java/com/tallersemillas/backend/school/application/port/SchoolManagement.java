package com.tallersemillas.backend.school.application.port;

import java.util.*;
public interface SchoolManagement {
 List<Map<String,Object>> students(String status,String search);
 Map<String,Object> student(long id);
 Map<String,Object> saveStudent(Long id,Map<String,Object> data);
 void retireStudent(long id);
 List<Map<String,Object>> guardians(long student);
 Map<String,Object> saveGuardian(Long id,Map<String,Object> data);
 void removeGuardian(long id,long student);
 Map<String,Object> history(long student,boolean create);
 List<Map<String,Object>> medicalEntries(long history);
 Map<String,Object> saveMedicalEntry(Long id,Map<String,Object> data);
 void removeMedicalEntry(long id);
 Map<String,Object> schema(String type);
 Map<String,Object> publishSchema(String type,List<Map<String,Object>> fields);
 List<Map<String,Object>> slots(boolean availableOnly);
 Map<String,Object> slot(long id);
 Map<String,Object> saveSlot(Long id,Map<String,Object> data);
 void removeSlot(long id);
 Map<String,Object> bookInterview(Map<String,Object> data);
 List<Map<String,Object>> interviews();
 Map<String,Object> attendance(long id,boolean attended);
 Map<String,Object> enroll(Map<String,Object> data);
 Map<String,Object> inscriptions(Map<String,String> filters);
 Map<String,Object> decide(long id,boolean approve);
}
