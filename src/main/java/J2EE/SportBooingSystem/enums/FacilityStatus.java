package J2EE.SportBooingSystem.enums;

public enum FacilityStatus {
    PENDING_APPROVAL("Chờ admin duyệt"), 
    OPEN("Đang hoạt động"),              
    CLOSED("Tạm đóng cửa"),          
    MAINTENANCE("Đang bảo trì"),
    BLOCKED("Bị khóa bởi Admin");        

    private final String displayName;

    FacilityStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}