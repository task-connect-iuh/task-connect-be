package vn.taskconnect.map.dto.response;

/**
 * Mot goi y trong dropdown autocomplete dia chi, dung cho GET /api/v1/map/autocomplete.
 * BINH THUONG (nguon VietMap) KHONG kem toa do - frontend goi tiep GET /api/v1/map/place?refId=
 * khi nguoi dung THAT SU chon 1 goi y (xem Javadoc VietMapClient.autocomplete). Khi VietMap
 * khong kha dung va MapService fallback sang Photon (xem PhotonClient.search()), lat/lng
 * duoc dien san ngay o day vi Photon tra toa do truc tiep trong ket qua tim kiem - frontend
 * nhan dien truong hop nay qua lat/lng khac null va BO QUA buoc goi /map/place.
 */
public record AddressSuggestionResponse(String refId, String label, String addressText, String operatingArea,
        Double lat, Double lng) {

    /** Goi y tu VietMap - luon can goi them Place API de lay toa do that su. */
    public AddressSuggestionResponse(String refId, String label, String addressText, String operatingArea) {
        this(refId, label, addressText, operatingArea, null, null);
    }
}
