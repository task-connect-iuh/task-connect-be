package vn.taskconnect.task.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Nguong nghiep vu cho tinh nang "Goi y muc gia" luc dang viec (dua tren gia da chot cua cac
 * task tuong tu trong qua khu, xem TaskPriceSuggestionService), doc tu prefix
 * {@code task.price-suggestion} trong application.yml - khong hardcode trong Java, cung
 * nguyen tac voi MatchingProperties.
 *
 * @param similarityThreshold nguong cosine similarity toi thieu (0-1) de mot task qua khu duoc
 *                              tinh la "tuong tu" - da dieu chinh tu 0.75 xuong 0.70 sau 1 lan
 *                              test that voi Gemini (2026-09-29, xem comment application.yml),
 *                              van la gia tri tham khao, chua du data de coi la chuan
 * @param minSamples so luong task tuong tu toi thieu (vuot nguong similarity) can co de dua ra
 *                     goi y - khong du thi tra ve khong co goi y (giu nguyen "thoa thuan"),
 *                     tranh goi y tu 1-2 mau khong dai dien
 * @param topK so luong task tuong tu nhat (trong nhung task da vuot nguong) dung de tinh median
 *             gia goi y - gioi han de khong de vai task cu lech pha loang ket qua
 */
@ConfigurationProperties(prefix = "task.price-suggestion")
public record TaskPriceSuggestionProperties(double similarityThreshold, int minSamples, int topK) {
}
