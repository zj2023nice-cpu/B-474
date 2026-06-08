package com.example.lab.service;

import com.example.lab.dto.LabQuery;
import com.example.lab.entity.Lab;
import com.example.lab.repository.LabRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class LabService {
    @Autowired
    private LabRepository labRepository;

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

    public void delete(Long id) {
        labRepository.deleteById(id);
    }
}
