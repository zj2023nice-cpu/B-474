package com.example.lab.service;

import com.example.lab.entity.Equipment;
import com.example.lab.entity.Lab;
import com.example.lab.enums.EquipmentStatus;
import com.example.lab.repository.EquipmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 设备持久化辅助类。
 * 唯一作用：把"单次保存"放到独立事务里，使 EquipmentService 的重试循环可以正常工作。
 *
 * <p>为什么不直接写在 EquipmentService 内部？
 * Spring AOP 基于代理，<b>同类内部方法调用不会走代理</b>，
 * 内部方法上的 @Transactional(REQUIRES_NEW) 注解不生效。
 * 拆到独立组件后，EquipmentService 通过 bean 调用 helper bean，代理链完整。</p>
 */
@Component
public class EquipmentPersistenceHelper {

    @Autowired
    private EquipmentRepository equipmentRepository;

    /**
     * 在新事务中插入设备记录。
     * 一旦抛 DataIntegrityViolationException，本事务独立回滚，不影响外部调用方的重试逻辑。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public Equipment saveInNewTransaction(Equipment source, Lab lab, String code) {
        Equipment toSave = new Equipment();
        toSave.setName(source.getName());
        toSave.setModel(source.getModel());
        toSave.setManufacturer(source.getManufacturer());
        toSave.setPurchaseDate(source.getPurchaseDate());
        toSave.setPrice(source.getPrice());
        toSave.setLifeSpan(source.getLifeSpan());
        toSave.setLab(lab);
        toSave.setCode(code);
        toSave.setStatus(EquipmentStatus.NORMAL);
        return equipmentRepository.save(toSave);
    }
}
