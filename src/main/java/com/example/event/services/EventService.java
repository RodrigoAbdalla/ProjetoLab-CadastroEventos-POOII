package com.example.event.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import javax.persistence.EntityNotFoundException;

import com.example.event.dto.EventDTO;
import com.example.event.dto.EventInsertDTO;
import com.example.event.dto.EventUpdateDTO;
import com.example.event.entities.Event;
import com.example.event.entities.Place;
import com.example.event.repositories.AdminRepository;
import com.example.event.repositories.EventRepository;
import com.example.event.repositories.PlaceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EventService {

    @Autowired
    private PlaceRepository placeRepository;

    @Autowired
    private EventRepository repo;


    @Autowired
    private AdminRepository adminRepository;
    public Page<EventDTO> getEvents(PageRequest pageRequest, String name, String description, String  startDateString, Double  priceTicket) {



                                                                             // FORMATO DE DATA ACEITO = yyyy-mm-dd  || yyyy/mm/dd  || yyyy.mm.dd 
        if(startDateString.contains("/")){                                  // Logica para trocar os caracteres incorretos, caso nao seja "-"
            startDateString = startDateString.replace("/", "-");
        }
        else if(startDateString.contains(".")){
            startDateString = startDateString.replace(".", "-");
        }
        
        try{                                                                        // Mapeando o erro para caso o usuario tente colocar uma data nom formato errado
            LocalDate startDate = LocalDate.parse(startDateString.trim());          // TRANSFORMA A STRING RECEBIDA EM UMA VARIAVEL LOCAL DATE 
            Page<Event> list = repo.find(pageRequest, name, description, startDate, priceTicket);     
            return list.map( e -> new EventDTO(e));
        }
        catch(DateTimeParseException e){
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Error trying to Convert to DataType. Please note that Data Format is yyyy/MM/dd");
        }
         
        

        
    }


    public EventDTO getEventById(Long id) {
        Optional<Event> op = repo.findById(id);
        Event event = op.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        return new EventDTO(event);
    }

    public EventDTO insert(EventInsertDTO insertDTO) {
        if( 
            insertDTO.getName()         == ""    ||                 // Logica para o programa nao aceitar nomes, descrições e nem lugares vazios / nulos
            insertDTO.getDescription()  == ""    || 
            insertDTO.getName()         == null  || 
            insertDTO.getDescription()  == null  ||
            insertDTO.getStartDate()    == null  ||
            insertDTO.getEndDate()      == null  ||
            insertDTO.getStartTime()    == null  ||
            insertDTO.getEndTime()      == null  ||
            insertDTO.getEmailContact() == null ||
            insertDTO.getAmountFreeTickets()     == null  ||
            insertDTO.getAmountPayedTickets()     == null  ||
            insertDTO.getPriceTicket()     == null  ||
            insertDTO.getAdmin()     == null
        ){
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Please fill in all the required fields");
        }
        else if(
            insertDTO.getStartDate().isAfter(insertDTO.getEndDate()) ||         // Logica para nao aceitar que a data final seja maior que a data inicial
                ((insertDTO.getStartDate().isEqual(insertDTO.getEndDate())) 
                && 
                (insertDTO.getStartTime().isAfter(insertDTO.getEndTime())))
            ){
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "An event cannot start after the end");
        }  
        try {
            Event entity = new Event(insertDTO);
            // Coleta o numero do ADMIN que está guardado no inserDTO e procura no repository
            entity.setAdmin(adminRepository.findById(insertDTO.getAdmin()).get());
            entity = repo.save(entity);
            return new EventDTO(entity);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin not found");
        }
        
    }

    public void delete(Long id) {
        try {
            repo.deleteById(id);
        } 
        catch (EmptyResultDataAccessException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
        }
        // Tratamento para caso o evento já possua um lugar cadastrado, 
        // neste caso, só é possivel excluindo todas as associações primeiro, com o DELETE /events/{id}/places/{id} (Disponível apenas na AF)
        catch(DataIntegrityViolationException e){
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "This Event has a Place. To remove an event, you need to first delete the associated places.");

        }
    }

    public EventDTO update(Long id, EventUpdateDTO updateDTO) {
        try {
            
            if( 
            updateDTO.getName()         == ""    ||                 // Logica para o programa nao aceitar nomes, descrições e nem lugares vazios / nulos
            updateDTO.getDescription()  == ""    || 
            updateDTO.getName()         == null  || 
            updateDTO.getDescription()  == null  ||
            updateDTO.getStartDate()    == null  ||
            updateDTO.getEndDate()      == null  ||
            updateDTO.getStartTime()    == null  ||
            updateDTO.getEndTime()      == null  ||
            updateDTO.getPriceTicket()     == null
            ){
                throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "Please fill in all the required fields");
            }
            else if(
                updateDTO.getStartDate().isAfter(updateDTO.getEndDate()) ||         // Logica para nao aceitar que a data final seja maior que a data inicial
                ((updateDTO.getStartDate().isEqual(updateDTO.getEndDate())) 
                && 
                (updateDTO.getStartTime().isAfter(updateDTO.getEndTime())))
            ){
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "An event cannot start after the end");
            }
            // Logica para nao aceitar mudanças de data do evento para antes da data atual.
            else if(updateDTO.getStartDate().isBefore(LocalDateTime.now().toLocalDate())){
                throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "You cannot change the start date to before today");
            }

            Event entity = repo.getOne(id);
            if(entity.getEndDate().isBefore(LocalDateTime.now().toLocalDate()) || (entity.getEndDate().isEqual(LocalDateTime.now().toLocalDate()) && entity.getEndTime().isBefore(LocalDateTime.now().toLocalTime()))){
                throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "The event is over, update is not avaiable");
            }
            
        
            
            entity.setName(updateDTO.getName());
            entity.setDescription(updateDTO.getDescription());
            entity.setStartDate(updateDTO.getStartDate());
            entity.setEndDate(updateDTO.getEndDate());
            entity.setStartTime(updateDTO.getStartTime());
            entity.setEndTime(updateDTO.getEndTime());
            entity.setPriceTicket(updateDTO.getPriceTicket());
            entity = repo.save(entity);
            return new EventDTO(entity);
        } 
        catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found");
            
        }
    }


    public EventDTO addPlaceToEvent(Long idEvent, Long idPlace) {

        // Verificação se existe o Event com o ID solicitado
        Optional<Event> opEvent = repo.findById(idEvent);
        Event event = opEvent.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        // Verificação se existe o Place com o ID solicitado
        Optional<Place> opPlace = placeRepository.findById(idPlace);
        Place place = opPlace.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Place not found"));

        // Verificação da disponibilidade das datas 
        List <Event> listEvents = new ArrayList<>();
        listEvents = place.getEvents();
        for (Event e : listEvents) {
            // Verificação para não permitir que o mesmo evento seja adicionado ao mesmo lugar
            if(e.getId() == event.getId()){
                throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "You have already link this place to this event");
            }
            // Verificação para não permitir que dois eventos que possuem horários simultâneos aconteçam  no mesmo lugar. 
            if (

                (event.getStartDate().isAfter(e.getStartDate()) & event.getStartDate().isBefore(e.getEndDate()) & event.getStartTime().isAfter(e.getStartTime()) & event.getStartTime().isBefore(e.getEndTime())) ||
                (event.getStartDate().isAfter(e.getStartDate()) & event.getStartDate().isBefore(e.getEndDate()) & event.getEndTime().isAfter(e.getStartTime()) & event.getEndTime().isBefore(e.getEndTime())) ||
                (event.getStartDate().isAfter(e.getStartDate()) & event.getStartDate().isBefore(e.getEndDate()) & event.getStartTime().isBefore(e.getStartTime()) & event.getEndTime().isAfter(e.getEndTime()))  ||
                (event.getStartDate().isAfter(e.getStartDate()) & event.getStartDate().isBefore(e.getEndDate()) & event.getStartTime().compareTo(e.getStartTime())== 0)  ||
                (event.getStartDate().isAfter(e.getStartDate()) & event.getStartDate().isBefore(e.getEndDate()) & event.getEndTime().compareTo(e.getEndTime())== 0)  ||

                (event.getEndDate().isAfter(e.getStartDate()) & event.getEndDate().isBefore(e.getEndDate()) & event.getStartTime().isAfter(e.getStartTime()) & event.getStartTime().isBefore(e.getEndTime())) ||
                (event.getEndDate().isAfter(e.getStartDate()) & event.getEndDate().isBefore(e.getEndDate()) & event.getEndTime().isAfter(e.getStartTime()) & event.getEndTime().isBefore(e.getEndTime())) ||
                (event.getEndDate().isAfter(e.getStartDate()) & event.getEndDate().isBefore(e.getEndDate()) & event.getStartTime().isBefore(e.getStartTime()) & event.getEndTime().isAfter(e.getEndTime())) ||
                (event.getEndDate().isAfter(e.getStartDate()) & event.getEndDate().isBefore(e.getEndDate()) & event.getStartTime().compareTo(e.getStartTime())== 0)  ||
                (event.getEndDate().isAfter(e.getStartDate()) & event.getEndDate().isBefore(e.getEndDate()) & event.getEndTime().compareTo(e.getEndTime())== 0)  ||

                (event.getEndDate().isEqual(e.getStartDate())  & event.getStartTime().isAfter(e.getStartTime()) & event.getStartTime().isBefore(e.getEndTime())) ||
                (event.getEndDate().isEqual(e.getStartDate())  & event.getEndTime().isAfter(e.getStartTime()) & event.getEndTime().isBefore(e.getEndTime())) ||
                (event.getEndDate().isEqual(e.getStartDate())  & event.getStartTime().isBefore(e.getStartTime()) & event.getEndTime().isAfter(e.getEndTime())) ||
                (event.getEndDate().isEqual(e.getStartDate())  & event.getStartTime().compareTo(e.getStartTime())== 0)  ||
                (event.getEndDate().isEqual(e.getStartDate())  & event.getEndTime().compareTo(e.getEndTime())== 0)  ||

                (event.getStartDate().isEqual(e.getStartDate())  & event.getStartTime().isAfter(e.getStartTime()) & event.getStartTime().isBefore(e.getEndTime())) ||
                (event.getStartDate().isEqual(e.getStartDate())  & event.getEndTime().isAfter(e.getStartTime()) & event.getEndTime().isBefore(e.getEndTime())) ||
                (event.getStartDate().isEqual(e.getStartDate())  & event.getStartTime().isBefore(e.getStartTime()) & event.getEndTime().isAfter(e.getEndTime())) ||
                (event.getStartDate().isEqual(e.getStartDate())  & event.getStartTime().compareTo(e.getStartTime())== 0)  ||
                (event.getStartDate().isEqual(e.getStartDate())  & event.getEndTime().compareTo(e.getEndTime())== 0)  ||

                (event.getEndDate().isEqual(e.getEndDate())  & event.getStartTime().isAfter(e.getStartTime()) & event.getStartTime().isBefore(e.getEndTime())) ||
                (event.getEndDate().isEqual(e.getEndDate())  & event.getEndTime().isAfter(e.getStartTime()) & event.getEndTime().isBefore(e.getEndTime())) ||
                (event.getEndDate().isEqual(e.getEndDate())  & event.getStartTime().isBefore(e.getStartTime()) & event.getEndTime().isAfter(e.getEndTime())) ||
                (event.getEndDate().isEqual(e.getEndDate())  & event.getStartTime().compareTo(e.getStartTime())== 0)  ||
                (event.getEndDate().isEqual(e.getEndDate())  & event.getEndTime().compareTo(e.getEndTime())== 0)  ||

                (event.getStartDate().isEqual(e.getEndDate())  & event.getStartTime().isAfter(e.getStartTime()) & event.getStartTime().isBefore(e.getEndTime())) ||
                (event.getStartDate().isEqual(e.getEndDate())  & event.getEndTime().isAfter(e.getStartTime()) & event.getEndTime().isBefore(e.getEndTime())) ||
                (event.getStartDate().isEqual(e.getEndDate())  & event.getStartTime().isBefore(e.getStartTime()) & event.getEndTime().isAfter(e.getEndTime())) ||
                (event.getStartDate().isEqual(e.getEndDate())  & event.getStartTime().compareTo(e.getStartTime())== 0)  ||
                (event.getStartDate().isEqual(e.getEndDate())  & event.getEndTime().compareTo(e.getEndTime())== 0)  
            ){
                throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "An Event will happen in the same place at the same time");
            }
        }
        event.addPlace(place);
        repo.save(event);
        return null;
    }


    public void removeLinkPlaceEvent(Long idEvent, Long idPlace) {
        // Verificação se existe o Event com o ID solicitado
        Optional<Event> opEvent = repo.findById(idEvent);
        Event event = opEvent.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));

        // Verificação se existe o Place com o ID solicitado
        Optional<Place> opPlace = placeRepository.findById(idPlace);
        Place place = opPlace.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Place not found"));

        // Verificação para garantir que existe uma conexão com o Evento e Lugar solicitados
        place = event.getPlaceById(idPlace);
        if (place == null)
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "This event has no link with this place.");
        
        
        // Verificação para não permitir que um lugar seja excluído do evento se o evento já tiver começado.
        if((event.getStartDate().isBefore(LocalDate.now()) || event.getStartDate().isEqual(LocalDate.now())) & (event.getEndDate().isAfter(LocalDate.now()) || (event.getEndDate().isEqual(LocalDate.now()) & event.getEndTime().compareTo(LocalTime.now()) != -1 )))
            throw new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "You can`t change de place of a event that is already happening");

        // Remove a conexão do evento e lugar
        event.removePlace(place);
        repo.save(event);
    }

}
