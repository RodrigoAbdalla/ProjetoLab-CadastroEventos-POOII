package com.example.event.repositories;

import com.example.event.entities.Attendee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AttendeeRepository extends JpaRepository <Attendee,Long> {
    @Query("SELECT e FROM Attendee e " + 
           "WHERE " +
           "e.balance  >=   :balance "
    )
    public Page<Attendee> find(Pageable pageRequest, Double balance);
    
    
}
