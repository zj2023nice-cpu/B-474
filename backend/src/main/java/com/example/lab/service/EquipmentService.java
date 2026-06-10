package com.example.lab.service;

import com.example.lab.dto.EquipmentDetailDTO;
import com.example.lab.dto.EquipmentQuery;
import com.example.lab.dto.ExpiringEquipmentDTO;
import com.example.lab.dto.ExpiringQuery;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipmentService {
    @Autowired
    private EquipmentRepository equipmentRepository;
    
    @Autowired
    private LabRepository labRepository;

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private RepairRepository repairRepository;

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

    public EquipmentDetailDTO getDetail(Long id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("设备不存在"));

        EquipmentDetailDTO dto = new EquipmentDetailDTO();
        dto.setId(equipment.getId());
        dto.setCode(equipment.getCode());
        dto.setName(equipment.getName());
        dto.setModel(equipment.getModel());
        dto.setManufacturer(equipment.getManufacturer());
        dto.setPurchaseDate(equipment.getPurchaseDate());
        dto.setPrice(equipment.getPrice());
        dto.setLifeSpan(equipment.getLifeSpan());
        dto.setStatus(equipment.getStatus());

        if (equipment.getLab() != null) {
            EquipmentDetailDTO.LabInfo labInfo = new EquipmentDetailDTO.LabInfo();
            Lab lab = equipment.getLab();
            labInfo.setId(lab.getId());
            labInfo.setName(lab.getName());
            labInfo.setBuilding(lab.getBuilding());
            labInfo.setRoom(lab.getRoom());
            labInfo.setPicName(lab.getPicName());
            labInfo.setPicPhone(lab.getPicPhone());
            labInfo.setCapacity(lab.getCapacity());
            dto.setLab(labInfo);
        }

        if (equipment.getPurchaseDate() != null && equipment.getLifeSpan() != null) {
            LocalDate expiryDate = equipment.getPurchaseDate().plusYears(equipment.getLifeSpan());
            dto.setExpiryDate(expiryDate);
            LocalDate today = LocalDate.now();
            long remaining = java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate);
            dto.setRemainingDays(remaining);
        }

        Borrow latestBorrow = borrowRepository.findTopByEquipment_IdOrderByApplyDateDesc(id);
        if (latestBorrow != null) {
            EquipmentDetailDTO.LatestBorrow borrowDTO = new EquipmentDetailDTO.LatestBorrow();
            borrowDTO.setId(latestBorrow.getId());
            borrowDTO.setApplicantName(latestBorrow.getApplicant() != null ? latestBorrow.getApplicant().getName() : null);
            borrowDTO.setApplyDate(latestBorrow.getApplyDate());
            borrowDTO.setStartTime(latestBorrow.getStartTime());
            borrowDTO.setEndTime(latestBorrow.getEndTime());
            borrowDTO.setPurpose(latestBorrow.getPurpose());
            borrowDTO.setStatus(latestBorrow.getStatus());
            dto.setLatestBorrow(borrowDTO);
        }

        Repair latestRepair = repairRepository.findTopByEquipment_IdOrderByReportDateDesc(id);
        if (latestRepair != null) {
            EquipmentDetailDTO.LatestRepair repairDTO = new EquipmentDetailDTO.LatestRepair();
            repairDTO.setId(latestRepair.getId());
            repairDTO.setDescription(latestRepair.getDescription());
            repairDTO.setReportDate(latestRepair.getReportDate());
            repairDTO.setRepairCompany(latestRepair.getRepairCompany());
            repairDTO.setCost(latestRepair.getCost());
            repairDTO.setRepairConclusion(latestRepair.getRepairConclusion());
            repairDTO.setFinishDate(latestRepair.getFinishDate());
            repairDTO.setStatus(latestRepair.getStatus());
            repairDTO.setReporterName(latestRepair.getReporter() != null ? latestRepair.getReporter().getName() : null);
            dto.setLatestRepair(repairDTO);
        }

        return dto;
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

    public Page<ExpiringEquipmentDTO> findExpiring(ExpiringQuery query) {
        LocalDate today = LocalDate.now();

        Specification<Equipment> spec = Specification.where((root, q, cb) ->
                cb.notEqual(root.get("status"), "SCRAPPED"));

        if (query.getLabId() != null) {
            spec = spec.and((root, q, cb) ->
                    cb.equal(root.get("lab").get("id"), query.getLabId()));
        }

        if (StringUtils.hasText(query.getStatus())) {
            spec = spec.and((root, q, cb) ->
                    cb.equal(root.get("status"), query.getStatus()));
        }

        List<Equipment> all = equipmentRepository.findAll(spec);

        List<ExpiringEquipmentDTO> dtos = all.stream().map(e -> {
            ExpiringEquipmentDTO dto = new ExpiringEquipmentDTO();
            dto.setId(e.getId());
            dto.setCode(e.getCode());
            dto.setName(e.getName());
            dto.setModel(e.getModel());
            dto.setManufacturer(e.getManufacturer());
            dto.setPurchaseDate(e.getPurchaseDate());
            dto.setPrice(e.getPrice());
            dto.setLifeSpan(e.getLifeSpan());
            dto.setStatus(e.getStatus());

            if (e.getLab() != null) {
                ExpiringEquipmentDTO.LabInfo labInfo = new ExpiringEquipmentDTO.LabInfo();
                labInfo.setId(e.getLab().getId());
                labInfo.setName(e.getLab().getName());
                dto.setLab(labInfo);
            }

            boolean hasPurchaseDate = e.getPurchaseDate() != null;
            boolean hasLifeSpan = e.getLifeSpan() != null;

            if (hasPurchaseDate && hasLifeSpan) {
                LocalDate expiryDate = e.getPurchaseDate().plusYears(e.getLifeSpan());
                long remaining = ChronoUnit.DAYS.between(today, expiryDate);
                dto.setExpiryDate(expiryDate);
                dto.setRemainingDays(remaining);
                dto.setDataComplete(true);
            } else {
                dto.setDataComplete(false);
                List<String> reasons = new ArrayList<>();
                if (!hasPurchaseDate) reasons.add("缺少采购日期");
                if (!hasLifeSpan) reasons.add("缺少使用年限");
                dto.setDataIncompleteReason(String.join("、", reasons));
            }

            return dto;
        }).collect(Collectors.toList());

        dtos = dtos.stream().filter(dto -> {
            if (Boolean.TRUE.equals(query.getExpiredOnly())) {
                return dto.isDataComplete() && dto.getRemainingDays() < 0;
            }
            if (!dto.isDataComplete()) {
                return query.getIncludeIncomplete();
            }
            return dto.getRemainingDays() <= 30;
        }).collect(Collectors.toList());

        Comparator<ExpiringEquipmentDTO> comparator = createExpiringComparator(query.getSortBy(), query.getSortOrder());
        dtos.sort(comparator);

        int total = dtos.size();
        int start = (query.getPage() - 1) * query.getSize();
        int end = Math.min(start + query.getSize(), total);

        List<ExpiringEquipmentDTO> pageContent;
        if (start >= total) {
            pageContent = new ArrayList<>();
        } else {
            pageContent = dtos.subList(start, end);
        }

        Pageable pageable = PageRequest.of(query.getPage() - 1, query.getSize());
        return new PageImpl<>(pageContent, pageable, total);
    }

    private Comparator<ExpiringEquipmentDTO> createExpiringComparator(String sortBy, String sortOrder) {
        boolean asc = "asc".equalsIgnoreCase(sortOrder);

        Comparator<ExpiringEquipmentDTO> dataCompleteFirst = Comparator.comparing(
                (ExpiringEquipmentDTO dto) -> !dto.isDataComplete());

        Comparator<ExpiringEquipmentDTO> fieldComparator;
        switch (sortBy) {
            case "expiryDate":
                fieldComparator = Comparator.comparing(
                        (ExpiringEquipmentDTO dto) -> dto.getExpiryDate() != null ? dto.getExpiryDate() : LocalDate.MAX,
                        asc ? Comparator.naturalOrder() : Comparator.reverseOrder());
                break;
            case "overdueDegree":
                fieldComparator = Comparator.comparingLong(
                        (ExpiringEquipmentDTO dto) -> {
                            if (dto.getRemainingDays() == null) return 0L;
                            return Math.max(0L, -dto.getRemainingDays());
                        });
                if (!asc) {
                    fieldComparator = fieldComparator.reversed();
                }
                break;
            case "remainingDays":
            default:
                fieldComparator = Comparator.comparingLong(
                        (ExpiringEquipmentDTO dto) -> dto.getRemainingDays() != null ? dto.getRemainingDays() : Long.MAX_VALUE);
                if (!asc) {
                    fieldComparator = fieldComparator.reversed();
                }
                break;
        }

        return dataCompleteFirst.thenComparing(fieldComparator)
                .thenComparing(dto -> dto.getCode() != null ? dto.getCode() : "");
    }
}
