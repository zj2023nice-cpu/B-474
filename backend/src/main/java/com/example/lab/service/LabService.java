package com.example.lab.service;

import com.example.lab.dto.LabDetailDTO;
import com.example.lab.dto.LabQuery;
import com.example.lab.entity.Lab;
import com.example.lab.repository.EquipmentRepository;
import com.example.lab.repository.LabRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LabService {
    @Autowired
    private LabRepository labRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    private static final String[] EQUIPMENT_STATUSES = {"NORMAL", "BORROWED", "REPAIRING", "SCRAPPED"};

    public Lab save(Lab lab) {
        return labRepository.save(lab);
    }

    public List<Lab> findAll() {
        return labRepository.findAll();
    }
    
    public Page<Lab> findAll(LabQuery query) {
        Specification<Lab> spec = createSpecification(query);
        Pageable pageable = createPageable(query);
        return labRepository.findAll(spec, pageable);
    }
    
    private Specification<Lab> createSpecification(LabQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            Specification<Lab> spec = Specification.where(null);
            
            if (StringUtils.hasText(query.getName())) {
                spec = spec.and((r, q, cb) -> 
                    cb.like(cb.lower(r.get("name")), "%" + query.getName().toLowerCase() + "%"));
            }
            
            if (StringUtils.hasText(query.getBuilding())) {
                spec = spec.and((r, q, cb) -> 
                    cb.like(cb.lower(r.get("building")), "%" + query.getBuilding().toLowerCase() + "%"));
            }
            
            if (StringUtils.hasText(query.getPicName())) {
                spec = spec.and((r, q, cb) -> 
                    cb.like(cb.lower(r.get("picName")), "%" + query.getPicName().toLowerCase() + "%"));
            }
            
            return spec.toPredicate(root, criteriaQuery, criteriaBuilder);
        };
    }
    
    private Pageable createPageable(LabQuery query) {
        Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortOrder()) 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, query.getSortBy());
        return PageRequest.of(query.getPage() - 1, query.getSize(), sort);
    }

    public LabDetailDTO getDetail(Long id) {
        Lab lab = labRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("实验室不存在"));

        LabDetailDTO dto = new LabDetailDTO();
        dto.setId(lab.getId());
        dto.setName(lab.getName());
        dto.setBuilding(lab.getBuilding());
        dto.setRoom(lab.getRoom());
        dto.setPicName(lab.getPicName());
        dto.setPicPhone(lab.getPicPhone());
        dto.setCapacity(lab.getCapacity());

        long total = equipmentRepository.countByLab_Id(id);
        dto.setTotalEquipment(total);

        Map<String, Long> statusCounts = new LinkedHashMap<>();
        for (String status : EQUIPMENT_STATUSES) {
            statusCounts.put(status, equipmentRepository.countByLab_IdAndStatus(id, status));
        }
        dto.setStatusCounts(statusCounts);

        return dto;
    }

    public void delete(Long id) {
        labRepository.deleteById(id);
    }
}
