package J2EE.SportBooingSystem.enums;

public enum SportType {
    FOOTBALL("Bóng đá"),
    TENNIS("Tennis"),
    BADMINTON("Cầu lông"),
    BASKETBALL("Bóng rổ"),
    VOLLEYBALL("Bóng chuyền"),
    PICKLEBALL("Pickleball");
    private final String displayName;

    SportType(String displayName) {
        this.displayName = displayName;
    }
    public String getDisplayName() {
        return displayName;
    }
}
