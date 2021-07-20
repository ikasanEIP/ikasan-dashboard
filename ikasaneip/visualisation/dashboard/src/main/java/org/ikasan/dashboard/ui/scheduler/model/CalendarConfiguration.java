package org.ikasan.dashboard.ui.scheduler.model;

public class CalendarConfiguration {
    private int numberOfEventsPerDay = 30;
    private boolean showMonthGrid = false;
    private boolean showDayGrid = true;
    private boolean showYearList = false;
    private boolean showMonthList = false;
    private boolean showWeekList = true;
    private boolean showDayList = true;

    public int getNumberOfEventsPerDay() {
        return numberOfEventsPerDay;
    }

    public void setNumberOfEventsPerDay(int numberOfEventsPerDay) {
        this.numberOfEventsPerDay = numberOfEventsPerDay;
    }

    public boolean isShowMonthGrid() {
        return showMonthGrid;
    }

    public void setShowMonthGrid(boolean showMonthGrid) {
        this.showMonthGrid = showMonthGrid;
    }

    public boolean isShowDayGrid() {
        return showDayGrid;
    }

    public void setShowDayGrid(boolean showDayGrid) {
        this.showDayGrid = showDayGrid;
    }

    public boolean isShowYearList() {
        return showYearList;
    }

    public void setShowYearList(boolean showYearList) {
        this.showYearList = showYearList;
    }

    public boolean isShowMonthList() {
        return showMonthList;
    }

    public void setShowMonthList(boolean showMonthList) {
        this.showMonthList = showMonthList;
    }

    public boolean isShowWeekList() {
        return showWeekList;
    }

    public void setShowWeekList(boolean showWeekList) {
        this.showWeekList = showWeekList;
    }

    public boolean isShowDayList() {
        return showDayList;
    }

    public void setShowDayList(boolean showDayList) {
        this.showDayList = showDayList;
    }
}
