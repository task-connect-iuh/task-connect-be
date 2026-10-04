package vn.taskconnect.ai.infrastructure;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import vn.taskconnect.ai.api.dto.CategoryClassificationRequest;
import vn.taskconnect.ai.api.dto.ClarifyingAnswerInput;
import vn.taskconnect.ai.api.dto.RefineDescriptionRequest;
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

    /**
     * Dung prompt tieng Viet gui kem anh cho Gemini Vision de sinh tieu de + mo ta cong viec,
     * phan loai category goi y, VA (khi mo ta CHUA DU CU THE) sinh toi da 3 cau hoi lam ro cho
     * Poster - tat ca trong CUNG mot lan goi (xem Javadoc ImageTaskSuggestionResult).
     *
     * <p>QUAN TRONG (chot voi nguoi dung 2026-09-29, sua lai sau khi test that): cau hoi lam ro
     * la HAI dieu kien DOC LAP voi phan loai category, khong phai mot - giong bac si hoi benh
     * nhan them du trieu chung ro (dieu kien DU) du da biet benh nhan den vi khoa nao (dieu kien
     * CAN). "category.outcome=CATEGORY" (da khop ro nhom dich vu) KHONG co nghia mo ta da du
     * chi tiet de Tasker bao gia - vd anh chup dong ho nuoc co the de dang xac dinh category
     * "Cap thoat nuoc", nhung mo ta van co the qua chung ("co su co lien quan den dong ho nuoc")
     * khong noi ro su co GI. Hai truong hop can phan biet ro:
     * - Category ro + mo ta ro rang mot su co cu the (vd "ri nuoc o chan voi") -> KHONG can hoi
     *   gi them, clarifyingQuestions RONG, du outcome co la CATEGORY hay OTHER.
     * - Category ro HAY khong ro, nhung mo ta CHUNG CHUNG/khong neu duoc mot su co cu the (chi
     *   mo ta hinh dang thiet bi chung) -> VAN can hoi them, bat ke category co khop hay khong.
     * Ly do can hoi them: anh chi chup duoc TRIEU CHUNG be mat (vd mat dong ho, vet uot), khong
     * chup duoc NGUYEN NHAN/dien bien an sau (vd dong ho chay nhanh bat thuong, ong ngam vo) -
     * chi nguoi tai hien truong (Poster) moi biet, AI khong the "doan gioi hon anh cho phep".
     * KHONG yeu cau phat hien SUSPICIOUS o day - kiem duyet noi dung van la trach nhiem cua
     * buildCategoryClassificationPrompt() chay lai tren mo ta cuoi cung luc submit.
     */
    public String buildImageTaskSuggestionPrompt(List<CategoryClassificationRequest.CandidateCategory> candidates) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ban la tro ly dang viec cua TaskConnect, nen tang ket noi Tasker (tho sua dien ")
                .append("nuoc) voi Task Poster (nguoi thue). Nhiem vu: nhin buc anh dinh kem, mo ta ")
                .append("tinh trang/su co dang thay trong anh, roi goi y tieu de + mo ta chi tiet bang ")
                .append("TIENG VIET de Poster dung dang cong viec, va phan loai vao 1 trong 2 truong ")
                .append("hop lien quan den danh sach danh muc ung vien ben duoi.\n\n")
                .append("QUY TAC BAT BUOC:\n")
                .append("1. CHI mo ta nhung gi THAT SU quan sat duoc trong anh (vd loai thiet bi, dau ")
                .append("hieu hu hong ro rang nhu ri nuoc/chay den/nut vo). KHONG bia them chi tiet ")
                .append("khong the nhin thay trong anh (vd nguyen nhan, thoi gian hu, thuong hieu neu ")
                .append("khong doc duoc chu tren anh).\n")
                .append("2. VAN PHONG: viet title/description theo DUNG giong dieu cua chinh Poster ")
                .append("(nguoi can thue tho) dang tu ke lai van de nha minh gap - KHONG viet nhu bao ")
                .append("cao kiem tra ky thuat/nhan vien quan sat hien truong. Cam cac cum tu may moc ")
                .append("kieu 'Quan sat thay...', 'Ghi nhan tinh trang...', 'Can tho den kiem tra tinh ")
                .append("trang hoat dong hoac xu ly su co lien quan den...'.\n")
                .append("VI DU KHONG duoc viet (qua may moc, nghe nhu robot/bao cao): 'Quan sat thay mat ")
                .append("dong ho do nuoc mau xanh hien thi cac con so va kim chi. Can tho den kiem tra ")
                .append("tinh trang hoat dong hoac xu ly su co lien quan den dong ho nuoc.'\n")
                .append("NEN viet the nay (tu nhien, giong nguoi thuc su dang can sua): 'Dong ho nuoc nha ")
                .append("minh co ve chay khong binh thuong, kim nhay lien tuc du nha khong dung nuoc may. ")
                .append("Minh can tho kiem tra lai xem dong ho co bi loi hay ro ri cho nao khong.'\n")
                .append("3. Neu anh khong the hien ro mot su co/cong viec can lam (vd anh mo, khong ")
                .append("lien quan dien-nuoc), van tra ve title/description mo ta trung thuc nhung gi ")
                .append("thay duoc, KHONG tu suy dien mot su co khong co that.\n")
                .append("4. Kiem tra DANH SACH DANH MUC UNG VIEN (DOC LAP voi buoc 5 ve cau hoi lam ro ")
                .append("ben duoi - hai buoc nay KHONG phu thuoc nhau). Neu anh khop ro mot danh muc, ")
                .append("tra ve outcome=\"CATEGORY\", candidateId=id danh muc do (dung id trong danh ")
                .append("sach, khong bia them), confidence=do tin cay 0-100. Neu KHONG khop ro danh ")
                .append("muc nao, tra ve outcome=\"OTHER\", candidateId=null, confidence=0.\n")
                .append("5. CAU HOI LAM RO (clarifyingQuestions): danh gia RIENG xem description vua ")
                .append("viet o buoc 2 co NEU RO duoc MOT su co/trieu chung cu the chua (vd 'ri nuoc o ")
                .append("chan voi', 'kim dong ho chay lien tuc du khong dung nuoc', 'o cam bi chay den') ")
                .append("hay van con CHUNG CHUNG (vd chi mo ta hinh dang/vi tri thiet bi ma KHONG neu ")
                .append("duoc su co gi dang xay ra). Neu description DA neu ro mot su co cu the, tra ve ")
                .append("clarifyingQuestions=mang RONG [] - KHONG can hoi gi them, BAT KE outcome o ")
                .append("buoc 4 la CATEGORY hay OTHER. Neu description CON CHUNG CHUNG, sinh toi da 3 ")
                .append("cau hoi lam ro trong clarifyingQuestions - moi cau hoi ve MOT chi tiet cu the ")
                .append("chi nguoi dang o hien truong moi quan sat/biet duoc (vd am thanh nghe duoc, ")
                .append("thoi diem bat dau, dien bien theo thoi gian, hoa don dien/nuoc thay doi), ")
                .append("KHONG hoi lai nhung gi anh da the hien ro, KHONG hoi chung chung kieu 'ban co ")
                .append("the mo ta them khong'.\n")
                .append("6. Moi phan tu trong clarifyingQuestions la object {\"key\": string ngan 2-5 tu ")
                .append("tom tat chu de cau hoi, PHAI co day du dau tieng Viet giong het nhu \"text\" ")
                .append("(vd \"Tiếng nước chảy\", KHONG duoc viet \"Tieng nuoc chay\" khong dau), \"text\": ")
                .append("cau hoi day du bang tieng Viet tu nhien lich su, co dau day du (vd \"Khi khoa het ")
                .append("voi roi, ban co con nghe tieng nuoc chay duoi san khong?\" phai viet thanh \"Khi ")
                .append("khoá hết vòi rồi, bạn có còn nghe tiếng nước chảy dưới sàn không?\"), \"placeholder\": ")
                .append("vi du cau tra loi ngan co dau day du de goi y cach tra loi (vd \"Vd: Có, ban đêm ")
                .append("nghe rõ lắm\")}. TOAN BO ba truong key/text/placeholder BAT BUOC co dau tieng ")
                .append("Viet day du, khong duoc viet khong dau o bat ky truong nao.\n")
                .append("7. Tra ve DUY NHAT mot object JSON, khong kem giai thich, khong kem markdown ")
                .append("code fence, khong kem van ban nao khac ngoai JSON. Dinh dang bat buoc: ")
                .append("{\"title\": string, \"description\": string, \"category\": {\"outcome\": ")
                .append("string, \"candidateId\": string hoac null, \"confidence\": number, ")
                .append("\"suspiciousReason\": null}, \"clarifyingQuestions\": [{\"key\": string, ")
                .append("\"text\": string, \"placeholder\": string}, ...]}. suspiciousReason LUON LUON ")
                .append("null o day.\n\n")
                .append("DANH SACH DANH MUC UNG VIEN:\n");
        for (int i = 0; i < candidates.size(); i++) {
            CategoryClassificationRequest.CandidateCategory candidate = candidates.get(i);
            sb.append(i + 1).append(". id=\"").append(candidate.candidateId()).append("\", ten=\"")
                    .append(candidate.name()).append("\", mo ta=\"").append(candidate.contextText()).append("\"\n");
        }
        return sb.toString();
    }

    /**
     * Dung prompt tieng Viet gui cho Groq de gop mo ta goc + cac cau tra loi lam ro (xem
     * ClarifyingAnswerInput) thanh MOT doan mo ta tu nhien, chuyen nghiep de Tasker hieu dung
     * cong viec can lam - thay vi Poster tu doc cau tra loi tho ghep tho "{key}: {answer}".
     * Tra ve PLAIN TEXT (khong phai JSON) vi ket qua chi la mot chuoi mo ta duy nhat.
     */
    public String buildRefineDescriptionPrompt(RefineDescriptionRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Ban la tro ly dang viec cua TaskConnect, nen tang ket noi Tasker (tho sua dien ")
                .append("nuoc) voi Task Poster (nguoi thue). Poster da viet mo ta ban dau, roi tra loi ")
                .append("them mot vai cau hoi lam ro chi tiet. Nhiem vu: viet lai thanh MOT doan mo ta ")
                .append("hoan chinh, mach lac bang TIENG VIET co dau day du, gop ca mo ta ban dau va cac ")
                .append("chi tiet moi tra loi vao lam mot, de Tasker doc hieu ngay tinh trang/su co can ")
                .append("xu ly.\n\n")
                .append("QUY TAC BAT BUOC:\n")
                .append("1. CHI duoc dung thong tin co trong mo ta ban dau va cac cau tra loi duoi day. ")
                .append("CAM bia them chi tiet, nguyen nhan, hay so lieu khong co trong input. Cau tra ")
                .append("loi nao de trong hoac khong ro nghia thi BO QUA, khong dua vao mo ta.\n")
                .append("2. VAN PHONG: viet theo giong dieu Poster (nguoi can thue tho) tu ke lai van ")
                .append("de nha minh gap, TU NHIEN nhu nguoi that dang mo ta - KHONG viet nhu bao cao ")
                .append("ky thuat. Duoc phep sap xep lai cau chu cho mach lac hon, SUA chinh ta/dau cau ")
                .append("tieng Viet neu Poster go thieu dau hoac viet tat, nhung KHONG doi nghia cau ")
                .append("tra loi.\n")
                .append("3. Ket qua la MOT doan van xuoi lien mach (khong gach dau dau, khong danh so, ")
                .append("khong nhan lai cau hoi), do dai vua phai (khoang 2-5 cau).\n")
                .append("4. CHI tra ve DUY NHAT doan mo ta dang plain text, KHONG kem giai thich, KHONG ")
                .append("kem markdown, KHONG kem dau ngoac kep bao quanh ca doan.\n\n")
                .append("MO TA BAN DAU:\n")
                .append(request.originalDescription() == null || request.originalDescription().isBlank()
                        ? "(Poster chua viet gi)" : request.originalDescription())
                .append("\n\nCAU HOI LAM RO VA TRA LOI CUA POSTER:\n");
        List<ClarifyingAnswerInput> answers = request.answers();
        for (int i = 0; i < answers.size(); i++) {
            ClarifyingAnswerInput answer = answers.get(i);
            sb.append(i + 1).append(". Hoi: ").append(answer.questionText())
                    .append(" | Tra loi: ").append(answer.answer()).append("\n");
        }
        sb.append("\nHay viet lai thanh mot doan mo ta hoan chinh theo dung quy tac da neu.");
        return sb.toString();
    }
}
