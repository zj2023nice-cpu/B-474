package com.example.lab.service;

import com.example.lab.dto.EquipmentQuery;
import com.example.lab.entity.Equipment;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipmentService {
    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private LabRepository labRepository;

    @Transactional
    public Equipment addEquipment(Equipment equipment) {
        Lab lab = labRepository.findById(equipment.getLab().getId()).orElseThrow(() -> new RuntimeException("Lab not found"));
        equipment.setLab(lab);
        
        long count = equipmentRepository.findByLab_Id(lab.getId()).size();
        String code = String.format("LAB%02d-%03d", lab.getId(), count + 1);
        equipment.setCode(code);
        equipment.setStatus("NORMAL");
        
        return equipmentRepository.save(equipment);
    }

    public Equipment updateEquipment(Equipment equipment) {
        return equipmentRepository.save(equipment);
    }

    public void deleteEquipment(Long id) {
        equipmentRepository.deleteById(id);
    }

    public List<Equipment> findAll() {
        return equipmentRepository.findAll();
    }
    
    public Page<Equipment> findAll(EquipmentQuery query) {
        Specification<Equipment> spec = createSpecification(query);
        Pageable pageable = createPageable(query);
        return equipmentRepository.findAll(spec, pageable);
    }
    
    private Specification<Equipment> createSpecification(EquipmentQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            Specification<Equipment> spec = Specification.where(null);
            
            if (StringUtils.hasText(query.getName())) {
                spec = spec.and((r, q, cb) -> 
                    cb.like(cb.lower(r.get("name")), "%" + query.getName().toLowerCase() + "%"));
            }
            
            if (StringUtils.hasText(query.getCode())) {
                spec = spec.and((r, q, cb) -> 
                    cb.like(cb.lower(r.get("code")), "%" + query.getCode().toLowerCase() + "%"));
            }
            
            if (StringUtils.hasText(query.getStatus())) {
                spec = spec.and((r, q, cb) -> 
                    cb.equal(r.get("status"), query.getStatus()));
            }
            
            if (query.getLabId() != null) {
                spec = spec.and((r, q, cb) -> 
                    cb.equal(r.get("lab").get("id"), query.getLabId()));
            }
            
            return spec.toPredicate(root, criteriaQuery, criteriaBuilder);
        };
    }
    
    private Pageable createPageable(EquipmentQuery query) {
        Sort.Direction direction = "asc".equalsIgnoreCase(query.getSortOrder()) 
            ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sort = Sort.by(direction, query.getSortBy());
        return PageRequest.of(query.getPage() - 1, query.getSize(), sort);
    }
    
    public Equipment findById(Long id) {
        return equipmentRepository.findById(id).orElse(null);
    }
    
    public List<Equipment> findExpiringIn30Days() {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(30);
        
        List<Equipment> activeEquipments = equipmentRepository.findByStatusNot("SCRAPPED");
        
        return activeEquipments.stream()
            .filter(e -> {
                if (e.getPurchaseDate() == null || e.getLifeSpan() == null) {
                    return false;
                }
                LocalDate expiryDate = e.getPurchaseDate().plusYears(e.getLifeSpan());
                return !expiryDate.isBefore(today) && !expiryDate.isAfter(futureDate);
            })
            .sorted((e1, e2) -> {
                LocalDate expiry1 = e1.getPurchaseDate().plusYears(e1.getLifeSpan());
                LocalDate expiry2 = e2.getPurchaseDate().plusYears(e2.getLifeSpan());
                return expiry1.compareTo(expiry2);
            })
            .collect(Collectors.toList());
    }
}
