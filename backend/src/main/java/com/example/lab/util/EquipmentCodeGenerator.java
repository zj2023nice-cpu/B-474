package com.example.lab.util;

import com.example.lab.entity.Equipment;
import com.example.lab.repository.EquipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 设备编号生成器。
 *
 * <h3>生成策略</h3>
 * <p>格式：LAB{labId}-{serial}，其中：</p>
 * <ul>
 *   <li>labId：实验室 ID，最少 2 位，不足补前导零；超过 2 位按实际位数输出。</li>
 *   <li>serial：流水号，最少 3 位，不足补前导零；超过 3 位按实际位数输出。</li>
 * </ul>
 *
 * <h3>流水号取法</h3>
 * <p>同一实验室下，解析所有已有设备编号的 serial 部分，取 <b>最大值 + 1</b>。
 * 不是 count + 1，所以删除历史设备不会导致编号复用。</p>
 *
 * <h3>脏数据容错</h3>
 * <p>老数据可能混入格式异常的编号（如手工录入、导入错误等）。
 * 解析时使用正则 {@code ^LAB(\d+)-(\d+)$} 严格匹配，不满足以下任一条件即跳过：</p>
 * <ul>
 *   <li>格式不匹配正则；</li>
 *   <li>前缀中的 labId 与当前实验室 ID 不一致（跨实验室遗留数据）；</li>
 *   <li>serial 解析失败（数值溢出等）。</li>
 * </ul>
 * <p>被跳过的异常编号 <b>不参与</b> 最大值计算，仅记录 warn 日志。</p>
 *
 * <h3>并发 & 唯一约束</h3>
 * <p>本组件只负责"选号"，不保号。并发下仍可能两个线程算出同一个 serial。
 * 真正的一致性保证由两层完成：</p>
 * <ol>
 *   <li>数据库层：{@code equipments.code} 列有唯一约束；</li>
 *   <li>Service 层：捕获 {@code DataIntegrityViolationException} 后
 *       重新调用 {@link #generateNextCode(Long)} 并重试保存，
 *       最多重试 {@link #MAX_RETRY} 次。</li>
 * </ol>
 */
@Component
public class EquipmentCodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(EquipmentCodeGenerator.class);

    public static final int MAX_RETRY = 5;

    private static final Pattern CODE_PATTERN = Pattern.compile("^LAB(\\d+)-(\\d+)$");

    private static final int MIN_LAB_ID_WIDTH = 2;
    private static final int MIN_SERIAL_WIDTH = 3;

    @Autowired
    private EquipmentRepository equipmentRepository;

    /**
     * 为指定实验室生成下一个设备编号。
     *
     * @param labId 实验室 ID（必须为正整数）
     * @return 新编号，如 LAB01-001、LAB100-0099、LAB01-10000
     * @throws IllegalArgumentException labId 为 null 或非正数
     */
    public String generateNextCode(Long labId) {
        validateLabId(labId);

        long maxSerial = findMaxSerial(labId);
        long nextSerial = maxSerial + 1;

        String code = formatCode(labId, nextSerial);
        logger.info("生成设备编号: labId={}, maxSerial={}, nextSerial={}, code={}",
                labId, maxSerial, nextSerial, code);
        return code;
    }

    /**
     * 查询指定实验室下已使用的最大流水号。
     * <p>无有效编号时返回 0，这样第一个生成的 serial 就是 1。</p>
     *
     * @param labId 实验室 ID
     * @return 最大有效流水号，≥ 0
     */
    public long findMaxSerial(Long labId) {
        validateLabId(labId);

        List<Equipment> equipments = equipmentRepository.findByLab_Id(labId);
        if (equipments == null || equipments.isEmpty()) {
            return 0L;
        }

        String expectedPrefix = "LAB" + labId + "-";
        long maxSerial = 0L;
        int skipped = 0;

        for (Equipment eq : equipments) {
            String code = eq.getCode();
            if (code == null || code.isEmpty()) {
                skipped++;
                continue;
            }

            Matcher matcher = CODE_PATTERN.matcher(code);
            if (!matcher.matches()) {
                skipped++;
                logger.warn("跳过格式异常的设备编号: id={}, code={}", eq.getId(), code);
                continue;
            }

            long parsedLabId;
            long serial;
            try {
                parsedLabId = Long.parseLong(matcher.group(1));
                serial = Long.parseLong(matcher.group(2));
            } catch (NumberFormatException ex) {
                skipped++;
                logger.warn("跳过编号数值解析失败的设备: id={}, code={}, cause={}",
                        eq.getId(), code, ex.getMessage());
                continue;
            }

            if (parsedLabId != labId) {
                skipped++;
                logger.warn("跳过后缀与实验室不匹配的设备编号: id={}, code={}, 期望前缀={}",
                        eq.getId(), code, expectedPrefix);
                continue;
            }

            if (serial > maxSerial) {
                maxSerial = serial;
            }
        }

        if (skipped > 0) {
            logger.warn("实验室 {} 共跳过 {} 个异常编号（不影响最大流水号计算）", labId, skipped);
        }

        return maxSerial;
    }

    /**
     * 按"动态位数"格式化编号。
     * <p>
     * 实验室 ID 至少 2 位，流水号至少 3 位；超出时按实际位数输出，不截断。
     * 这样既能对齐老数据 LAB01-001 风格，也能自然扩展到 LAB100-1000。
     * </p>
     *
     * @param labId  实验室 ID
     * @param serial 流水号（≥ 1）
     * @return 格式化后的编号
     */
    public String formatCode(Long labId, long serial) {
        String labPart = padLeft(String.valueOf(labId), MIN_LAB_ID_WIDTH, '0');
        String serialPart = padLeft(String.valueOf(serial), MIN_SERIAL_WIDTH, '0');
        return "LAB" + labPart + "-" + serialPart;
    }

    /**
     * 尝试从给定编号中反推流水号。
     * <p>主要给测试 / 排障用，返回 null 表示格式不对或不属于该实验室。</p>
     *
     * @param code  编号
     * @param labId 期望的实验室 ID，null 表示不校验前缀
     * @return 流水号，或 null
     */
    public Long parseSerial(String code, Long labId) {
        if (code == null) return null;
        Matcher m = CODE_PATTERN.matcher(code);
        if (!m.matches()) return null;
        try {
            long parsedLabId = Long.parseLong(m.group(1));
            long serial = Long.parseLong(m.group(2));
            if (labId != null && parsedLabId != labId) return null;
            return serial;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private void validateLabId(Long labId) {
        if (labId == null || labId <= 0) {
            throw new IllegalArgumentException("labId 必须为正整数，实际: " + labId);
        }
    }

    private static String padLeft(String s, int minLen, char pad) {
        if (s.length() >= minLen) return s;
        StringBuilder sb = new StringBuilder(minLen);
        for (int i = s.length(); i < minLen; i++) sb.append(pad);
        sb.append(s);
        return sb.toString();
    }
}
