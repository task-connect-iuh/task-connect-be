package vn.taskconnect.matching.service;

import java.math.BigDecimal;

/**
 * Tien ich tinh khoang cach dia ly, dung cho buoc loc/tinh diem cau truc theo ban kinh cua
 * TaskerMatchingService. Brute-force haversine trong Java la du cho quy mo do an (khong can
 * Redis Geo/PostGIS) - xem plan da duyet "Phan tich nghiep vu: 2 tang tin hieu".
 */
final class GeoUtils {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
        // Chi chua static method
    }

    /**
     * Khoang cach haversine giua hai toa do (lat/lng), don vi km. Cong thuc chuan, chinh xac
     * du cho muc dich loc ban kinh/hien thi (khong can do chinh xac cap GPS trac dia).
     */
    static double distanceKm(BigDecimal lat1, BigDecimal lng1, BigDecimal lat2, BigDecimal lng2) {
        double lat1Rad = Math.toRadians(lat1.doubleValue());
        double lat2Rad = Math.toRadians(lat2.doubleValue());
        double deltaLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double deltaLng = Math.toRadians(lng2.doubleValue() - lng1.doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
