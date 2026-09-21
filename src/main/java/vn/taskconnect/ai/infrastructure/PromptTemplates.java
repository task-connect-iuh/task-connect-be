package vn.taskconnect.ai.infrastructure;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import vn.taskconnect.ai.api.dto.CategoryClassificationRequest;
import vn.taskconnect.ai.api.dto.SuggestionReasonRequest;

/**
 * Dung prompt tieng Viet gui cho Groq de sinh ly do/diem tru cho tung ung vien trong
 * SuggestionReasonRequest. Yeu cau STRICT JSON, cam bia so lieu ngoai input - giu ranking
 * deterministic (Java da tinh xong thu tu o TaskerMatchingService), LLM chi lam nhiem vu
 * "dien giai" bang ngon ngu tu nhien tu chinh cac con so da co, khong tu cham diem lai.
 */
@Component
public class PromptTemplates {

    /**
     * Dung toan bo noi dung prompt gui cho Groq: huong dan vai tro, dinh dang JSON bat buoc,
     * mot vi du minh hoa ngan de neo dung cau truc, roi den boi canh cong viec va danh sach
     * ung vien that su can danh gia.
     */
    public String buildSuggestionReasonPrompt(SuggestionReasonRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ban la tro ly ghep viec cua TaskConnect, nen tang ket noi Tasker (tho sua ")
                .append("dien nuoc) voi Task Poster (nguoi thue). Nhiem vu: voi MOI ung vien duoc ")
                .append("liet ke ben duoi, hay viet ly do vi sao AI goi y nguoi nay (diem hop) va ")
                .append("diem chua hop/rui ro (neu co), bang TIENG VIET, ngan gon, de hieu voi ")
                .append("nguoi dung pho thong.\n\n")
                .append("QUY TAC BAT BUOC:\n")
                .append("1. CHI duoc dung dung so lieu/du kien da cho trong phan facts cua tung ung ")
                .append("vien. CAM bia them so lieu, ten rieng, hay chi tiet khong co trong input.\n")
                .append("2. Tra ve DUY NHAT mot mang JSON (JSON array), khong kem giai thich, khong ")
                .append("kem markdown code fence, khong kem van ban nao khac ngoai JSON.\n")
                .append("3. Moi phan tu trong mang la mot object dung dinh dang: ")
                .append("{\"candidateId\": string, \"confidence\": number tu 0 den 100, ")
                .append("\"reasons\": [string, ...], \"concerns\": [string, ...]}.\n")
                .append("4. reasons la cac diem HOP (uu diem) - vd lich ranh khop, gia phu hop, gan. ")
                .append("concerns la cac diem CHUA HOP hoac rui ro - vd cach xa, chua co kinh nghiem, ")
                .append("moi xac minh. concerns duoc phep la mang rong [] neu thuc su khong co diem ")
                .append("tru dang ke - KHONG bia diem tru gia tao chi de co noi dung.\n")
                .append("5. Phai tra ve DU cho TAT CA ung vien duoc liet ke, dung thu tu candidateId ")
                .append("nhu input, khong duoc bo sot.\n")
                .append("6. VIET TU NHIEN, KHONG lap lai dung mot khuon cau cho moi ung vien (vd cam ")
                .append("viet may moc kieu \"Khoang cach X km, xa\" cho tat ca) - doi tu ngu, cach dien ")
                .append("dat giua cac ung vien khac nhau nhu nguoi that dang viet, dung cung mot cum tu ")
                .append("co dinh lap lai nhieu lan trong cung phan hoi. Neu mot ung vien co 0 nam kinh ")
                .append("nghiem, KHONG ghi may moc \"0 nam kinh nghiem\" - dien dat te nhi hon, vd \"moi ")
                .append("vao nghe\", \"chua co nhieu kinh nghiem thuc te\".\n\n")
                .append("VI DU MINH HOA (chi de tham khao cau truc VA do da dang van phong can co giua ")
                .append("cac ung vien, khong lien quan du lieu that, KHONG duoc chep lai nguyen van cho ")
                .append("du lieu that ben duoi):\n")
                .append("Input mau: candidateId=\"c1\", facts={\"distance\": \"1.2 km\", ")
                .append("\"priceFit\": \"trong khoang de xuat\", \"availability\": \"khop Thu 7 sang\"}; ")
                .append("candidateId=\"c2\", facts={\"distance\": \"17.6 km\", ")
                .append("\"priceFit\": \"cao hon ngan sach ban de xuat\", \"availability\": \"khong ranh dung thoi gian ban chon\"}\n")
                .append("Output mau: [{\"candidateId\": \"c1\", \"confidence\": 85, ")
                .append("\"reasons\": [\"Ở ngay gần bạn, chỉ 1.2 km\", \"Mức giá đúng với ngân sách bạn đưa ra\", ")
                .append("\"Đúng khung Thứ 7 sáng bạn chọn\"], \"concerns\": []}, ")
                .append("{\"candidateId\": \"c2\", \"confidence\": 42, \"reasons\": [\"Đã xác minh kỹ năng phù hợp\"], ")
                .append("\"concerns\": [\"Khá xa so với khu vực bạn cần, khoảng 17.6 km\", ")
                .append("\"Giá đề xuất nhỉnh hơn ngân sách bạn đưa ra\", \"Chưa rõ có rảnh đúng thời điểm bạn cần không\"]}]\n\n")
                .append("BOI CANH CONG VIEC CAN GOI Y TASKER:\n")
                .append(request.contextDescription()).append("\n\n")
                .append("DANH SACH UNG VIEN CAN DANH GIA:\n");
        List<SuggestionReasonRequest.CandidateFacts> candidates = request.candidates();
        for (int i = 0; i < candidates.size(); i++) {
            SuggestionReasonRequest.CandidateFacts candidate = candidates.get(i);
            sb.append(i + 1).append(". candidateId=\"").append(candidate.candidateId()).append("\", facts={");
            boolean first = true;
            for (Map.Entry<String, String> entry : candidate.facts().entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append("\"").append(entry.getKey()).append("\": \"").append(entry.getValue()).append("\"");
                first = false;
            }
            sb.append("}\n");
        }
        sb.append("\nHay tra ve JSON array dung dinh dang da mo ta o tren, du cho tat ca ")
                .append(candidates.size()).append(" ung vien.");
        return sb.toString();
    }

    /**
     * Dung prompt tieng Viet gui cho Groq de phan loai mo ta cong viec vao 1 trong cac danh
     * muc ung vien, hoac xac dinh OTHER (khong khop danh muc nao) / SUSPICIOUS (khop mot tieu
     * chi vi pham). candidates.contextText la kho tri thuc RAG duy nhat (lay tu
     * user_service_categories.description/keywords - xem .claude/rules/15-ai-module.md), cam
     * LLM tu bia them danh muc ngoai danh sach candidates.
     */
    public String buildCategoryClassificationPrompt(CategoryClassificationRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ban la tro ly phan loai cong viec cua TaskConnect, nen tang ket noi Tasker ")
                .append("(tho sua dien nuoc) voi Task Poster (nguoi thue). Nhiem vu: doc mo ta ")
                .append("cong viec ben duoi, roi phan loai vao DUNG MOT trong ba truong hop sau.\n\n")
                .append("QUY TAC BAT BUOC:\n")
                .append("1. Kiem tra DANH SACH TIEU CHI VI PHAM TRUOC TIEN. Neu mo ta khop bat ky ")
                .append("tieu chi nao trong do, tra ve outcome=\"SUSPICIOUS\", candidateId=null, ")
                .append("confidence=0, suspiciousReason=ma cua tieu chi do (dung dung ma trong ")
                .append("danh sach, khong bia them ma moi) - du cho co ve mo ta cung khop mot danh ")
                .append("muc, tieu chi vi pham luon uu tien hon.\n")
                .append("2. Neu khong khop tieu chi vi pham nao, kiem tra DANH SACH DANH MUC UNG ")
                .append("VIEN. Neu khop ro mot danh muc, tra ve outcome=\"CATEGORY\", candidateId=id ")
                .append("danh muc do (dung id trong danh sach, khong bia them), confidence=do tin ")
                .append("cay 0-100, suspiciousReason=null.\n")
                .append("3. Neu khong khop tieu chi vi pham nao va cung khong khop ro danh muc nao ")
                .append("(vd viec ngoai pham vi dien-nuoc dan dung nhung khong co dau hieu vi pham), ")
                .append("tra ve outcome=\"OTHER\", candidateId=null, confidence=0, ")
                .append("suspiciousReason=null.\n")
                .append("4. Tra ve DUY NHAT mot object JSON, khong kem giai thich, khong kem markdown ")
                .append("code fence, khong kem van ban nao khac ngoai JSON. Dinh dang bat buoc: ")
                .append("{\"outcome\": string, \"candidateId\": string hoac null, \"confidence\": ")
                .append("number, \"suspiciousReason\": string hoac null}.\n\n")
                .append("DANH SACH TIEU CHI VI PHAM (SUSPICIOUS):\n");
        List<CategoryClassificationRequest.SuspiciousCriterion> criteria = request.suspiciousCriteria();
        for (int i = 0; i < criteria.size(); i++) {
            CategoryClassificationRequest.SuspiciousCriterion criterion = criteria.get(i);
            sb.append(i + 1).append(". ma=\"").append(criterion.code()).append("\", mo ta=\"")
                    .append(criterion.description()).append("\"\n");
        }
        sb.append("\nDANH SACH DANH MUC UNG VIEN:\n");
        List<CategoryClassificationRequest.CandidateCategory> candidates = request.candidates();
        for (int i = 0; i < candidates.size(); i++) {
            CategoryClassificationRequest.CandidateCategory candidate = candidates.get(i);
            sb.append(i + 1).append(". id=\"").append(candidate.candidateId()).append("\", ten=\"")
                    .append(candidate.name()).append("\", mo ta=\"").append(candidate.contextText()).append("\"\n");
        }
        sb.append("\nMO TA CONG VIEC CAN PHAN LOAI:\n").append(request.description());
        return sb.toString();
    }
}
