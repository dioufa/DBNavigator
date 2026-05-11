package com.db.navigator.service;

import com.db.navigator.exception.DuplicateResourceException;
import com.db.navigator.exception.ResourceNotFoundException;
import com.db.navigator.model.dto.StationDTO;
import com.db.navigator.model.entity.Station;
import com.db.navigator.repository.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StationService {

    private final StationRepository stationRepository;

    public List<StationDTO> findAll() {
        return stationRepository.findAll().stream()
                .map(this::toDTO)
                .toList();
    }

    public StationDTO findById(Long id) {
        return stationRepository.findById(id)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Station nicht gefunden."));
    }

    public StationDTO findByCode(String code) {
        return stationRepository.findByCode(code)
                .map(this::toDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Station nicht gefunden."));
    }

    public List<StationDTO> searchByName(String search) {
        return stationRepository.searchByName(search).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public StationDTO create(StationDTO dto) {
        if (stationRepository.existsByCode(dto.getCode())) {
            throw new DuplicateResourceException("Station mit Code " + dto.getCode() + "existiert bereit.");
        }

        Station station = toEntity(dto);
        assert station != null;
        Station saved = stationRepository.save(station);
        return toDTO(saved);
    }

    @Transactional
    public StationDTO update(Long id, StationDTO dto) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Station nicht gefunden mit id = " + id));
        station.setCode(dto.getCode());
        station.setName(dto.getName());
        station.setCity(dto.getCity());

        Station updated = stationRepository.save(station);
        return toDTO(updated);
    }

    @Transactional
    public void delete(Long id) {
        Station station = stationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Station nicht gefunden mit id = " + id));
        stationRepository.deleteById(id);
    }

    private StationDTO toDTO(Station station) {
        return StationDTO.builder()
                .id(station.getId())
                .code(station.getCode())
                .name(station.getName())
                .city(station.getCity())
                .build();
    }

    private Station toEntity(StationDTO dto) {
        return Station.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .city(dto.getCity())
                .build();
    }
}
