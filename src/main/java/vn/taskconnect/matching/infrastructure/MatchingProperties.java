package vn.taskconnect.matching.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Trong so xep hang va nguong cau hinh cua module Matching, doc tu prefix {@code matching.*}
 * trong application.yml. Tat ca la thong so co the chinh sau khi co du lieu that (xem plan
 * da duyet "Chua validate lai toan bo threshold so bang du lieu that"), khong hardcode trong
 * Java - dung nguyen tac giong CryptoProperties.
 *
 * @param weights trong so cua tung tieu chi cau truc + ty trong semantic khi tron finalScore
 * @param lowConfidenceThreshold nguong % duoi do FE gan nhan "Do khop thap" (khong an bot,
 *                                chi gan nhan - quyet dinh da chot voi nguoi dung)
 */
@ConfigurationProperties(prefix = "matching")
public record MatchingProperties(Weights weights, int lowConfidenceThreshold) {

    /**
     * @param distance trong so tieu chi khoang cach trong structuredScore
     * @param price trong so tieu chi khop gia trong structuredScore
     * @param experience trong so tieu chi kinh nghiem trong structuredScore
     * @param availability trong so tieu chi khop lich ranh trong structuredScore
     *                      (4 trong so tren cong lai = 1.0, cau thanh structuredScore)
     * @param semantic ty trong semanticScore khi tron vao finalScore: finalScore =
     *                 (1 - semantic) * structuredScore + semantic * semanticScore
     */
    public record Weights(double distance, double price, double experience, double availability, double semantic) {
    }
}
