package J2EE.SportBooingSystem.enums;

public enum DayType {
    ALL("Tất cả các ngày"),
    WEEKDAY("Ngày thường (T2–T6)"),
    WEEKEND("Cuối tuần (T7–CN)");

    private final String displayName;

    DayType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
