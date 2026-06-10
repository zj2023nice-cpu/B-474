package com.example.lab.service;

import com.example.lab.dto.LabDetailDTO;
import com.example.lab.dto.LabQuery;
import com.example.lab.entity.Borrow;
import com.example.lab.entity.Equipment;
import com.example.lab.entity.Lab;
import com.example.lab.entity.Repair;
import com.example.lab.repository.BorrowRepository;
import com.example.lab.repository.EquipmentRepository;
import com.example.lab.repository.LabRepository;
import com.example.lab.repository.RepairRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LabService {
    @Autowired
    private LabRepository labRepository;

    @Autowired
    private EquipmentRepository equipmentRepository;

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private RepairRepository repairRepository;

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

        List<Equipment> labEquipments = equipmentRepository.findByLab_Id(id);

        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(30);
        List<LabDetailDTO.EquipmentSummary> expiringList = labEquipments.stream()
                .filter(e -> !"SCRAPPED".equals(e.getStatus()))
                .filter(e -> e.getPurchaseDate() != null && e.getLifeSpan() != null)
                .filter(e -> {
                    LocalDate expiry = e.getPurchaseDate().plusYears(e.getLifeSpan());
                    return !expiry.isBefore(today) && !expiry.isAfter(futureDate);
                })
                .sorted(Comparator.comparing(e -> e.getPurchaseDate().plusYears(e.getLifeSpan())))
                .map(e -> {
                    LabDetailDTO.EquipmentSummary s = new LabDetailDTO.EquipmentSummary();
                    s.setId(e.getId());
                    s.setCode(e.getCode());
                    s.setName(e.getName());
                    LocalDate expiry = e.getPurchaseDate().plusYears(e.getLifeSpan());
                    s.setExpiryDate(expiry);
                    s.setRemainingDays(ChronoUnit.DAYS.between(today, expiry));
                    return s;
                })
                .collect(Collectors.toList());
        dto.setExpiringEquipments(expiringList);
        dto.setExpiringCount(expiringList.size());

        List<Borrow> activeBorrows = borrowRepository.findByEquipment_Lab_IdAndStatusIn(id, Arrays.asList("APPROVED"));
        dto.setActiveBorrowCount(activeBorrows.size());
        dto.setActiveBorrows(activeBorrows.stream()
                .sorted(Comparator.comparing(Borrow::getApplyDate).reversed())
                .limit(5)
                .map(b -> {
                    LabDetailDTO.BorrowSummary bs = new LabDetailDTO.BorrowSummary();
                    bs.setId(b.getId());
                    bs.setEquipmentName(b.getEquipment() != null ? b.getEquipment().getName() : null);
                    bs.setEquipmentCode(b.getEquipment() != null ? b.getEquipment().getCode() : null);
                    bs.setApplicantName(b.getApplicant() != null ? b.getApplicant().getName() : null);
                    bs.setStartTime(b.getStartTime());
                    bs.setEndTime(b.getEndTime());
                    bs.setPurpose(b.getPurpose());
                    return bs;
                })
                .collect(Collectors.toList()));

        List<Repair> activeRepairs = repairRepository.findByEquipment_Lab_IdAndStatusNot(id, "FINISHED");
        dto.setActiveRepairCount(activeRepairs.size());
        dto.setActiveRepairs(activeRepairs.stream()
                .sorted(Comparator.comparing(Repair::getReportDate).reversed())
                .limit(5)
                .map(r -> {
                    LabDetailDTO.RepairSummary rs = new LabDetailDTO.RepairSummary();
                    rs.setId(r.getId());
                    rs.setEquipmentName(r.getEquipment() != null ? r.getEquipment().getName() : null);
                    rs.setEquipmentCode(r.getEquipment() != null ? r.getEquipment().getCode() : null);
                    rs.setDescription(r.getDescription());
                    rs.setReportDate(r.getReportDate());
                    rs.setStatus(r.getStatus());
                    return rs;
                })
                .collect(Collectors.toList()));

        return dto;
    }

    public void delete(Long id) {
        labRepository.deleteById(id);
    }
}
